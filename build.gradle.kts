import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.OutputStream

plugins {
    idea
    jacoco
    id("org.springframework.boot") version "2.5.2"
    id("se.ascp.gradle.gradle-versions-filter") version "0.1.6"
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.spring") version "1.5.20"
    id("com.github.fhermansson.assertj-generator") version "1.1.2"
    id("org.jmailen.kotlinter") version "3.4.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("pl.allegro.tech.build.axion-release") version "1.13.3"

    //openapi generation
    id("com.github.johnrengelman.processes") version "0.5.0"
    id("org.springdoc.openapi-gradle-plugin") version "1.3.2"
}

apply(from = "checks.gradle")

group = "se.svt.oss"
//project.version = scmVersion.version

assertjGenerator {
    classOrPackageNames = arrayOf(
        "se.svt.oss.encore.model",
        "se.svt.oss.redisson.starter.queue",
        "se.svt.oss.mediaanalyzer.file"
    )
    entryPointPackage = "se.svt.oss.encore"
}

kotlinter {
    disabledRules = arrayOf("import-ordering")
}

tasks.test {
    useJUnitPlatform()
}

openApi {
    forkProperties.set("-Dspring.profiles.active=local")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

repositories {
    mavenCentral()
}

configurations {
    implementation {
        exclude(module = "spring-boot-starter-logging")
        exclude(module = "lombok")
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2020.0.3")
    }
}

val redissonVersion = "3.16.0"

dependencies {
    implementation("se.svt.oss:media-analyzer:1.0.3")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("com.lmax:disruptor:3.4.2")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.redisson:redisson-spring-boot-starter:$redissonVersion")
    implementation("org.redisson:redisson-spring-data-25:$redissonVersion") // match boot version
    implementation("io.github.microutils:kotlin-logging:2.0.8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.5.0")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.github.openfeign:feign-okhttp")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    //openapi generation
    implementation("org.springdoc:springdoc-openapi-ui:1.5.9")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.5.9")
    implementation("org.springdoc:springdoc-openapi-data-rest:1.5.9")
    implementation("org.springdoc:springdoc-openapi-hateoas:1.5.9")

    testImplementation("se.svt.oss.junit5:junit5-redis-extension:2.0.3")
    testImplementation("se.svt.oss:random-port-initializer:1.0.5")
    testImplementation("org.awaitility:awaitility:4.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.assertj:assertj-core")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:3.14.4")//shall match with okhttp version used
    testImplementation("com.ninja-squad:springmockk:3.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}


tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "6.8.3"
}

val integrationTestsPreReq = setOf("mediainfo", "ffmpeg", "ffprobe").map {

    tasks.create("Verify $it is in path, required for integration tests", Exec::class.java) {
        isIgnoreExitValue = true
        executable = it

        if (!it.equals("mediainfo")) {
            args("-hide_banner")
        }
    }
}

tasks.test {
    dependsOn(integrationTestsPreReq)
}

