// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.service.poll

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import se.svt.oss.encore.config.EncoreProperties
import se.svt.oss.encore.service.job.JobProcessor
import se.svt.oss.encore.service.queue.QueueService
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Profile("!conductor & !singlejobworker")
@Service
@ExperimentalCoroutinesApi
@FlowPreview
class JobPoller(
    private val queueService: QueueService,
    private val jobProcessor: JobProcessor,
    private val scheduler: ThreadPoolTaskScheduler,
    private val encoreProperties: EncoreProperties,
) {

    private val log = KotlinLogging.logger {}
    private var scheduledTasks = emptyList<ScheduledFuture<*>>()

    @PostConstruct
    fun init() {
        scheduledTasks = (0 until encoreProperties.concurrency).map { queueNo ->
            scheduler.scheduleWithFixedDelay(
                {
                    try {
                        queueService.poll(queueNo)?.let { jobProcessor.processJob(it) }
                    } catch (e: Throwable) {
                        log.error(e) { "Error polling queue $queueNo!" }
                    }
                },
                Instant.now().plus(encoreProperties.pollInitialDelay),
                encoreProperties.pollDelay
            )
        }
    }

    @PreDestroy
    fun destroy() {
        scheduledTasks.forEach { it.cancel(false) }
    }
}
