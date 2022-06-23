package se.svt.oss.encore

import org.pf4j.spring.SpringPluginManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PluginConfiguration {

    @Bean
    fun pluginManager() =
        SpringPluginManager()
}