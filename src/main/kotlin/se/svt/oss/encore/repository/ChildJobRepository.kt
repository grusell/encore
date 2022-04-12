// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.repository

import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import se.svt.oss.encore.model.childjob.ChildJob

@RepositoryRestResource
@Tag(name = "childjob")
interface ChildJobRepository : PagingAndSortingRepository<ChildJob, UUID>
