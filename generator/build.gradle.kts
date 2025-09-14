plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("io.near.generator.MainKt")
}

dependencies {
    // KotlinPoet for code generation
    implementation("com.squareup:kotlinpoet:1.15.3")
    
    // JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<JavaExec>("generate") {
    group = "generation"
    description = "Generate Kotlin types from OpenAPI spec"
    mainClass.set("io.near.generator.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(
        "--spec", "$rootDir/openapi-spec.json",
        "--output", "$rootDir/near-jsonrpc-types/src/main/kotlin",
        "--package", "io.near.jsonrpc.types.generated"
    )
}