// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import se.svt.oss.encore.service.EncoreJobService

@CrossOrigin
@RestController
class EncoreJobController(
    private val encoreJobService: EncoreJobService
) {

    //    TODO: Make this properly paged and allow it to override the endpoint given by RestRepositoryResource (/encoreJobs)?
    @Operation(
        summary = "Get Recent Encore Jobs",
        description = "Returns a list of Encore Jobs ordered by created date (descending)"
    )
    @GetMapping("/encoreRecentJobs")
    fun getRecentJobs(@RequestParam amount: Long = 20L) = encoreJobService.getJobsSortedByCreatedDate(amount)
}
