// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.jsontype.NamedType
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.retry.annotation.EnableRetry
import se.svt.oss.encore.config.EncoreProperties
import se.svt.oss.encore.model.profile.AudioEncode
import se.svt.oss.encore.model.profile.ThumbnailEncode
import se.svt.oss.encore.model.profile.ThumbnailMapEncode
import se.svt.oss.encore.model.profile.X264Encode
import se.svt.oss.encore.model.profile.X265Encode


@EnableRetry
@EnableConfigurationProperties(EncoreProperties::class)
@SpringBootApplication(
    exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class]
)
class EncoreApplication

fun main(args: Array<String>) {
    SpringApplication.run(EncoreApplication::class.java, *args)
}
