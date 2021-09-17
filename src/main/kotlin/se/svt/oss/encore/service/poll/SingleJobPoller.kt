package se.svt.oss.encore.service.poll

import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import se.svt.oss.encore.config.EncoreProperties
import se.svt.oss.encore.service.job.JobProcessor
import se.svt.oss.encore.service.queue.QueueService

@Profile("singlejobworker")
@Component
class SingleJobPoller(
    private val queueService: QueueService,
    private val jobProcessor: JobProcessor,
    encoreProperties: EncoreProperties,
    private val context: ApplicationContext
) : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    private val queueNo = encoreProperties.worker.queueNo

    override fun run(vararg args: String) {
        try {
            val queueItem = queueService.poll(queueNo)
            if (queueItem == null) {
                log.info { "No job found on queue, exiting" }
            } else {
                jobProcessor.processJob(queueItem)
            }
        } catch (e: Throwable) {
            log.error(e) { "Error polling queue $queueNo!" }
        } finally {
            SpringApplication.exit(context)
        }
    }
}
