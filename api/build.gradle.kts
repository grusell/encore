import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    jacoco
    id("org.springframework.boot") version "2.6.6"
    id("se.ascp.gradle.gradle-versions-filter") version "0.1.10"
    kotlin("jvm") version "1.6.20"
    id("org.jmailen.kotlinter") version "3.9.0"
    id("pl.allegro.tech.build.axion-release") version "1.13.3"
}

group = "se.svt.oss"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

repositories {
    mavenCentral()
}