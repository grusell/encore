package se.svt.oss.encore.service.encorejobs

import org.springframework.stereotype.Service
import se.svt.oss.encore.api.AbstractEncoreJob
import se.svt.oss.encore.api.EncoreJobs
import se.svt.oss.encore.api.JobRequest
import se.svt.oss.encore.handlers.EncoreJobHandler
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.repository.EncoreJobRepository
import java.util.UUID

@Service
class EncoreJobsService(
    private val repository: EncoreJobRepository,
    private val encoreJobHandler: EncoreJobHandler) : EncoreJobs{
    override fun createJob(jobRequest: JobRequest): AbstractEncoreJob {
        val job = EncoreJob(
            id = jobRequest.id,
            externalId = jobRequest.externalId,
            profile = jobRequest.profile,
            outputFolder = jobRequest.outputFolder,
            baseName = jobRequest.baseName,
            progressCallbackUri = jobRequest.progressCallbackUri,
            priority = jobRequest.priority,
            debugOverlay = jobRequest.debugOverlay,
            logContext = jobRequest.logContext,
            seekTo = jobRequest.seekTo,
            duration = jobRequest.duration,
            inputs = jobRequest.inputs
        )
        return repository.save(job)
            .also { encoreJobHandler.onAfterCreate(it) }
    }

    override fun getJob(id: UUID): AbstractEncoreJob? =
        repository.findById(id).orElse(null)
}