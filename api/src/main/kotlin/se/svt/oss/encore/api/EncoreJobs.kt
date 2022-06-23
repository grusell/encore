package se.svt.oss.encore.api

import java.util.UUID


interface EncoreJobs {
    fun createJob(jobRequest: JobRequest): AbstractEncoreJob
    fun getJob(id: UUID): AbstractEncoreJob?
}