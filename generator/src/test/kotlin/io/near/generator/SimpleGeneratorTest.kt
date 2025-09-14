package io.near.generator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SimpleGeneratorTest {
    
    @TempDir
    lateinit var tempDir: File
    
    @Test
    @DisplayName("Simple generation test")
    fun testSimpleGeneration() {
        // Create minimal OpenAPI spec
        val spec = OpenApiSpec(
            title = "Test",
            version = "1.0",
            schemas = mapOf(
                "TestType" to SchemaDefinition.Object(
                    name = "TestType",
                    properties = mapOf(
                        "test_field" to Property.Primitive(
                            name = "test_field",
                            type = "string",
                            nullable = false
                        )
                    ),
                    required = setOf("test_field"),
                    description = null
                )
            )
        )
        
        // Generate code
        val generator = KotlinGenerator(
            packageName = "test.pkg",
            outputDir = tempDir
        )
        generator.generate(spec)
        
        // Check file exists
        val file = File(tempDir, "test/pkg/TestType.kt")
        assertTrue(file.exists(), "File should exist at ${file.absolutePath}")
        
        // Check content
        val content = file.readText()
        println("Generated file content:")
        println(content)
        
        // Basic checks
        assertTrue(content.contains("package test.pkg"))
        assertTrue(content.contains("data class TestType"))
        assertTrue(content.contains("val testField: String"))
        assertTrue(content.contains("@SerialName(\"test_field\")"))
    }
}