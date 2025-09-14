plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

// Include generated sources in compilation
kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
            // Include generated types if they exist (CI will generate these)
            kotlin.srcDir("src/main/kotlin/io/near/jsonrpc/types/generated")
        }
    }
}

dependencies {
    // Minimal dependencies - only serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Test dependencies
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}