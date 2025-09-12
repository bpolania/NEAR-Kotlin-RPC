import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("org.openapi.generator") version "7.2.0"
    id("maven-publish")
}

allprojects {
    group = "io.near"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
    
    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        
        // Configure test execution
        systemProperty("skipIntegrationTests", System.getProperty("skipIntegrationTests") ?: "false")
        // Use QuickNode endpoint by default for better rate limits
        environment("NEAR_TESTNET_RPC_URL", System.getenv("NEAR_TESTNET_RPC_URL") ?: "https://white-shy-fire.near-testnet.quiknode.pro/1c9f76d8dab07f1657d6aebc20441c38e81265e5")
    }

    dependencies {
        val kotlinVersion = "1.9.22"
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.1")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        "testImplementation"("io.mockk:mockk:1.13.8")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}

tasks.register<GenerateTask>("generateNearRpcFromOpenApi") {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/openapi-spec.json")
    outputDir.set("$buildDir/generated")
    apiPackage.set("io.near.jsonrpc.api")
    modelPackage.set("io.near.jsonrpc.models")
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "serializationLibrary" to "kotlinx_serialization",
        "collectionType" to "list",
        "enumPropertyNaming" to "UPPERCASE",
        "sourceFolder" to "src/main/kotlin"
    ))
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
}

tasks.register<Exec>("fetchOpenApiSpec") {
    commandLine("curl", "-L", 
        "https://raw.githubusercontent.com/near/nearcore/master/chain/jsonrpc/openapi/openapi.json",
        "-o", "$rootDir/openapi-spec.json")
    doLast {
        println("OpenAPI spec downloaded successfully")
    }
}

tasks.named("generateNearRpcFromOpenApi") {
    dependsOn("fetchOpenApiSpec")
}

tasks.register("testAll") {
    description = "Run all tests including integration tests"
    group = "verification"
    dependsOn(subprojects.map { it.tasks.named("test") })
}

tasks.register("testUnit") {
    description = "Run only unit tests (skip integration tests)"
    group = "verification"
    doFirst {
        System.setProperty("skipIntegrationTests", "true")
    }
    dependsOn(subprojects.map { it.tasks.named("test") })
}