package se.svt.oss.encore

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import se.svt.oss.encore.model.profile.AudioEncode
import se.svt.oss.encore.model.profile.Profile
import se.svt.oss.encore.model.profile.ThumbnailEncode
import se.svt.oss.encore.model.profile.ThumbnailMapEncode
import se.svt.oss.encore.model.profile.X264Encode
import se.svt.oss.encore.model.profile.X265Encode


class DeserializationTest {

    @Test
    fun testDeserialization() {

        val yamlMapper = YAMLMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        yamlMapper.registerModule(EncodeDtoModule())
        /*
        yamlMapper.registerSubtypes(
            X264Encode::class.java,
            AudioEncode::class.java,
            ThumbnailMapEncode::class.java,
            ThumbnailEncode::class.java,
            X265Encode::class.java
        )*/

        val profileResource = ClassPathResource("profile/program.yml")
        //val profile: Profile =
        yamlMapper.readValue<Profile>(profileResource.inputStream)
    }
}