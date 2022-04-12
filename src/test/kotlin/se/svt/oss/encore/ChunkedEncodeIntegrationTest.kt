// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore

import com.fasterxml.jackson.core.type.TypeReference
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.redisson.api.RBlockingQueue
import org.redisson.api.RPriorityBlockingQueue
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import se.svt.oss.encore.Assertions.assertThat
import se.svt.oss.encore.model.childjob.ChildJob
import se.svt.oss.encore.model.childjob.ChildJobProgress
import se.svt.oss.encore.model.queue.QueueItem
import se.svt.oss.encore.repository.ChildJobRepository
import se.svt.oss.encore.repository.EncoreJobRepository
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ActiveProfiles("test")
class ChunkedEncodeIntegrationTest : EncoreIntegrationTestBase() {

    @Autowired
    lateinit var redisson: RedissonClient

    @Autowired
    lateinit var repository: EncoreJobRepository

    @Autowired
    lateinit var childJobRepository: ChildJobRepository

    @Value("\${spring.redis.port}")
    var redisPort: Int = 0

    private val log = KotlinLogging.logger { }

    @Test
    fun jobIsSuccessfulAndNoAudioPresets(@TempDir outputDir: File) {

        val expectedOutputFiles = defaultExpectedOutputFiles(outputDir, testFileSurround) + listOf(
            expectedFile(
                outputDir,
                testFileSurround,
                "SURROUND.mp4"
            ),
            expectedFile(
                outputDir,
                testFileSurround,
                "STEREO_DE.mp4"
            )
        )
        val job = encoreClient.createJob(
            job(
                profile = "program",
//            profile = "simple",
                outputDir = outputDir,
                file = testFileSurround,
                enableChunkedEncode = true
            )
        )

        val worker = Worker()
        worker.start()

        val createdJob = awaitJob(
            jobId = job.id,
            timeout = Duration.ofMinutes(5)
        ) { it.status.isCompleted }

        worker.stop()

        val output = createdJob.output.map { it.file }

        assertThat(output).containsExactlyInAnyOrder(*expectedOutputFiles.toTypedArray())

        expectedOutputFiles
            .map { File(it) }
            .forEach { assertThat(it).isNotEmpty }
    }

    inner class Worker {

        val threadRef = AtomicReference<Thread>()

        private val childJobQueue: RPriorityBlockingQueue<QueueItem> = redisson.getPriorityBlockingQueue(
            encoreProperties.redisKeyPrefix + "-childjob-queue"
        )

        private val childJobProgressQueue: RBlockingQueue<ChildJobProgress> =
            redisson.getBlockingQueue(encoreProperties.redisKeyPrefix + "-childjob-progress-queue")

        fun start() {
            val thread = Thread {
                doPoll()
            }
            thread.start()
            threadRef.set(thread)
        }

        fun stop() {
            threadRef.get().interrupt()
            threadRef.get().join()
        }

        private fun doPoll() {
            var receivedJobs = 0
            try {
                while (true) {
                    childJobQueue.poll(1, TimeUnit.SECONDS)?.let { queueItem ->
                        log.info { "Received queue item $queueItem" }
                        val childJob = childJobRepository.findById(UUID.fromString(queueItem.id)).get()
                        val encoreJob = repository.findById(childJob.parentId).get()
                        runChildJob(childJob)
                        receivedJobs++
                        encoreJob.activeChildJobs--
                        repository.save(encoreJob)

                        log.info { "processed childjob $receivedJobs" }
                        childJobProgressQueue.add(
                            ChildJobProgress(
                                childJob.id, childJob.parentId, 100,
                                "SUCCESSFUL", encoreJob.activeChildJobs
                            )
                        )
                    }
                }
            } catch (exception: Exception) {
                log.error(exception) { "ChildJob execution failed" }
            }
        }

        private fun runChildJob(childJob: ChildJob) {
            val workDir = Files.createTempDirectory("encore_").toFile()

            val commands: List<List<String>> = objectMapper.readValue(
                childJob.commands,
                object : TypeReference<List<List<String>>>() {}
            )

            commands.forEach { command ->
                val ffmpegProcess = ProcessBuilder(command)
                    .directory(workDir)
                    .redirectErrorStream(true)
                    .start()
                ffmpegProcess.inputStream.reader().useLines { lines ->
                    log.info { "Skipped ${lines.count()} of ffmpeg output" }
                }

                ffmpegProcess.waitFor(1L, TimeUnit.MINUTES)
                ffmpegProcess.destroy()

                val exitCode = ffmpegProcess.waitFor()
                if (exitCode != 0) {
                    throw RuntimeException(
                        "Error running ffmpeg (exit code $exitCode)"
                    )
                }
            }
        }
    }
}
