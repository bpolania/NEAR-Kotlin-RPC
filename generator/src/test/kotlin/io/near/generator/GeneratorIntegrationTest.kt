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
        // Parser now creates Empty type for objects with no properties
        assertTrue(emptySchema is SchemaDefinition.Empty)
        
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
    @DisplayName("Should handle empty schemas")
    fun testEmptySchemas() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                EmptyType:
                  type: object
                  properties: {}
                AnotherEmpty:
                  type: object
        """.trimIndent()
        
        val specFile = File(tempDir, "empty-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        // Verify parsing
        val emptySchema = spec.schemas["EmptyType"]
        assertNotNull(emptySchema)
        // Parser now creates Empty type for objects with no properties
        assertTrue(emptySchema is SchemaDefinition.Empty)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // Verify generated files
        val emptyFile = File(outputDir, "io/near/jsonrpc/types/generated/EmptyType.kt")
        assertTrue(emptyFile.exists(), "EmptyType.kt should be generated")
        
        val content = emptyFile.readText()
        println("Empty type generated content:")
        println(content)
        
        // Should generate as object (singleton)
        assertTrue(content.contains("object EmptyType"))
        assertTrue(content.contains("@Serializable"))
    }
    
    @Test
    @DisplayName("Should handle primitive type schemas")
    fun testPrimitiveTypes() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                UserId:
                  type: string
                  description: "User identifier"
                Timestamp:
                  type: integer
                  format: int64
                Balance:
                  type: number
                  format: double
        """.trimIndent()
        
        val specFile = File(tempDir, "primitive-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // Check UserId
        val userIdFile = File(outputDir, "io/near/jsonrpc/types/generated/UserId.kt")
        assertTrue(userIdFile.exists(), "UserId.kt should be generated")
        
        val userIdContent = userIdFile.readText()
        println("UserId generated content:")
        println(userIdContent)
        // Primitive schemas are generated as value classes
        assertTrue(userIdContent.contains("UserId"))
        assertTrue(userIdContent.contains("String"))
        
        // Check Timestamp
        val timestampFile = File(outputDir, "io/near/jsonrpc/types/generated/Timestamp.kt")
        assertTrue(timestampFile.exists(), "Timestamp.kt should be generated")
        
        val timestampContent = timestampFile.readText()
        assertTrue(timestampContent.contains("Timestamp"))
        assertTrue(timestampContent.contains("Long"))
        
        // Check Balance
        val balanceFile = File(outputDir, "io/near/jsonrpc/types/generated/Balance.kt")
        assertTrue(balanceFile.exists(), "Balance.kt should be generated")
        
        val balanceContent = balanceFile.readText()
        assertTrue(balanceContent.contains("Balance"))
        assertTrue(balanceContent.contains("Double"))
    }
    
    @Test
    @DisplayName("Should handle allOf schemas (merged types)")
    fun testAllOfSchemas() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                BaseEntity:
                  type: object
                  properties:
                    id:
                      type: string
                    created_at:
                      type: string
                  required:
                    - id
                UserProfile:
                  type: object
                  properties:
                    name:
                      type: string
                    email:
                      type: string
                  required:
                    - name
                CompleteUser:
                  allOf:
                    - ${'$'}ref: '#/components/schemas/BaseEntity'
                    - ${'$'}ref: '#/components/schemas/UserProfile'
                    - type: object
                      properties:
                        role:
                          type: string
                        active:
                          type: boolean
                      required:
                        - role
        """.trimIndent()
        
        val specFile = File(tempDir, "allof-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val allOfSchema = spec.schemas["CompleteUser"]
        assertNotNull(allOfSchema)
        assertTrue(allOfSchema is SchemaDefinition.AllOf)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val userFile = File(outputDir, "io/near/jsonrpc/types/generated/CompleteUser.kt")
        assertTrue(userFile.exists(), "CompleteUser.kt should be generated")
        
        val content = userFile.readText()
        println("AllOf generated content:")
        println(content)
        
        // Should have merged all properties
        assertTrue(content.contains("data class CompleteUser"))
        assertTrue(content.contains("val id: String"))  // from BaseEntity
        assertTrue(content.contains("val createdAt: String?"))  // from BaseEntity
        assertTrue(content.contains("val name: String"))  // from UserProfile
        assertTrue(content.contains("val email: String?"))  // from UserProfile
        assertTrue(content.contains("val role: String"))  // from inline object
        assertTrue(content.contains("val active: Boolean?"))  // from inline object
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
    
    @Test
    @DisplayName("Should handle complex nested schemas")
    fun testComplexNestedSchemas() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                NestedResponse:
                  type: object
                  properties:
                    status:
                      type: string
                    data:
                      type: object
                      properties:
                        items:
                          type: array
                          items:
                            type: object
                            properties:
                              id:
                                type: integer
                              value:
                                type: string
                        metadata:
                          type: object
                          additionalProperties:
                            type: string
                  required:
                    - status
                    - data
        """.trimIndent()
        
        val specFile = File(tempDir, "nested-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val file = File(outputDir, "io/near/jsonrpc/types/generated/NestedResponse.kt")
        assertTrue(file.exists())
        
        val content = file.readText()
        println("Complex nested generated content:")
        println(content)
        assertTrue(content.contains("NestedResponse"))
        assertTrue(content.contains("status"))
        // Complex nested objects become JsonElement
        assertTrue(content.contains("JsonElement") || content.contains("data"))
    }
    
    @Test
    @DisplayName("Should handle nullable and default values correctly")
    fun testNullableAndDefaults() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                ConfigOptions:
                  type: object
                  properties:
                    required_field:
                      type: string
                    optional_field:
                      type: string
                    nullable_field:
                      type: string
                      nullable: true
                    with_default:
                      type: integer
                      default: 42
                  required:
                    - required_field
        """.trimIndent()
        
        val specFile = File(tempDir, "nullable-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val file = File(outputDir, "io/near/jsonrpc/types/generated/ConfigOptions.kt")
        assertTrue(file.exists())
        
        val content = file.readText()
        println("Nullable test content:")
        println(content)
        
        // Required field should not be nullable
        assertTrue(content.contains("val requiredField: String"))
        // Optional fields should be nullable with default null
        assertTrue(content.contains("val optionalField: String? = null"))
        assertTrue(content.contains("val nullableField: String? = null"))
        assertTrue(content.contains("val withDefault: Int? = null"))
    }
    
    @Test
    @DisplayName("Should handle schemas with descriptions")
    fun testDescriptions() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                DocumentedType:
                  type: object
                  description: "This is a well-documented type for testing"
                  properties:
                    id:
                      type: string
                      description: "Unique identifier for the entity"
                    name:
                      type: string
                      description: "Human-readable name"
                    count:
                      type: integer
                  required:
                    - id
        """.trimIndent()
        
        val specFile = File(tempDir, "documented-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val file = File(outputDir, "io/near/jsonrpc/types/generated/DocumentedType.kt")
        assertTrue(file.exists())
        
        val content = file.readText()
        println("Documented type content:")
        println(content)
        
        // Check that descriptions are included as KDoc
        assertTrue(content.contains("This is a well-documented type for testing"))
        assertTrue(content.contains("Unique identifier for the entity"))
        assertTrue(content.contains("Human-readable name"))
    }
    
    @Test
    @DisplayName("Should handle reference-only schemas")
    fun testReferenceOnlySchema() {
        // This tests the edge case where a schema is just a reference
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                BaseType:
                  type: object
                  properties:
                    value:
                      type: string
                AliasType:
                  ${'$'}ref: '#/components/schemas/BaseType'
        """.trimIndent()
        
        val specFile = File(tempDir, "ref-only-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // BaseType should be generated
        val baseFile = File(outputDir, "io/near/jsonrpc/types/generated/BaseType.kt")
        assertTrue(baseFile.exists())
        
        // AliasType should NOT be generated (it's just a reference)
        val aliasFile = File(outputDir, "io/near/jsonrpc/types/generated/AliasType.kt")
        assertFalse(aliasFile.exists(), "Pure reference schemas should not generate files")
    }
    
    @Test
    @DisplayName("Should handle array types at schema level")
    fun testArraySchema() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                StringArray:
                  type: array
                  items:
                    type: string
                ModelWithArrays:
                  type: object
                  properties:
                    tags:
                      type: array
                      items:
                        type: string
                    scores:
                      type: array
                      items:
                        type: number
                        format: float
                    nested:
                      type: array
                      items:
                        ${'$'}ref: '#/components/schemas/StringArray'
        """.trimIndent()
        
        val specFile = File(tempDir, "array-schema-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // Array schema at top level should not generate a file (handled inline)
        val arrayFile = File(outputDir, "io/near/jsonrpc/types/generated/StringArray.kt")
        assertFalse(arrayFile.exists())
        
        // Model with array properties should exist
        val modelFile = File(outputDir, "io/near/jsonrpc/types/generated/ModelWithArrays.kt")
        assertTrue(modelFile.exists())
        val content = modelFile.readText()
        assertTrue(content.contains("tags: List<String>?"))
        assertTrue(content.contains("scores: List<Float>?"))
    }
    
    @Test
    @DisplayName("Should handle complex primitive type mappings")
    fun testPrimitiveTypeMappings() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                PrimitiveTypes:
                  type: object
                  properties:
                    byteString:
                      type: string
                      format: byte
                    binaryData:
                      type: string
                      format: binary
                    dateField:
                      type: string
                      format: date
                    dateTimeField:
                      type: string
                      format: date-time
                    int32Field:
                      type: integer
                      format: int32
                    int64Field:
                      type: integer
                      format: int64
                    floatField:
                      type: number
                      format: float
                    doubleField:
                      type: number
                      format: double
                    boolField:
                      type: boolean
                    objectField:
                      type: object
        """.trimIndent()
        
        val specFile = File(tempDir, "primitive-types-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val file = File(outputDir, "io/near/jsonrpc/types/generated/PrimitiveTypes.kt")
        assertTrue(file.exists())
        val content = file.readText()
        
        // Check type mappings
        assertTrue(content.contains("byteString: String?"))  // byte format -> String
        assertTrue(content.contains("binaryData: ByteArray?"))  // binary format -> ByteArray
        assertTrue(content.contains("dateField: String?"))  // date format -> String
        assertTrue(content.contains("dateTimeField: String?"))  // date-time format -> String
        assertTrue(content.contains("int32Field: Int?"))  // int32 -> Int
        assertTrue(content.contains("int64Field: Long?"))  // int64 -> Long
        assertTrue(content.contains("floatField: Float?"))  // float -> Float
        assertTrue(content.contains("doubleField: Double?"))  // double -> Double
        assertTrue(content.contains("boolField: Boolean?"))  // boolean -> Boolean
        assertTrue(content.contains("objectField: JsonElement?"))  // object -> JsonElement
    }
    
    @Test
    @DisplayName("Should handle complex union types with objects")
    fun testComplexUnionTypes() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                AnyOfWithObjects:
                  anyOf:
                    - type: object
                      properties:
                        name:
                          type: string
                        age:
                          type: integer
                    - type: object
                      properties:
                        company:
                          type: string
                        employees:
                          type: integer
                OneOfMixed:
                  oneOf:
                    - type: object
                      properties:
                        id:
                          type: string
                    - type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "union-types-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // AnyOf with objects should merge properties
        val anyOfFile = File(outputDir, "io/near/jsonrpc/types/generated/AnyOfWithObjects.kt")
        assertTrue(anyOfFile.exists())
        val anyOfContent = anyOfFile.readText()
        assertTrue(anyOfContent.contains("data class AnyOfWithObjects"))
        assertTrue(anyOfContent.contains("name: String?"))
        assertTrue(anyOfContent.contains("age: Int?"))
        assertTrue(anyOfContent.contains("company: String?"))
        assertTrue(anyOfContent.contains("employees: Int?"))
        
        // OneOf should create sealed class with options
        val oneOfFile = File(outputDir, "io/near/jsonrpc/types/generated/OneOfMixed.kt")
        assertTrue(oneOfFile.exists())
        val oneOfContent = oneOfFile.readText()
        assertTrue(oneOfContent.contains("sealed class OneOfMixed"))
        assertTrue(oneOfContent.contains("data class OneOfMixedOption1"))
        assertTrue(oneOfContent.contains("object OneOfMixedOption2"))
    }
    
    @Test
    @DisplayName("Should handle edge cases in primitive type mapping")
    fun testEdgeCasePrimitiveTypes() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                UnknownTypes:
                  type: object
                  properties:
                    unknownType:
                      type: unknown
                    arrayType:
                      type: array
                    objectType:
                      type: object
                DigitEnum:
                  type: string
                  enum:
                    - "123value"
                    - "456option"
                    - "normal"
        """.trimIndent()
        
        val specFile = File(tempDir, "edge-case-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        // Check UnknownTypes with edge cases
        val unknownFile = File(outputDir, "io/near/jsonrpc/types/generated/UnknownTypes.kt")
        assertTrue(unknownFile.exists())
        val unknownContent = unknownFile.readText()
        println("UnknownTypes content:")
        println(unknownContent)
        // Unknown types should map to Any?
        assertTrue(unknownContent.contains("unknownType: Any?"))
        // array type at property level without items will go through array property parser
        assertTrue(unknownContent.contains("arrayType: List<String>?"))  // Default item type is string
        // object type should be JsonElement
        assertTrue(unknownContent.contains("objectType: JsonElement?"))
        
        // Check DigitEnum with values starting with digits
        val enumFile = File(outputDir, "io/near/jsonrpc/types/generated/DigitEnum.kt")
        assertTrue(enumFile.exists())
        val enumContent = enumFile.readText()
        // Enum values starting with digits should be prefixed with underscore
        assertTrue(enumContent.contains("_123VALUE"))
        assertTrue(enumContent.contains("_456OPTION"))
        assertTrue(enumContent.contains("NORMAL"))
    }
    
    @Test
    @DisplayName("Should handle format specifications for primitive types")
    fun testFormatSpecifications() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                FormattedTypes:
                  type: object
                  properties:
                    date_field:
                      type: string
                      format: date
                    datetime_field:
                      type: string
                      format: date-time
                    byte_field:
                      type: string
                      format: byte
                    binary_field:
                      type: string
                      format: binary
                    int32_field:
                      type: integer
                      format: int32
                    int64_field:
                      type: integer
                      format: int64
                    float_field:
                      type: number
                      format: float
                    double_field:
                      type: number
                      format: double
        """.trimIndent()
        
        val specFile = File(tempDir, "format-spec.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val outputDir = File(tempDir, "generated")
        val generator = KotlinGenerator(
            packageName = "io.near.jsonrpc.types.generated",
            outputDir = outputDir
        )
        generator.generate(spec)
        
        val file = File(outputDir, "io/near/jsonrpc/types/generated/FormattedTypes.kt")
        assertTrue(file.exists())
        
        val content = file.readText()
        
        // Check proper type mappings
        assertTrue(content.contains("val dateField: String?"))  // dates as strings
        assertTrue(content.contains("val datetimeField: String?"))  // datetime as strings
        assertTrue(content.contains("val byteField: String?"))  // base64 as string
        assertTrue(content.contains("val binaryField: ByteArray?"))  // binary as ByteArray
        assertTrue(content.contains("val int32Field: Int?"))
        assertTrue(content.contains("val int64Field: Long?"))
        assertTrue(content.contains("val floatField: Float?"))
        assertTrue(content.contains("val doubleField: Double?"))
    }
}