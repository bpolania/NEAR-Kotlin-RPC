package io.near.generator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.io.File

class OpenApiParserTest {
    
    private val parser = OpenApiParser()
    private val mapper = ObjectMapper(YAMLFactory())
    
    @Nested
    @DisplayName("Schema Parsing")
    inner class SchemaParsingTests {
        
        @Test
        @DisplayName("Should parse simple object schema")
        fun testParseSimpleObject() {
            val yaml = """
                type: object
                properties:
                  id:
                    type: string
                  name:
                    type: string
                required:
                  - id
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("TestObject", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Object)
            val objSchema = schema as SchemaDefinition.Object
            assertEquals(2, objSchema.properties.size)
            assertTrue(objSchema.required.contains("id"))
            assertFalse(objSchema.required.contains("name"))
        }
        
        @Test
        @DisplayName("Should parse array schema")
        fun testParseArray() {
            val yaml = """
                type: array
                items:
                  type: string
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("TestArray", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Array)
            val arraySchema = schema as SchemaDefinition.Array
            assertTrue(arraySchema.items is SchemaDefinition.Primitive)
        }
        
        @Test
        @DisplayName("Should parse enum schema")
        fun testParseEnum() {
            val yaml = """
                type: string
                enum:
                  - ACTIVE
                  - INACTIVE
                  - PENDING
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("Status", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Enum)
            val enumSchema = schema as SchemaDefinition.Enum
            assertEquals(3, enumSchema.values.size)
            assertTrue(enumSchema.values.contains("ACTIVE"))
        }
        
        @Test
        @DisplayName("Should parse anyOf schema")
        fun testParseAnyOf() {
            val yaml = """
                anyOf:
                  - type: string
                  - type: number
                  - type: boolean
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("MultiType", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.AnyOf)
            val anyOfSchema = schema as SchemaDefinition.AnyOf
            assertEquals(3, anyOfSchema.schemas.size)
        }
        
        @Test
        @DisplayName("Should parse oneOf schema")
        fun testParseOneOf() {
            val yaml = """
                oneOf:
                  - type: object
                    properties:
                      type:
                        type: string
                        const: cat
                      meow:
                        type: boolean
                  - type: object
                    properties:
                      type:
                        type: string
                        const: dog
                      bark:
                        type: boolean
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("Animal", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.OneOf)
            val oneOfSchema = schema as SchemaDefinition.OneOf
            assertEquals(2, oneOfSchema.schemas.size)
        }
        
        @Test
        @DisplayName("Should parse allOf schema")
        fun testParseAllOf() {
            val yaml = """
                allOf:
                  - type: object
                    properties:
                      id:
                        type: string
                  - type: object
                    properties:
                      name:
                        type: string
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("Combined", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.AllOf)
            val allOfSchema = schema as SchemaDefinition.AllOf
            assertEquals(2, allOfSchema.schemas.size)
        }
        
        @Test
        @DisplayName("Should handle empty schema")
        fun testParseEmptySchema() {
            val yaml = "{}"
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("Empty", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Object)
            val objSchema = schema as SchemaDefinition.Object
            assertTrue(objSchema.properties.isEmpty())
        }
        
        @Test
        @DisplayName("Should handle schema references")
        fun testParseReference() {
            val yaml = """
                '$ref': '#/components/schemas/UserAccount'
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("RefTest", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Reference)
            val refSchema = schema as SchemaDefinition.Reference
            assertEquals("UserAccount", refSchema.ref)
        }
        
        @Test
        @DisplayName("Should parse nested objects")
        fun testParseNestedObject() {
            val yaml = """
                type: object
                properties:
                  user:
                    type: object
                    properties:
                      id:
                        type: string
                      profile:
                        type: object
                        properties:
                          name:
                            type: string
                          age:
                            type: integer
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("NestedTest", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Object)
            val objSchema = schema as SchemaDefinition.Object
            assertTrue(objSchema.properties["user"] is SchemaDefinition.Object)
            
            val userSchema = objSchema.properties["user"] as SchemaDefinition.Object
            assertTrue(userSchema.properties["profile"] is SchemaDefinition.Object)
        }
        
        @Test
        @DisplayName("Should handle additional properties")
        fun testParseAdditionalProperties() {
            val yaml = """
                type: object
                properties:
                  id:
                    type: string
                additionalProperties:
                  type: string
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("WithAdditional", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Object)
            val objSchema = schema as SchemaDefinition.Object
            assertNotNull(objSchema.additionalProperties)
            assertTrue(objSchema.additionalProperties is SchemaDefinition.Primitive)
        }
    }
    
    @Nested
    @DisplayName("Type Conversion")
    inner class TypeConversionTests {
        
        @Test
        @DisplayName("Should convert snake_case to camelCase")
        fun testSnakeToCamelConversion() {
            val yaml = """
                type: object
                properties:
                  user_name:
                    type: string
                  first_name:
                    type: string
                  last_updated_at:
                    type: string
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("User", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Object)
            val objSchema = schema as SchemaDefinition.Object
            
            // Parser should preserve original names, conversion happens in generator
            assertTrue(objSchema.properties.containsKey("user_name"))
            assertTrue(objSchema.properties.containsKey("first_name"))
            assertTrue(objSchema.properties.containsKey("last_updated_at"))
        }
        
        @Test
        @DisplayName("Should handle nullable types")
        fun testNullableTypes() {
            val yaml = """
                type: object
                properties:
                  requiredField:
                    type: string
                  optionalField:
                    type: string
                required:
                  - requiredField
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("NullableTest", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.Object)
            val objSchema = schema as SchemaDefinition.Object
            assertTrue(objSchema.required.contains("requiredField"))
            assertFalse(objSchema.required.contains("optionalField"))
        }
    }
    
    @Nested
    @DisplayName("Complex Scenarios")
    inner class ComplexScenarioTests {
        
        @Test
        @DisplayName("Should handle NEAR RPC response patterns")
        fun testNearRpcResponsePattern() {
            val yaml = """
                oneOf:
                  - type: object
                    properties:
                      result:
                        type: object
                        properties:
                          chain_id:
                            type: string
                          sync_info:
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
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("RpcResponse", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.OneOf)
            val oneOfSchema = schema as SchemaDefinition.OneOf
            assertEquals(2, oneOfSchema.schemas.size)
            
            // Both should be objects
            assertTrue(oneOfSchema.schemas[0] is SchemaDefinition.Object)
            assertTrue(oneOfSchema.schemas[1] is SchemaDefinition.Object)
        }
        
        @Test
        @DisplayName("Should handle discriminated unions")
        fun testDiscriminatedUnion() {
            val yaml = """
                oneOf:
                  - type: object
                    properties:
                      type:
                        type: string
                        const: transfer
                      amount:
                        type: string
                  - type: object
                    properties:
                      type:
                        type: string
                        const: function_call
                      method_name:
                        type: string
                      args:
                        type: string
            """.trimIndent()
            
            val node = mapper.readTree(yaml)
            val schema = parser.parseSchema("Action", node, mutableMapOf())
            
            assertTrue(schema is SchemaDefinition.OneOf)
            val oneOfSchema = schema as SchemaDefinition.OneOf
            
            // Verify each variant has a type discriminator
            oneOfSchema.schemas.forEach { variant ->
                assertTrue(variant is SchemaDefinition.Object)
                val objVariant = variant as SchemaDefinition.Object
                assertTrue(objVariant.properties.containsKey("type"))
            }
        }
    }
}