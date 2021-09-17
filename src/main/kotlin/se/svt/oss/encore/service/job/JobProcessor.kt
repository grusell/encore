package se.svt.oss.encore.service.job

import mu.KotlinLogging
import mu.withLoggingContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.model.Status
import se.svt.oss.encore.model.queue.QueueItem
import se.svt.oss.encore.repository.EncoreJobRepository
import se.svt.oss.encore.service.EncoreService
import se.svt.oss.encore.service.queue.QueueService
import java.util.UUID

@Component
class JobProcessor(
    private val repository: EncoreJobRepository,
    private val queueService: QueueService,
    private val encoreService: EncoreService,
) {

    private val log = KotlinLogging.logger {}

    fun processJob(queueItem: QueueItem) {
        val id = UUID.fromString(queueItem.id)
        log.info { "Handling job $id" }
        val job = repository.findByIdOrNull(id)
            ?: retry(id) // Sometimes there has been sync issues
            ?: throw RuntimeException("Job ${queueItem.id} does not exist")

        withLoggingContext(job.contextMap) {
            if (job.status.isCancelled) {
                log.info { "Job was cancelled" }
                return
            }
            log.info { "Running job" }
            try {
                encoreService.encode(job)
            } catch (e: InterruptedException) {
                repostJob(job)
            }
        }
    }

    private fun repostJob(job: EncoreJob) {
        try {
            log.info { "Adding job to queue (repost on interrupt)" }
            queueService.enqueue(job)
            log.info { "Added job to queue (repost on interrupt)" }
        } catch (e: Exception) {
            val message = "Failed to add interrupted job to queue"
            log.error(e) { message }
            job.message = message
            job.status = Status.FAILED
            repository.save(job)
        }
    }

    private fun retry(id: UUID): EncoreJob? {
        Thread.sleep(5000)
        log.info { "Retrying read of job from repository " }
        return repository.findByIdOrNull(id)
    }
}
