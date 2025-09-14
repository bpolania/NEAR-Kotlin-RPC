import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("org.openapi.generator") version "7.2.0"
    id("maven-publish")
    id("jacoco")
}

allprojects {
    group = "io.near"
    version = "1.0.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")

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
    
    // Configure JaCoCo after evaluation
    afterEvaluate {
        tasks.named("test") {
            finalizedBy(tasks.named("jacocoTestReport"))
        }
        
        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
        }
    }

    dependencies {
        val kotlinVersion = "1.9.22"
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.1")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        "testImplementation"("io.mockk:mockk:1.13.8")
    }

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/bpolania/NEAR-Kotlin-RPC")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                
                pom {
                    name.set(project.name)
                    description.set("Type-safe Kotlin client for NEAR Protocol JSON-RPC interface")
                    url.set("https://github.com/bpolania/NEAR-Kotlin-RPC")
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("bpolania")
                            name.set("bpolania")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:git://github.com/bpolania/NEAR-Kotlin-RPC.git")
                        developerConnection.set("scm:git:ssh://github.com/bpolania/NEAR-Kotlin-RPC.git")
                        url.set("https://github.com/bpolania/NEAR-Kotlin-RPC")
                    }
                }
            }
        }
    }
}

tasks.register<GenerateTask>("generateNearRpcFromOpenApi") {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/openapi-spec.json")
    outputDir.set("${layout.buildDirectory.get()}/generated")
    apiPackage.set("io.near.jsonrpc.api")
    modelPackage.set("io.near.jsonrpc.models")
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "serializationLibrary" to "kotlinx_serialization",
        "collectionType" to "list",
        "enumPropertyNaming" to "UPPERCASE",
        "sourceFolder" to "src/main/kotlin",
        "removeEnumValuePrefix" to "false"
    ))
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    validateSpec.set(false) // Skip validation due to patternProperties issue
    // Skip problematic schemas
    skipValidateSpec.set(true)
    globalProperties.set(mapOf(
        "models" to "",
        "apis" to ""
    ))
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

// Aggregate JaCoCo coverage report
tasks.register<JacocoReport>("jacocoRootReport") {
    description = "Generate aggregated JaCoCo coverage report for all modules"
    group = "verification"
    
    dependsOn(subprojects.map { it.tasks.named("test") })
    
    sourceDirectories.setFrom(subprojects.map { 
        it.layout.projectDirectory.dir("src/main/kotlin")
    })
    
    classDirectories.setFrom(subprojects.map {
        it.layout.buildDirectory.dir("classes/kotlin/main")
    })
    
    executionData.setFrom(subprojects.map {
        it.layout.buildDirectory.file("jacoco/test.exec")
    })
    
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate"))
    }
}

tasks.register("testWithCoverage") {
    description = "Run all tests and generate coverage report"
    group = "verification"
    dependsOn("testAll", "jacocoRootReport")
}