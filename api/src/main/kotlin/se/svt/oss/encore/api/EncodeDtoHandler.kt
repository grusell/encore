package se.svt.oss.encore.api

import se.svt.oss.encore.api.output.Output

interface EncodeDtoHandler {

    val supportedEncodes: List<Class<*>>

    fun getOutput(encode: EncodeDto, job: AbstractEncoreJob): Output?
}