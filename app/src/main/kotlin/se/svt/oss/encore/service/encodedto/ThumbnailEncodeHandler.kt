package se.svt.oss.encore.service.encodedto

import mu.KotlinLogging
import org.springframework.stereotype.Component
import se.svt.oss.encore.api.AbstractEncoreJob
import se.svt.oss.encore.api.EncodeDto
import se.svt.oss.encore.api.EncodeDtoHandler
import se.svt.oss.encore.api.input.analyzedVideo
import se.svt.oss.encore.api.mediafile.toParams
import se.svt.oss.encore.api.output.Output
import se.svt.oss.encore.api.output.VideoStreamEncode
import se.svt.oss.encore.model.profile.ThumbnailEncode
import se.svt.oss.mediaanalyzer.file.toFractionOrNull
import kotlin.math.round

@Component
class ThumbnailEncodeHandler: EncodeDtoHandler {
    override val supportedEncodes = listOf(ThumbnailEncode::class.java)

    override fun getOutput(encode: EncodeDto, job: AbstractEncoreJob): Output? {
        if (encode !is ThumbnailEncode) throw IllegalArgumentException("Wrong type: ${encode.javaClass}")
        return encode.getOutput(job)
    }

    private val log = KotlinLogging.logger { }

   fun ThumbnailEncode.getOutput(job: AbstractEncoreJob): Output? {
        val videoStream = job.inputs.analyzedVideo(inputLabel)?.highestBitrateVideoStream
            ?: return logOrThrow("Can not produce thumbnail $suffix. No video input with label $inputLabel!")

        val frameRate = videoStream.frameRate.toFractionOrNull()?.toDouble()
            ?: if (job.duration != null || job.seekTo != null || job.thumbnailTime != null) {
                return logOrThrow("Can not produce thumbnail $suffix! No framerate detected in video input $inputLabel.")
            } else 0.0

        val numFrames = job.duration?.let { round(it * frameRate).toInt() } ?: videoStream.numFrames
        val skipFrames = job.seekTo?.let { round(it * frameRate).toInt() } ?: 0
        val frames = job.thumbnailTime?.let {
            listOf(round(it * frameRate).toInt())
        } ?: percentages.map {
            (it * numFrames) / 100 + skipFrames
        }

        val filter = frames.joinToString(
            separator = "+",
            prefix = "select=",
            postfix = ",scale=$thumbnailWidth:$thumbnailHeight"
        ) { "eq(n\\,$it)" }

        val fileRegex = Regex("${job.baseName}$suffix\\d{2}\\.jpg")
        val params = linkedMapOf(
            "frames:v" to "${frames.size}",
            "vsync" to "vfr",
            "q:v" to "$quality"
        )

        return Output(
            id = "${suffix}02d.jpg",
            video = VideoStreamEncode(
                params = params.toParams(),
                filter = filter,
                inputLabels = listOf(inputLabel)
            ),
            output = "${job.baseName}$suffix%02d.jpg",
            postProcessor = { outputFolder ->
                outputFolder.listFiles().orEmpty().filter { it.name.matches(fileRegex) }
            },
            seekable = false
        )
    }

    private fun ThumbnailEncode.logOrThrow(message: String): Output? {
        if (optional) {
            log.info { message }
            return null
        } else {
            throw RuntimeException(message)
        }
    }
}