package io.near.generator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Integration tests for the OpenAPI parser and Kotlin generator
 */
class GeneratorIntegrationTest {
    
    @TempDir
    lateinit var tempDir: File
    
    @Test
    @DisplayName("Should parse and generate code from OpenAPI spec")
    fun testFullPipeline() {
        // Create a test OpenAPI spec file
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                User:
                  type: object
                  properties:
                    id:
                      type: string
                    name:
                      type: string
                    email:
                      type: string
                  required:
                    - id
                    - name
                Status:
                  type: string
                  enum:
                    - ACTIVE
                    - INACTIVE
                    - PENDING
                EmptyObject:
                  type: object
                  properties: {}
        """.trimIndent()
        
        val specFile = File(tempDir, "test-spec.yaml")
        specFile.writeText(specContent)
        
        // Parse the spec
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        // Verify parsing
        assertEquals("Test API", spec.title)
        assertEquals("1.0.0", spec.version)
        assertEquals(3, spec.schemas.size)
        
        // Check User schema
        val userSchema = spec.schemas["User"]
        assertNotNull(userSchema)
        assertTrue(userSchema is SchemaDefinition.Object)
        val userObj = userSchema as SchemaDefinition.Object
        assertEquals(3, userObj.properties.size)
        assertTrue(userObj.required.contains("id"))
        assertTrue(userObj.required.contains("name"))
        assertFalse(userObj.required.contains("email"))
        
        // Check Status enum
        val statusSchema = spec.schemas["Status"]
        assertNotNull(statusSchema)
        assertTrue(statusSchema is SchemaDefinition.Enum)
        val statusEnum = statusSchema as SchemaDefinition.Enum
        assertEquals(3, statusEnum.values.size)
        assertTrue(statusEnum.values.contains("ACTIVE"))
        
        // Check EmptyObject
        val emptySchema = spec.schemas["EmptyObject"]
        assertNotNull(emptySchema)
        assertTrue(emptySchema is SchemaDefinition.Object)
        val emptyObj = emptySchema as SchemaDefinition.Object
        assertTrue(emptyObj.properties.isEmpty())
        
        // Generate code
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // Verify generated files
        val userFile = File(outputDir, "io/near/jsonrpc/types/generated/User.kt")
        assertTrue(userFile.exists(), "User.kt should be generated")
        
        val statusFile = File(outputDir, "io/near/jsonrpc/types/generated/Status.kt")
        assertTrue(statusFile.exists(), "Status.kt should be generated")
        
        val emptyFile = File(outputDir, "io/near/jsonrpc/types/generated/EmptyObject.kt")
        assertTrue(emptyFile.exists(), "EmptyObject.kt should be generated")
        
        // Check User content
        val userContent = userFile.readText()
        assertTrue(userContent.contains("data class User"))
        assertTrue(userContent.contains("val id: String"))
        assertTrue(userContent.contains("val name: String"))
        assertTrue(userContent.contains("val email: String?"))
        assertTrue(userContent.contains("@Serializable"))
        
        // Check Status content
        val statusContent = statusFile.readText()
        assertTrue(statusContent.contains("enum class Status"))
        assertTrue(statusContent.contains("ACTIVE"))
        assertTrue(statusContent.contains("@Serializable"))
        
        // Check EmptyObject content
        val emptyContent = emptyFile.readText()
        assertTrue(emptyContent.contains("object EmptyObject"))
    }
    
    @Test
    @DisplayName("Should handle snake_case to camelCase conversion")
    fun testSnakeCaseConversion() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                Account:
                  type: object
                  properties:
                    user_name:
                      type: string
                    created_at:
                      type: string
                      format: date-time
                    last_login_time:
                      type: integer
                  required:
                    - user_name
        """.trimIndent()
        
        val specFile = File(tempDir, "snake-case-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // List all generated files
        println("Output directory: ${outputDir.absolutePath}")
        println("Files in output directory:")
        outputDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                println("  ${file.relativeTo(outputDir)}")
            }
        }
        
        val accountFile = File(outputDir, "io/near/jsonrpc/types/generated/Account.kt")
        assertTrue(accountFile.exists(), "Account.kt should exist at ${accountFile.absolutePath}")
        
        val content = accountFile.readText()
        println("Generated content for Account.kt:")
        println(content)
        
        // Property names should be camelCase
        assertTrue(content.contains("val userName: String"))
        assertTrue(content.contains("val createdAt: String?"))
        assertTrue(content.contains("val lastLoginTime: Int?"))
        
        // SerialName annotations should preserve snake_case
        assertTrue(content.contains("@SerialName(\"user_name\")"))
        assertTrue(content.contains("@SerialName(\"created_at\")"))
        assertTrue(content.contains("@SerialName(\"last_login_time\")"))
    }
    
    @Test
    @DisplayName("Should handle oneOf schemas")
    fun testOneOfSchema() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                Action:
                  oneOf:
                    - type: object
                      properties:
                        type:
                          type: string
                          enum: [transfer]
                        amount:
                          type: string
                      required:
                        - type
                        - amount
                    - type: object
                      properties:
                        type:
                          type: string
                          enum: [call]
                        method_name:
                          type: string
                      required:
                        - type
                        - method_name
        """.trimIndent()
        
        val specFile = File(tempDir, "oneof-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val actionSchema = spec.schemas["Action"]
        assertNotNull(actionSchema)
        assertTrue(actionSchema is SchemaDefinition.OneOf)
        val oneOf = actionSchema as SchemaDefinition.OneOf
        assertEquals(2, oneOf.options.size)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val actionFile = File(outputDir, "io/near/jsonrpc/types/generated/Action.kt")
        assertTrue(actionFile.exists())
        
        val content = actionFile.readText()
        println("OneOf generated content:")
        println(content)
        assertTrue(content.contains("sealed class Action"))
        assertTrue(content.contains("ActionOption"))  // Generated with parent name prefix
        assertTrue(content.contains("data class"))
    }
    
    @Test
    @DisplayName("Should handle anyOf schemas")
    fun testAnyOfSchema() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                FlexibleValue:
                  anyOf:
                    - type: string
                    - type: number
                    - type: boolean
        """.trimIndent()
        
        val specFile = File(tempDir, "anyof-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val flexSchema = spec.schemas["FlexibleValue"]
        assertNotNull(flexSchema)
        assertTrue(flexSchema is SchemaDefinition.AnyOf)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val flexFile = File(outputDir, "io/near/jsonrpc/types/generated/FlexibleValue.kt")
        assertTrue(flexFile.exists())
        
        val content = flexFile.readText()
        println("AnyOf generated content:")
        println(content)
        // AnyOf generates a data class with all options as nullable fields
        assertTrue(content.contains("data class FlexibleValue"))
        assertTrue(content.contains("val string: String?"))
        assertTrue(content.contains("val number: Double?"))
        assertTrue(content.contains("val boolean: Boolean?"))
    }
    
    @Test
    @DisplayName("Should handle references")
    fun testReferences() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                Address:
                  type: object
                  properties:
                    street:
                      type: string
                    city:
                      type: string
                  required:
                    - city
                Person:
                  type: object
                  properties:
                    name:
                      type: string
                    address:
                      ${'$'}ref: '#/components/schemas/Address'
                  required:
                    - name
        """.trimIndent()
        
        val specFile = File(tempDir, "ref-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        assertEquals(2, spec.schemas.size)
        
        val personSchema = spec.schemas["Person"]
        assertNotNull(personSchema)
        assertTrue(personSchema is SchemaDefinition.Object)
        val personObj = personSchema as SchemaDefinition.Object
        
        val addressProp = personObj.properties["address"]
        assertNotNull(addressProp)
        assertTrue(addressProp is Property.Reference)
        val refProp = addressProp as Property.Reference
        assertEquals("Address", refProp.ref)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val personFile = File(outputDir, "io/near/jsonrpc/types/generated/Person.kt")
        assertTrue(personFile.exists())
        
        val addressFile = File(outputDir, "io/near/jsonrpc/types/generated/Address.kt")
        assertTrue(addressFile.exists())
        
        val personContent = personFile.readText()
        assertTrue(personContent.contains("val address: Address?"))
    }
    
    @Test
    @DisplayName("Should handle array properties in objects")
    fun testArrayProperties() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                Order:
                  type: object
                  properties:
                    items:
                      type: array
                      items:
                        type: string
                    tags:
                      type: array
                      items:
                        type: object
                        properties:
                          name:
                            type: string
                          value:
                            type: string
                  required:
                    - items
        """.trimIndent()
        
        val specFile = File(tempDir, "array-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // Order file should be generated
        val orderFile = File(outputDir, "io/near/jsonrpc/types/generated/Order.kt")
        assertTrue(orderFile.exists(), "Order.kt should be generated")
        
        val orderContent = orderFile.readText()
        println("Order generated content:")
        println(orderContent)
        
        // Check array properties are correctly generated as List types
        assertTrue(orderContent.contains("val items: List<String>"))
        assertTrue(orderContent.contains("val tags: List<JsonElement>?"))  // Complex nested objects become JsonElement
    }
    
    @Test
    @DisplayName("Should handle NEAR-specific patterns")
    fun testNearPatterns() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: NEAR RPC API
              version: 1.0.0
            components:
              schemas:
                BlockHeader:
                  type: object
                  properties:
                    height:
                      type: integer
                    hash:
                      type: string
                    prev_hash:
                      type: string
                    gas_price:
                      type: string
                  required:
                    - height
                    - hash
                RpcResponse:
                  oneOf:
                    - type: object
                      properties:
                        result:
                          type: object
                    - type: object
                      properties:
                        error:
                          type: object
                          properties:
                            code:
                              type: integer
                            message:
                              type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "near-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val blockFile = File(outputDir, "io/near/jsonrpc/types/generated/BlockHeader.kt")
        assertTrue(blockFile.exists())
        
        val blockContent = blockFile.readText()
        assertTrue(blockContent.contains("val height: Int"))  // integer without format maps to Int
        assertTrue(blockContent.contains("val hash: String"))
        assertTrue(blockContent.contains("@SerialName(\"prev_hash\")"))
        assertTrue(blockContent.contains("val prevHash: String?"))
        assertTrue(blockContent.contains("@SerialName(\"gas_price\")"))
        assertTrue(blockContent.contains("val gasPrice: String?"))
        
        val rpcFile = File(outputDir, "io/near/jsonrpc/types/generated/RpcResponse.kt")
        assertTrue(rpcFile.exists())
        
        val rpcContent = rpcFile.readText()
        assertTrue(rpcContent.contains("sealed class RpcResponse"))
    }
}