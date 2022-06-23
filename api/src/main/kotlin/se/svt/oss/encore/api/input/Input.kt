// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.api.input

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import se.svt.oss.mediaanalyzer.file.FractionString
import se.svt.oss.mediaanalyzer.file.MediaContainer
import se.svt.oss.mediaanalyzer.file.MediaFile
import se.svt.oss.mediaanalyzer.file.VideoFile

const val TYPE_AUDIO_VIDEO = "AudioVideo"
const val TYPE_AUDIO = "Audio"
const val TYPE_VIDEO = "Video"
const val AR_REGEX = "^[1-9]\\d*[:/][1-9]\\d*$"
const val AR_MESSAGE = "Must be positive fraction, e.g. 16:9"
const val DEFAULT_VIDEO_LABEL = "main"
const val DEFAULT_AUDIO_LABEL = "main"

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(value = AudioVideoInput::class, name = TYPE_AUDIO_VIDEO),
    JsonSubTypes.Type(value = VideoInput::class, name = TYPE_VIDEO),
    JsonSubTypes.Type(value = AudioInput::class, name = TYPE_AUDIO),
)
sealed interface Input {
    // @get:Schema(description = "URI of input file", required = true, example = "/path/to/file.mp4")
    val uri: String

    // @get:Schema(description = "Input params required to properly decode input", example = """{ "ac": "2" }""")
    val params: LinkedHashMap<String, String>

    // @get:Schema(description = "Type of input", allowableValues = [TYPE_AUDIO_VIDEO, TYPE_VIDEO, TYPE_AUDIO], required = true)
    val type: String

    var analyzed: MediaFile?
}

sealed interface AudioIn : Input {

    val audioLabel: String

    val useFirstAudioStreams: Int?

    val audioFilters: List<String>

    val analyzedAudio: MediaContainer

    val audioStream: Int?
}

sealed interface VideoIn : Input {

    val videoLabel: String

    val dar: FractionString?

    val cropTo: FractionString?

    val padTo: FractionString?

    val videoFilters: List<String>

    val analyzedVideo: VideoFile

    val videoStream: Int?
    val probeInterlaced: Boolean
}

data class AudioInput(
    override val uri: String,
    override val audioLabel: String = DEFAULT_AUDIO_LABEL,
    override val params: LinkedHashMap<String, String> = linkedMapOf(),
    override val useFirstAudioStreams: Int? = null,
    override val audioFilters: List<String> = emptyList(),
    override var analyzed: MediaFile? = null,
    override val audioStream: Int? = null
) : AudioIn {
    override val analyzedAudio: MediaContainer
        @JsonIgnore
        get() = analyzed as? MediaContainer ?: throw RuntimeException("Analyzed audio for $uri is ${analyzed?.type}")

    override val type: String
        get() = TYPE_AUDIO

    val duration: Double
        @JsonIgnore
        get() = analyzedAudio.duration
}

data class VideoInput(
    override val uri: String,
    override val videoLabel: String = DEFAULT_VIDEO_LABEL,
    override val params: LinkedHashMap<String, String> = linkedMapOf(),
    override val dar: FractionString? = null,
    override val cropTo: FractionString? = null,
    override val padTo: FractionString? = null,
    override val videoFilters: List<String> = emptyList(),
    override var analyzed: MediaFile? = null,
    override val videoStream: Int? = null,
    override val probeInterlaced: Boolean = true
) : VideoIn {
    override val analyzedVideo: VideoFile
        @JsonIgnore
        get() = analyzed as? VideoFile ?: throw RuntimeException("Analyzed video for $uri is ${analyzed?.type}")

    override val type: String
        get() = TYPE_VIDEO

    val duration: Double
        @JsonIgnore
        get() = analyzedVideo.duration
}

data class AudioVideoInput(
    override val uri: String,
    override val videoLabel: String = DEFAULT_VIDEO_LABEL,
    override val audioLabel: String = DEFAULT_AUDIO_LABEL,
    override val params: LinkedHashMap<String, String> = linkedMapOf(),
    override val dar: FractionString? = null,
    override val useFirstAudioStreams: Int? = null,
    override val cropTo: FractionString? = null,
    override val padTo: FractionString? = null,
    override val videoFilters: List<String> = emptyList(),
    override val audioFilters: List<String> = emptyList(),
    override var analyzed: MediaFile? = null,
    override val videoStream: Int? = null,
    override val audioStream: Int? = null,
    override val probeInterlaced: Boolean = true
) : VideoIn, AudioIn {
    override val analyzedVideo: VideoFile
        @JsonIgnore
        get() = analyzed as? VideoFile ?: throw RuntimeException("Analyzed audio/video for $uri is ${analyzed?.type}")

    override val analyzedAudio: MediaContainer
        @JsonIgnore
        get() = analyzedVideo

    override val type: String
        get() = TYPE_AUDIO_VIDEO

    val duration: Double
        @JsonIgnore
        get() = analyzedVideo.duration
}

fun List<Input>.inputParams(readDuration: Double?): List<String> =
    flatMap { input ->
        input.params.toParams() +
            (readDuration?.let { listOf("-t", "$it") } ?: emptyList()) +
            listOf("-i", input.uri)
    }

fun List<Input>.maxDuration(): Double? = maxOfOrNull {
    when (it) {
        is AudioVideoInput -> it.duration
        is AudioInput -> it.duration
        is VideoInput -> it.duration
    }
}

fun List<Input>.analyzedAudio(label: String): MediaContainer? {
    val audioInputs = filterIsInstance<AudioIn>()
    require(audioInputs.distinctBy { it.audioLabel }.size == audioInputs.size) {
        "Inputs contains duplicate audio labels!"
    }
    return audioInputs
        .find { it.audioLabel == label }
        ?.analyzedAudio
}

fun List<Input>.analyzedVideo(label: String): VideoFile? {
    val videoInputs = filterIsInstance<VideoIn>()
    require(videoInputs.distinctBy { it.videoLabel }.size == videoInputs.size) {
        "Inputs contains duplicate video labels!"
    }
    return videoInputs
        .find { it.videoLabel == label }
        ?.analyzedVideo
}

fun Map<String, Any?>.toParams(): List<String> =
    flatMap { entry ->
        listOfNotNull("-${entry.key}", entry.value?.let { "$it" })
    }
