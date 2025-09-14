package io.near.generator

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val arguments = parseArguments(args)
    
    val specFile = File(arguments["spec"] ?: error("--spec parameter is required"))
    val outputDir = File(arguments["output"] ?: error("--output parameter is required"))
    val packageName = arguments["package"] ?: error("--package parameter is required")
    
    if (!specFile.exists()) {
        println("Error: Spec file not found: ${specFile.absolutePath}")
        exitProcess(1)
    }
    
    // Create output directory if it doesn't exist
    outputDir.mkdirs()
    
    println("Generating Kotlin types from OpenAPI spec...")
    println("  Spec: ${specFile.absolutePath}")
    println("  Output: ${outputDir.absolutePath}")
    println("  Package: $packageName")
    
    try {
        // Parse the OpenAPI spec
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        println("Parsed OpenAPI spec: ${spec.title} v${spec.version}")
        println("Found ${spec.schemas.size} schemas to generate")
        
        // Generate Kotlin code
        val generator = KotlinGenerator(packageName, outputDir)
        generator.generate(spec)
        
        println("Successfully generated Kotlin types!")
    } catch (e: Exception) {
        println("Error generating types: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

private fun parseArguments(args: Array<String>): Map<String, String> {
    val arguments = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--spec" -> arguments["spec"] = args.getOrNull(i + 1) ?: error("Missing value for --spec")
            "--output" -> arguments["output"] = args.getOrNull(i + 1) ?: error("Missing value for --output")
            "--package" -> arguments["package"] = args.getOrNull(i + 1) ?: error("Missing value for --package")
        }
        i += 2
    }
    return arguments
}