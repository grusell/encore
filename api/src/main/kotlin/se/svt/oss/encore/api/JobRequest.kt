package se.svt.oss.encore.api

import se.svt.oss.encore.api.input.Input
import java.net.URI
import java.util.UUID

interface JobRequest {

    val id: UUID
    val externalId: String?
    val profile: String
    val outputFolder: String
    val baseName: String
    val progressCallbackUri: URI?
    val priority: Int
    val debugOverlay: Boolean
    val logContext: Map<String, String>
    val seekTo: Double?
    val duration: Double?
    val thumbnailTime: Double?
    val inputs: List<Input>
}

data class JobRequestImpl(
    override val id: UUID = UUID.randomUUID(),
    override val externalId: String? = null,
    override val profile: String,
    override val outputFolder: String,
    override val baseName: String,
    override val progressCallbackUri: URI? = null,
    override val priority: Int = 0,
    override val debugOverlay: Boolean = false,
    override val logContext: Map<String, String> = emptyMap(),
    override val seekTo: Double? = null,
    override val duration: Double? = null,
    override val thumbnailTime: Double? = null,
    override val inputs: List<Input>
) : JobRequest
