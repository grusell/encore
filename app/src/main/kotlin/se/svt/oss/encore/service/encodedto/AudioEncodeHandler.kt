package se.svt.oss.encore.service.encodedto

import mu.KotlinLogging
import org.springframework.stereotype.Component
import se.svt.oss.encore.api.AbstractEncoreJob
import se.svt.oss.encore.api.EncodeDto
import se.svt.oss.encore.api.EncodeDtoHandler
import se.svt.oss.encore.api.input.analyzedAudio
import se.svt.oss.encore.api.mediafile.AudioLayout
import se.svt.oss.encore.api.mediafile.audioLayout
import se.svt.oss.encore.api.mediafile.channelCount
import se.svt.oss.encore.api.mediafile.toParams
import se.svt.oss.encore.api.output.AudioStreamEncode
import se.svt.oss.encore.api.output.Output
import se.svt.oss.encore.config.AudioMixPreset
import se.svt.oss.encore.config.EncoreProperties
import se.svt.oss.encore.model.profile.AudioEncode

@Component
class AudioEncodeHandler(encoreProperties: EncoreProperties): EncodeDtoHandler {

    val  audioMixPresets = encoreProperties.audioMixPresets

    override val supportedEncodes = listOf(AudioEncode::class.java)

    override fun getOutput(encode: EncodeDto, job: AbstractEncoreJob): Output? {
        if (encode !is AudioEncode) throw IllegalArgumentException("Wrong type: ${encode.javaClass}")
        return encode.getOutput(job)
    }

    private val log = KotlinLogging.logger { }

    fun AudioEncode.getOutput(job: AbstractEncoreJob): Output? {
        val outputName = "${job.baseName}$suffix.$format"
        val analyzed = job.inputs.analyzedAudio(inputLabel)
            ?: return logOrThrow("Can not generate $outputName! No audio input with label '$inputLabel'.")
        if (analyzed.audioLayout() == AudioLayout.INVALID) {
            throw RuntimeException("Audio layout of audio input '$inputLabel' is not supported!")
        }
        val inputChannels = analyzed.channelCount()
        val preset = audioMixPresets[audioMixPreset]
            ?: throw RuntimeException("Audio mix preset '$audioMixPreset' not found!")
        val pan = preset.panMapping[inputChannels]?.get(channels)
            ?: if (channels <= inputChannels) preset.defaultPan[channels] else null

        val outParams = linkedMapOf<String, Any>()
        if (pan == null) {
            if (preset.fallbackToAuto && isApplicable(inputChannels)) {
                outParams["ac:a:{stream_index}"] = channels
            } else {
                return logOrThrow("Can not generate $outputName! No audio mix preset for '$audioMixPreset': $inputChannels -> $channels channels!")
            }
        }
        outParams["c:a:{stream_index}"] = codec
        outParams["ar:a:{stream_index}"] = samplerate
        bitrate?.let { outParams["b:a:{stream_index}"] = it }
        outParams += params

        return Output(
            id = "$suffix.$format",
            video = null,
            audioStreams = listOf(
                AudioStreamEncode(
                    params = outParams.toParams(),
                    inputLabels = listOf(inputLabel),
                    filter = filtersToString(pan)
                )
            ),
            output = outputName,
        )
    }

    private fun AudioEncode.filtersToString(pan: String?): String? {
        val allFilters = pan?.let { listOf("pan=$it") + filters } ?: filters
        return if (allFilters.isEmpty()) null else allFilters.joinToString(",")
    }

    private fun AudioEncode.isApplicable(channelCount: Int): Boolean {
        return channelCount > 0 && (channels == 2 || channels in 1..channelCount)
    }

    private fun AudioEncode.logOrThrow(message: String): Output? {
        if (optional) {
            log.info { message }
            return null
        } else {
            throw RuntimeException(message)
        }
    }
}