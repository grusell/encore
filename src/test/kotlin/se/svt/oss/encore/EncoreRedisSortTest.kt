// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import se.svt.oss.junit5.redis.EmbeddedRedisExtension
import se.svt.oss.randomportinitializer.RandomPortInitializer
import se.svt.oss.encore.Assertions.assertThat
import se.svt.oss.encore.config.EncoreProperties
import se.svt.oss.encore.model.input.AudioVideoInput
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.model.Status
import se.svt.oss.encore.model.callback.JobProgress
import se.svt.oss.encore.repository.EncoreJobRepository
import java.io.File
import java.net.URI
import java.time.Duration
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(EmbeddedRedisExtension::class)
@ContextConfiguration(initializers = [RandomPortInitializer::class])
@DirtiesContext
class EncoreRedisSortTest {

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

    lateinit var mockServer: MockWebServer

    @BeforeEach
    fun setUp() {
        for (i in 1..100) {
            repository.save(job())
        }
    }

    @AfterEach
    fun tearDown() {

    }

    @Test
    fun test() {

    }

    fun job(
        outputDir: String = "/output",
        priority: Int = 0,
        file: String = "blaha.mp4"
    ) =
        EncoreJob(
            externalId = "externalId",
            baseName = file,
            profile = "program",
            outputFolder = outputDir,
            progressCallbackUri = URI.create("http://localhost:${mockServer.port}/callbacks/111"),
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
