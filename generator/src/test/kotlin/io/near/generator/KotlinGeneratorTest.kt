package io.near.generator

import com.squareup.kotlinpoet.FileSpec
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinGeneratorTest {
    
    private val generator = KotlinGenerator()
    
    @TempDir
    lateinit var tempDir: File
    
    @Nested
    @DisplayName("Code Generation")
    inner class CodeGenerationTests {
        
        @Test
        @DisplayName("Should generate data class for object schema")
        fun testGenerateDataClass() {
            val schema = SchemaDefinition.Object(
                properties = mapOf(
                    "id" to SchemaDefinition.Primitive("string"),
                    "name" to SchemaDefinition.Primitive("string"),
                    "age" to SchemaDefinition.Primitive("integer")
                ),
                required = setOf("id"),
                additionalProperties = null
            )
            
            val schemas = mapOf("User" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/User.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("data class User"))
            assertTrue(content.contains("val id: String"))
            assertTrue(content.contains("val name: String? = null"))
            assertTrue(content.contains("val age: Int? = null"))
            assertTrue(content.contains("@Serializable"))
        }
        
        @Test
        @DisplayName("Should generate object for empty schema")
        fun testGenerateEmptyObject() {
            val schema = SchemaDefinition.Object(
                properties = emptyMap(),
                required = emptySet(),
                additionalProperties = null
            )
            
            val schemas = mapOf("EmptyObject" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/EmptyObject.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("object EmptyObject"))
            assertFalse(content.contains("data class"))
            assertTrue(content.contains("@Serializable"))
        }
        
        @Test
        @DisplayName("Should generate enum class")
        fun testGenerateEnum() {
            val schema = SchemaDefinition.Enum(
                values = listOf("ACTIVE", "INACTIVE", "PENDING")
            )
            
            val schemas = mapOf("Status" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/Status.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("enum class Status"))
            assertTrue(content.contains("ACTIVE"))
            assertTrue(content.contains("INACTIVE"))
            assertTrue(content.contains("PENDING"))
            assertTrue(content.contains("@Serializable"))
        }
        
        @Test
        @DisplayName("Should generate sealed class for oneOf")
        fun testGenerateOneOf() {
            val schema = SchemaDefinition.OneOf(
                schemas = listOf(
                    SchemaDefinition.Object(
                        properties = mapOf(
                            "type" to SchemaDefinition.Primitive("string"),
                            "value" to SchemaDefinition.Primitive("string")
                        ),
                        required = setOf("type", "value"),
                        additionalProperties = null
                    ),
                    SchemaDefinition.Object(
                        properties = mapOf(
                            "type" to SchemaDefinition.Primitive("string"),
                            "amount" to SchemaDefinition.Primitive("number")
                        ),
                        required = setOf("type", "amount"),
                        additionalProperties = null
                    )
                )
            )
            
            val schemas = mapOf("Action" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/Action.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("sealed class Action"))
            assertTrue(content.contains("data class Option0"))
            assertTrue(content.contains("data class Option1"))
        }
        
        @Test
        @DisplayName("Should handle snake_case to camelCase conversion")
        fun testSnakeCaseToCamelCase() {
            val schema = SchemaDefinition.Object(
                properties = mapOf(
                    "user_name" to SchemaDefinition.Primitive("string"),
                    "first_name" to SchemaDefinition.Primitive("string"),
                    "last_updated_at" to SchemaDefinition.Primitive("string")
                ),
                required = setOf("user_name"),
                additionalProperties = null
            )
            
            val schemas = mapOf("UserProfile" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/UserProfile.kt")
            val content = generatedFile.readText()
            
            // Property names should be camelCase
            assertTrue(content.contains("val userName: String"))
            assertTrue(content.contains("val firstName: String?"))
            assertTrue(content.contains("val lastUpdatedAt: String?"))
            
            // SerialName annotations should preserve snake_case
            assertTrue(content.contains("@SerialName(\"user_name\")"))
            assertTrue(content.contains("@SerialName(\"first_name\")"))
            assertTrue(content.contains("@SerialName(\"last_updated_at\")"))
        }
        
        @Test
        @DisplayName("Should generate type aliases for primitive arrays")
        fun testGenerateArrayTypeAlias() {
            val schema = SchemaDefinition.Array(
                items = SchemaDefinition.Primitive("string")
            )
            
            val schemas = mapOf("StringList" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/StringList.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("typealias StringList = List<String>"))
        }
        
        @Test
        @DisplayName("Should handle nested objects")
        fun testGenerateNestedObjects() {
            val profileSchema = SchemaDefinition.Object(
                properties = mapOf(
                    "bio" to SchemaDefinition.Primitive("string"),
                    "avatar" to SchemaDefinition.Primitive("string")
                ),
                required = emptySet(),
                additionalProperties = null
            )
            
            val userSchema = SchemaDefinition.Object(
                properties = mapOf(
                    "id" to SchemaDefinition.Primitive("string"),
                    "profile" to profileSchema
                ),
                required = setOf("id"),
                additionalProperties = null
            )
            
            val schemas = mapOf("User" to userSchema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/User.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("data class User"))
            assertTrue(content.contains("val profile: Profile?"))
            assertTrue(content.contains("data class Profile"))
            assertTrue(content.contains("val bio: String?"))
            assertTrue(content.contains("val avatar: String?"))
        }
        
        @Test
        @DisplayName("Should handle references")
        fun testGenerateWithReferences() {
            val addressSchema = SchemaDefinition.Object(
                properties = mapOf(
                    "street" to SchemaDefinition.Primitive("string"),
                    "city" to SchemaDefinition.Primitive("string")
                ),
                required = setOf("city"),
                additionalProperties = null
            )
            
            val userSchema = SchemaDefinition.Object(
                properties = mapOf(
                    "name" to SchemaDefinition.Primitive("string"),
                    "address" to SchemaDefinition.Reference("Address")
                ),
                required = setOf("name"),
                additionalProperties = null
            )
            
            val schemas = mapOf(
                "Address" to addressSchema,
                "User" to userSchema
            )
            generator.generate(schemas, tempDir.absolutePath)
            
            val userFile = File(tempDir, "io/near/jsonrpc/types/generated/User.kt")
            assertTrue(userFile.exists())
            
            val userContent = userFile.readText()
            assertTrue(userContent.contains("val address: Address?"))
            
            val addressFile = File(tempDir, "io/near/jsonrpc/types/generated/Address.kt")
            assertTrue(addressFile.exists())
        }
        
        @Test
        @DisplayName("Should handle additional properties as Map")
        fun testGenerateAdditionalProperties() {
            val schema = SchemaDefinition.Object(
                properties = mapOf(
                    "id" to SchemaDefinition.Primitive("string")
                ),
                required = setOf("id"),
                additionalProperties = SchemaDefinition.Primitive("string")
            )
            
            val schemas = mapOf("Metadata" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/Metadata.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("val additionalProperties: Map<String, String>? = null"))
        }
        
        @Test
        @DisplayName("Should generate anyOf as JsonElement")
        fun testGenerateAnyOf() {
            val schema = SchemaDefinition.AnyOf(
                schemas = listOf(
                    SchemaDefinition.Primitive("string"),
                    SchemaDefinition.Primitive("number"),
                    SchemaDefinition.Primitive("boolean")
                )
            )
            
            val schemas = mapOf("FlexibleType" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/FlexibleType.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("typealias FlexibleType = JsonElement"))
        }
        
        @Test
        @DisplayName("Should handle NEAR-specific types correctly")
        fun testGenerateNearTypes() {
            val blockSchema = SchemaDefinition.Object(
                properties = mapOf(
                    "author" to SchemaDefinition.Primitive("string"),
                    "header" to SchemaDefinition.Object(
                        properties = mapOf(
                            "height" to SchemaDefinition.Primitive("integer"),
                            "hash" to SchemaDefinition.Primitive("string"),
                            "prev_hash" to SchemaDefinition.Primitive("string")
                        ),
                        required = setOf("height", "hash"),
                        additionalProperties = null
                    ),
                    "chunks" to SchemaDefinition.Array(
                        items = SchemaDefinition.Reference("Chunk")
                    )
                ),
                required = setOf("author", "header"),
                additionalProperties = null
            )
            
            val schemas = mapOf("Block" to blockSchema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/Block.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("data class Block"))
            assertTrue(content.contains("val author: String"))
            assertTrue(content.contains("val header: Header"))
            assertTrue(content.contains("val chunks: List<Chunk>?"))
            assertTrue(content.contains("data class Header"))
            assertTrue(content.contains("@SerialName(\"prev_hash\")"))
            assertTrue(content.contains("val prevHash: String?"))
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle reserved keywords")
        fun testReservedKeywords() {
            val schema = SchemaDefinition.Object(
                properties = mapOf(
                    "class" to SchemaDefinition.Primitive("string"),
                    "interface" to SchemaDefinition.Primitive("string"),
                    "return" to SchemaDefinition.Primitive("string")
                ),
                required = emptySet(),
                additionalProperties = null
            )
            
            val schemas = mapOf("ReservedTest" to schema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/ReservedTest.kt")
            val content = generatedFile.readText()
            
            // Should escape reserved keywords with backticks
            assertTrue(content.contains("val `class`: String?"))
            assertTrue(content.contains("val `interface`: String?"))
            assertTrue(content.contains("val `return`: String?"))
        }
        
        @Test
        @DisplayName("Should handle deeply nested structures")
        fun testDeeplyNestedStructures() {
            val level3 = SchemaDefinition.Object(
                properties = mapOf("value" to SchemaDefinition.Primitive("string")),
                required = emptySet(),
                additionalProperties = null
            )
            
            val level2 = SchemaDefinition.Object(
                properties = mapOf("level3" to level3),
                required = emptySet(),
                additionalProperties = null
            )
            
            val level1 = SchemaDefinition.Object(
                properties = mapOf("level2" to level2),
                required = emptySet(),
                additionalProperties = null
            )
            
            val rootSchema = SchemaDefinition.Object(
                properties = mapOf("level1" to level1),
                required = emptySet(),
                additionalProperties = null
            )
            
            val schemas = mapOf("DeepNest" to rootSchema)
            generator.generate(schemas, tempDir.absolutePath)
            
            val generatedFile = File(tempDir, "io/near/jsonrpc/types/generated/DeepNest.kt")
            assertTrue(generatedFile.exists())
            
            val content = generatedFile.readText()
            assertTrue(content.contains("data class DeepNest"))
            assertTrue(content.contains("data class Level1"))
            assertTrue(content.contains("data class Level2"))
            assertTrue(content.contains("data class Level3"))
        }
    }
}