plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

group = "dev.lyzev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kord)
    implementation(libs.piko)
    implementation(libs.guava)
    implementation(libs.logback.classic)

}

kotlin {
    jvmToolchain(24)
}

application {
    mainClass.set("dev.lyzev.turnstile.TurnstileKt")
}