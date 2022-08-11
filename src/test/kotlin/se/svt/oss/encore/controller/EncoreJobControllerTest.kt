// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verifySequence
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.service.EncoreJobService
import java.util.UUID

@WebMvcTest(EncoreJobController::class)
@AutoConfigureMockMvc(addFilters = false)
class EncoreJobControllerTest {

    @MockkBean
    private lateinit var encoreJobService: EncoreJobService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Nested
    inner class Jobs {

        @Test
        fun getRecentJobs() {
            val jobs = listOf(
                EncoreJob(
                    id = UUID.randomUUID(),
                    externalId = "1",
                    profile = "animerat",
                    outputFolder = "/shares/test",
                    baseName = "test"
                ),
                EncoreJob(
                    id = UUID.randomUUID(),
                    externalId = "2",
                    profile = "animerat",
                    outputFolder = "/shares/test",
                    baseName = "test"
                )
            )
            every { encoreJobService.getJobsSortedByCreatedDate(any()) } returns jobs

            mockMvc.perform(get("/encoreRecentJobs").param("amount", "10"))
                .andExpect(status().is2xxSuccessful)
                .andExpect(content().json(objectMapper.writeValueAsString(jobs)))

            verifySequence { encoreJobService.getJobsSortedByCreatedDate(eq(10L)) }
        }
    }
}
