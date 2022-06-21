package se.svt.oss.encore.service.encodedto

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import se.svt.oss.encore.api.AbstractEncoreJob
import se.svt.oss.encore.api.EncodeDto
import se.svt.oss.encore.api.EncodeDtoHandler
import se.svt.oss.encore.api.mediafile.toParams
import se.svt.oss.encore.api.output.AudioStreamEncode
import se.svt.oss.encore.api.output.Output
import se.svt.oss.encore.api.output.VideoStreamEncode
import se.svt.oss.encore.config.AudioMixPreset
import se.svt.oss.encore.model.profile.GenericVideoEncode
import se.svt.oss.encore.model.profile.VideoEncode
import se.svt.oss.encore.model.profile.X265Encode

@Component
class VideoEncodeHandler : EncodeDtoHandler {

    lateinit var  outputProducer2: OutputProducer2

    override val supportedEncodes = listOf(VideoEncode::class.java, GenericVideoEncode::class.java)

    override fun getOutput(encode: EncodeDto, job: AbstractEncoreJob): Output? {
        if (encode !is VideoEncode) throw IllegalArgumentException("Wrong type: ${encode.javaClass}")
        return encode.getOutput(job)
    }

    fun VideoEncode.getOutput(job: AbstractEncoreJob): Output? {

        val audioEncodesToUse = audioEncodes.ifEmpty { listOfNotNull(audioEncode) }

        val audio = audioEncodesToUse.flatMap { outputProducer2.getOutput(it, job)?.audioStreams.orEmpty() }

        return Output(
            id = "$suffix.$format",
            video = VideoStreamEncode(
                params = secondPassParams().toParams(),
                firstPassParams = firstPassParams().toParams(),
                inputLabels = listOf(inputLabel),
                twoPass = twoPass,
                filter = videoFilter(job.debugOverlay),
            ),
            audioStreams = audio,
            output = "${job.baseName}$suffix.$format"
        )
    }

    fun VideoEncode.firstPassParams(): Map<String, String> {
        return if (!twoPass) {
            emptyMap()
        } else params + Pair("c:v", codec) + passParams(1)
    }

    fun VideoEncode.secondPassParams(): Map<String, String> {
        return if (!twoPass) {
            params + Pair("c:v", codec)
        } else params + Pair("c:v", codec) + passParams(2)
    }

    fun VideoEncode.passParams(pass: Int): Map<String, String> =
        if (this is X265Encode) {
            val modifiedCodecParams = (codecParams + mapOf("pass" to pass.toString(), "stats" to "log$suffix"))
                .map { "${it.key}=${it.value}" }
                .joinToString(":")
            mapOf(codecParamName to modifiedCodecParams)
        } else {
            mapOf("pass" to pass.toString(), "passlogfile" to "log$suffix")
        }

    private fun VideoEncode.videoFilter(debugOverlay: Boolean): String? {
        val videoFilters = mutableListOf<String>()
        if (width != null && height != null) {
            videoFilters.add("scale=$width:$height:force_original_aspect_ratio=decrease:force_divisible_by=2")
        } else if (width != null || height != null) {
            videoFilters.add("scale=${width ?: -2}:${height ?: -2}")
        }
        filters?.let { videoFilters.addAll(it) }
        if (debugOverlay) {
            videoFilters.add("drawtext=text=$suffix:fontcolor=white:fontsize=50:box=1:boxcolor=black@0.75:boxborderw=5:x=10:y=10")
        }
        return if (videoFilters.isEmpty()) null else videoFilters.joinToString(",")
    }
}