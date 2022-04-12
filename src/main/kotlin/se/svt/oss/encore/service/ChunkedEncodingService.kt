// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.redisson.api.RBlockingQueue
import org.redisson.api.RPriorityBlockingQueue
import org.redisson.api.RedissonClient
import org.springframework.data.redis.core.PartialUpdate
import org.springframework.data.redis.core.RedisKeyValueTemplate
import org.springframework.stereotype.Service
import se.svt.oss.encore.config.EncoreProperties
import se.svt.oss.encore.model.childjob.ChildJob
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.model.Status
import se.svt.oss.encore.model.childjob.ChildJobProgress
import se.svt.oss.encore.model.input.maxDuration
import se.svt.oss.encore.model.output.Output
import se.svt.oss.encore.model.queue.QueueItem
import se.svt.oss.encore.process.ChunkParameters
import se.svt.oss.encore.process.CommandBuilder
import se.svt.oss.encore.repository.ChildJobRepository
import se.svt.oss.encore.repository.EncoreJobRepository
import se.svt.oss.encore.service.callback.CallbackService
import se.svt.oss.encore.service.mediaanalyzer.MediaAnalyzerService
import se.svt.oss.encore.service.profile.ProfileService
import se.svt.oss.mediaanalyzer.MediaAnalyzer
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

@Service
class ChunkedEncodingService(
    private val mediaAnalyzer: MediaAnalyzer,
    private val mediaAnalyzerService: MediaAnalyzerService,
    private val encoreProperties: EncoreProperties,
    private val redisson: RedissonClient,
    private val repository: EncoreJobRepository,
    private val childJobRepository: ChildJobRepository,
    private val redisKeyValueTemplate: RedisKeyValueTemplate,
    private val profileService: ProfileService,
    private val callbackService: CallbackService,
    private val objectMapper: ObjectMapper
) {

    private val log = KotlinLogging.logger { }

    private val exit = AtomicBoolean(false)

    private val childJobQueue: RPriorityBlockingQueue<QueueItem> = redisson.getPriorityBlockingQueue(
        encoreProperties.redisKeyPrefix + "-childjob-queue"
    )

    private val childJobProgressQueue: RBlockingQueue<ChildJobProgress> =
        redisson.getBlockingQueue<ChildJobProgress>(encoreProperties.redisKeyPrefix + "-childjob-progress-queue")

    private val executorService = Executors.newSingleThreadExecutor()

    fun encode(
        encoreJob: EncoreJob,
    ) {

        encoreJob.inputs.forEach { input ->
            mediaAnalyzerService.analyzeInput(input)
        }

        log.info { "Start $encoreJob" }
        encoreJob.status = Status.IN_PROGRESS
        repository.save(encoreJob)

        val profile = profileService.getProfile(encoreJob.profile)
        val outputFolder = encoreJob.outputFolder
        val outputs = profile.encodes.mapNotNull {
            it.getOutput(
                encoreJob,
                encoreProperties.audioMixPresets
            )
        }

        val gopDuration = 3.84
        val preferredNumberOfChunks = 10
        val gopsPerChunk =
            Math.ceil(encoreJob.inputs.maxDuration()!! / (preferredNumberOfChunks * gopDuration)).toInt()
        val chunkDuration = gopsPerChunk * gopDuration
        val nChunks = Math.ceil(encoreJob.inputs.maxDuration()!! / chunkDuration).toInt()
        val commandBuilder = CommandBuilder(encoreJob, profile, outputFolder)
        val chunkedCommands =
            (0 until nChunks).map { i ->
                val chunkParameters = ChunkParameters(
                    n = i,
                    seek = i * chunkDuration,
                    duration = chunkDuration
                )
                commandBuilder.buildCommands(outputs.filter { it.chunkable }, chunkParameters)
            }

        val otherCommands = commandBuilder.buildCommands(outputs.filterNot { it.chunkable })
        val commands = chunkedCommands + listOf(otherCommands)
        createChildJobs(encoreJob, "encoding", commands)
    }

    private fun pollChildJobProgress() {
        while (!exit.get()) {
            try {
                val cjp = childJobProgressQueue.poll(1, TimeUnit.SECONDS)
                if (cjp != null) {
                    log.info { "Recived child job progress: $cjp" }

                    // Do partial update of childjob status and progress
                    // val childJob = childJobRepository.findById(cjp.childJobId).get()

                    if (cjp.activeChildJobs == 0) {

                        val encoreJob = repository.findById(cjp.parentId).get()
                        log.info { "No reamining childjobs for stage ${encoreJob.chunkedEncodingState}" }
                        when (encoreJob.chunkedEncodingState) {
                            "encoding" -> startCombineChunks(encoreJob)
                            "collecting" -> handleFinishedJob(encoreJob)
                            else -> throw RuntimeException("Invalid state: ${encoreJob.chunkedEncodingState}")
                        }
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Caught error in checkChildProgress" }
            }
        }
    }

    private fun startCombineChunks(encoreJob: EncoreJob) {
        val profile = profileService.getProfile(encoreJob.profile)

        val outputFolder = encoreJob.outputFolder

        val outputs = profile.encodes.mapNotNull {
            it.getOutput(
                encoreJob,
                encoreProperties.audioMixPresets
            )
        }

        val cmds = outputs.filter { it.chunkable }.map { output ->
            listOf(combineChunksCommand(output, File(outputFolder)))
        }

        createChildJobs(encoreJob, "collecting", cmds)
    }

    private fun handleFinishedJob(encoreJob: EncoreJob) {
        log.info { "handleFinishedJob: $encoreJob" }
        val profile = profileService.getProfile(encoreJob.profile)
        val outputFolder = encoreJob.outputFolder

        val outputs = profile.encodes.mapNotNull {
            it.getOutput(
                encoreJob,
                encoreProperties.audioMixPresets
            )
        }

        //outputs.filter { it.chunkable }
//            .forEach { cleanup(it, File(outputFolder)) }

        val outputFiles = outputs.flatMap { out ->
            out.postProcessor.process(File(outputFolder))
                .map { mediaAnalyzer.analyze(it.toString()) }
        }
        encoreJob.apply {
            status = Status.SUCCESSFUL
            progress = 100
            output = outputFiles
        }
        val partialUpdate = PartialUpdate(encoreJob.id, EncoreJob::class.java)
            .set(encoreJob::progress.name, encoreJob.progress)
            .set(encoreJob::status.name, encoreJob.status)
            .set(encoreJob::output.name, encoreJob.output)
        redisKeyValueTemplate.update(partialUpdate)
        callbackService.sendProgressCallback(encoreJob)
    }

    private fun createChildJobs(encoreJob: EncoreJob, stage: String, allCmds: List<List<List<String>>>) {
        log.info { "Creating ${allCmds.size} childjobs for stage $stage" }
        val childJobs = allCmds.mapIndexed { i, cmds ->
            val cmdsString = objectMapper.writeValueAsString(cmds)
            val idx = encoreJob.childJobs.size + i
            ChildJob(
                parentId = encoreJob.id,
                name = "chunkencode-${encoreJob.id}-$idx",
                commands = cmdsString,
                idx = idx
            )
        }
        childJobRepository.saveAll(childJobs)
        encoreJob.childJobs.addAll(childJobs)
        encoreJob.activeChildJobs = childJobs.size
        encoreJob.chunkedEncodingState = stage
        repository.save(encoreJob)
        log.info { "Saved encoreJob with ${encoreJob.childJobs.size} childJobs, ${encoreJob.activeChildJobs} of which active" }
        childJobQueue.addAll(
            childJobs.map {
                QueueItem(it.id.toString(), encoreJob.priority, encoreJob.createdDate.toLocalDateTime())
            }
        )
    }

    private fun cleanup(
        output: Output,
        workDir: File
    ) {
        workDir.toPath()
            .listDirectoryEntries(output.output.substringBeforeLast(".") + "_chunk*")
            .forEach { it.deleteIfExists() }
        workDir.resolve(output.output + ".concat.txt").delete()
    }

    private fun combineChunksCommand(
        output: Output,
        workDir: File
    ): List<String> {
        val chunks = workDir.toPath()
            .listDirectoryEntries(output.output.substringBeforeLast(".") + "_chunk*")
            .sorted()
        val concatFile = workDir.resolve(output.output + ".concat.txt")
        concatFile.printWriter().use { out ->
            chunks.forEach { out.println("file $it") }
        }
        return listOf(
            "ffmpeg", "-hide_banner", "-loglevel", "warning", "-y", "-f",
            "concat", "-safe", "0", "-i", concatFile.toString(), "-c", "copy",
            workDir.resolve(output.output).toString()
        )
    }

    @PostConstruct
    fun startPollThread() {
        executorService.submit { pollChildJobProgress() }
    }

    @PreDestroy
    fun stopPollThread() {
        exit.set(true)
        executorService.shutdownNow()
        executorService.awaitTermination(5, TimeUnit.SECONDS)
    }
}
