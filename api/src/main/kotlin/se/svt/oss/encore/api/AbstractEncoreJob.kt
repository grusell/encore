// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.api

import se.svt.oss.encore.api.input.Input
import se.svt.oss.mediaanalyzer.file.MediaFile
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

interface AbstractEncoreJob {
    val id: UUID

    val externalId: String?

    val profile: String

    val outputFolder: String

    val baseName: String

    val createdDate: OffsetDateTime

    val progressCallbackUri: URI?

    val priority: Int

    var message: String?

    var progress: Int

    var speed: Double?

    var startedDate: OffsetDateTime?


    var completedDate: OffsetDateTime?

    val debugOverlay: Boolean

    val logContext: Map<String, String>

    val seekTo: Double?

    val duration: Double?

    val thumbnailTime: Double?

    val inputs: List<Input>

    var status: Status

}
