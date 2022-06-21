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