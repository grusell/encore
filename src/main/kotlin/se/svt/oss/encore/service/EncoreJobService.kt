// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.service

import mu.KotlinLogging
import org.springframework.data.redis.connection.SortParameters
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.query.SortQueryBuilder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.repository.EncoreJobRepository
import java.util.UUID

@Service
class EncoreJobService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val encoreJobRepository: EncoreJobRepository
) {
    private val log = KotlinLogging.logger { }

    fun getJobsSortedByCreatedDate(amount: Long): List<EncoreJob> {
        return stringRedisTemplate.sort(
            SortQueryBuilder
                .sort("encore-jobs")
                .by("encore-jobs:*->createdDate")
                .limit(0, amount)
                .order(SortParameters.Order.DESC)
                .alphabetical(true)
                .build()
        )
            .map(UUID::fromString)
            .mapNotNull(encoreJobRepository::findByIdOrNull)
    }
}
