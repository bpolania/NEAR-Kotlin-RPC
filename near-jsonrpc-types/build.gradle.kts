plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Minimal dependencies - only serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Test dependencies
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}