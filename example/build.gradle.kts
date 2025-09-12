plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("io.near.example.MainKt")
}

dependencies {
    implementation(project(":near-jsonrpc-client"))
    implementation(project(":near-jsonrpc-types"))
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
}