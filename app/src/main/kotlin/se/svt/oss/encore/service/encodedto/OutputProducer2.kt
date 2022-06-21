package se.svt.oss.encore.service.encodedto

import org.springframework.stereotype.Component
import se.svt.oss.encore.api.AbstractEncoreJob
import se.svt.oss.encore.api.EncodeDto
import se.svt.oss.encore.api.EncodeDtoHandler
import se.svt.oss.encore.api.output.Output
import se.svt.oss.encore.model.EncoreJob
import javax.annotation.PostConstruct

@Component
class OutputProducer2(val encodeDtoHandlers: List<EncodeDtoHandler>) {

    fun getOutput(encode: EncodeDto, encoreJob: AbstractEncoreJob): Output? {
        val handler = encodeDtoHandlers.firstOrNull {
            it.supportedEncodes.contains(encode.javaClass)
        } ?: encodeDtoHandlers.firstOrNull {
            it.supportedEncodes.any { clazz -> clazz.isAssignableFrom(encode.javaClass)}
        } ?: throw RuntimeException("No handler found for encode of type ${encode.javaClass}")

        val  output = handler.getOutput(encode, encoreJob)
        return output
    }

    @PostConstruct
    fun init() {
        encodeDtoHandlers.filter { it is VideoEncodeHandler }
            .forEach {
                (it as VideoEncodeHandler).outputProducer2 = this
            }
    }
}