package se.svt.oss.encore.service.encodedto

import mu.KotlinLogging
import org.apache.commons.math3.fraction.Fraction
import org.springframework.stereotype.Component
import se.svt.oss.encore.api.AbstractEncoreJob
import se.svt.oss.encore.api.EncodeDto
import se.svt.oss.encore.api.EncodeDtoHandler
import se.svt.oss.encore.api.input.analyzedVideo
import se.svt.oss.encore.api.output.Output
import se.svt.oss.encore.api.output.VideoStreamEncode
import se.svt.oss.encore.model.profile.ThumbnailMapEncode
import se.svt.oss.mediaanalyzer.file.stringValue
import se.svt.oss.mediaanalyzer.file.toFractionOrNull
import kotlin.io.path.createTempDirectory
import kotlin.math.round

@Component
class ThumbnailMapEncodeHandler: EncodeDtoHandler {
    override val supportedEncodes = listOf(ThumbnailMapEncode::class.java)

    override fun getOutput(encode: EncodeDto, job: AbstractEncoreJob): Output? {
        if (encode !is ThumbnailMapEncode) throw IllegalArgumentException("Wrong type: ${encode.javaClass}")
        return encode.getOutput(job)
    }

    private val log = KotlinLogging.logger { }

    fun ThumbnailMapEncode.getOutput(job: AbstractEncoreJob): Output? {
        val videoStream = job.inputs.analyzedVideo(inputLabel)?.highestBitrateVideoStream
            ?: return logOrThrow("No input with label $inputLabel!")
        var numFrames = videoStream.numFrames
        val duration = job.duration
        if (job.duration != null || job.seekTo != null) {
            val frameRate = videoStream.frameRate.toFractionOrNull()?.toDouble()
                ?: return logOrThrow("Can not generate thumbnail map $suffix! No framerate detected in video input $inputLabel.")
            if (duration != null) {
                numFrames = round(duration * frameRate).toInt()
            } else {
                job.seekTo?.let { numFrames -= round(it * frameRate).toInt() }
            }
        }

        if (numFrames < cols * rows) {
            val message =
                "Video input $inputLabel did not contain enough frames to generate thumbnail map $suffix: $numFrames < $cols cols * $rows rows"
            return logOrThrow(message)
        }
        val tempFolder = createTempDirectory(suffix).toFile()
        tempFolder.deleteOnExit()
        val pad =
            "aspect=${Fraction(tileWidth, tileHeight).stringValue()}:x=(ow-iw)/2:y=(oh-ih)/2" // pad to aspect ratio
        val nthFrame = numFrames / (cols * rows)
        var select = "not(mod(n\\,$nthFrame))"
        job.seekTo?.let { select += "*gte(t\\,$it)" }
        return Output(
            id = "$suffix.$format",
            video = VideoStreamEncode(
                params = listOf("-q:v", "5"),
                filter = "select=$select,pad=$pad,scale=-1:$tileHeight",
                inputLabels = listOf(inputLabel)
            ),
            output = tempFolder.resolve("${job.baseName}$suffix%03d.$format").toString(),
            seekable = false,
            postProcessor = { outputFolder ->
                try {
                    val targetFile = outputFolder.resolve("${job.baseName}$suffix.$format")
                    val process = ProcessBuilder(
                        "ffmpeg",
                        "-i",
                        "${job.baseName}$suffix%03d.$format",
                        "-vf",
                        "tile=${cols}x$rows",
                        "-frames:v",
                        "1",
                        "$targetFile"
                    )
                        .directory(tempFolder)
                        .start()
                    val status = process.waitFor()
                    tempFolder.deleteRecursively()
                    if (status != 0) {
                        throw RuntimeException("Ffmpeg returned status code $status")
                    }
                    listOf(targetFile)
                } catch (e: Exception) {
                    logOrThrow("Error creating thumbnail map! ${e.message}")
                    emptyList()
                }
            }
        )
    }

    private fun ThumbnailMapEncode.logOrThrow(message: String): Output? {
        if (optional) {
            log.info { message }
            return null
        } else {
            throw RuntimeException(message)
        }
    }
}