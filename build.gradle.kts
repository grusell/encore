plugins {
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.spring") version "1.6.20"
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "7.4"
}
