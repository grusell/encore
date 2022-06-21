package se.svt.oss.encore

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import se.svt.oss.encore.model.profile.AudioEncode
import se.svt.oss.encore.model.profile.ThumbnailEncode
import se.svt.oss.encore.model.profile.ThumbnailMapEncode
import se.svt.oss.encore.model.profile.X264Encode
import se.svt.oss.encore.model.profile.X265Encode

@Import(JacksonAutoConfiguration::class)
@Configuration
class YamlConfiguration {

    @Bean
    fun encodeDtoModule() =
        EncodeDtoModule()

    @Bean
    fun yamlMapper(modules: List<com.fasterxml.jackson.databind.Module>): YAMLMapper =
        YAMLMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .also { yamlMapper ->
                yamlMapper.registerModules(*modules.toTypedArray())
            } as YAMLMapper
}

class EncodeDtoModule: com.fasterxml.jackson.databind.Module() {

    override fun getModuleName() = "EncodeDtoModule"

    override fun version(): Version =
        Version.unknownVersion()

    override fun setupModule(context: SetupContext) {
        context.registerSubtypes(
            X264Encode::class.java,
            AudioEncode::class.java,
            ThumbnailMapEncode::class.java,
            ThumbnailEncode::class.java,
            X265Encode::class.java
        )
    }

}