// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import se.svt.oss.encore.config.EncoreProperties
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.model.input.AudioVideoInput
import se.svt.oss.encore.repository.EncoreJobRepository
import se.svt.oss.encore.service.EncoreJobService
import se.svt.oss.junit5.redis.EmbeddedRedisExtension
import se.svt.oss.randomportinitializer.RandomPortInitializer
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(EmbeddedRedisExtension::class)
@ContextConfiguration(initializers = [RandomPortInitializer::class])
@ActiveProfiles("test")
@DirtiesContext
class EncoreRedisSortTest {

    private val log = KotlinLogging.logger { }

    @Autowired
    lateinit var encoreClient: EncoreClient

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var scheduler: ThreadPoolTaskScheduler

    @Autowired
    lateinit var encoreProperties: EncoreProperties

    @Autowired
    lateinit var repository: EncoreJobRepository

    @Autowired
    lateinit var encoreJobService: EncoreJobService

    @BeforeEach
    fun setUp() {
        for (i in 1..100) {
            repository.save(job("$i"))
        }
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun test() {
        val findSortedJobs = encoreJobService.getJobsSortedByCreatedDate(10)
        Assertions.assertThat(findSortedJobs.map { it.externalId }).containsExactly(
            "100",
            "99",
            "98",
            "97",
            "96",
            "95",
            "94",
            "93",
            "92",
            "91"
        )
    }

    fun job(
        externalId: String,
        outputDir: String = "/output",
        priority: Int = 0,
        file: String = "blaha.mp4"
    ) =
        EncoreJob(
            externalId = externalId,
            baseName = file,
            profile = "program",
            outputFolder = outputDir,
            debugOverlay = true,
            priority = priority,
            inputs = listOf(
                AudioVideoInput(
                    uri = file,
                    useFirstAudioStreams = 6
                )
            ),
            logContext = mapOf("FlowId" to UUID.randomUUID().toString())
        )
}
