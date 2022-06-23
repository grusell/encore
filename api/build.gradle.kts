import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    jacoco
    //id("org.springframework.boot") version "2.6.6"
    id("se.ascp.gradle.gradle-versions-filter") version "0.1.10"
    kotlin("jvm")
    id("org.jmailen.kotlinter") version "3.9.0"
    id("pl.allegro.tech.build.axion-release") version "1.13.3"
    `java-library`
    `maven-publish`
}

group = "se.svt.oss"
version = "0.1.2"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("se.svt.oss:media-analyzer:1.0.3")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.2")
}