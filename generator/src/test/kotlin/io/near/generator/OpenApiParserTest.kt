package io.near.generator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.assertThrows
import java.io.File

/**
 * Unit tests for the OpenApiParser
 */
class OpenApiParserTest {
    
    @TempDir
    lateinit var tempDir: File
    
    @Test
    @DisplayName("Should parse basic OpenAPI spec structure")
    fun testBasicParsing() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
              description: Test API description
            components:
              schemas:
                SimpleType:
                  type: object
                  properties:
                    id:
                      type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "basic.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        assertEquals("Test API", spec.title)
        assertEquals("1.0.0", spec.version)
        assertEquals(1, spec.schemas.size)
        assertTrue(spec.schemas.containsKey("SimpleType"))
    }
    
    @Test
    @DisplayName("Should handle missing title and version gracefully")
    fun testMissingMetadata() {
        val specContent = """
            openapi: 3.0.0
            components:
              schemas:
                TestType:
                  type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "minimal.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        // Parser returns empty string if not found
        assertEquals("", spec.title)
        assertEquals("", spec.version)
        assertEquals(1, spec.schemas.size)
    }
    
    @Test
    @DisplayName("Should parse various property types correctly")
    fun testPropertyTypeParsing() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            components:
              schemas:
                MixedTypes:
                  type: object
                  properties:
                    string_prop:
                      type: string
                    int_prop:
                      type: integer
                    bool_prop:
                      type: boolean
                    array_prop:
                      type: array
                      items:
                        type: string
                    object_prop:
                      type: object
                      properties:
                        nested:
                          type: string
                    ref_prop:
                      ${'$'}ref: '#/components/schemas/OtherType'
                OtherType:
                  type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "mixed.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val schema = spec.schemas["MixedTypes"] as SchemaDefinition.Object
        assertEquals(6, schema.properties.size)
        
        // Check property types
        assertTrue(schema.properties["string_prop"] is Property.Primitive)
        assertTrue(schema.properties["int_prop"] is Property.Primitive)
        assertTrue(schema.properties["bool_prop"] is Property.Primitive)
        assertTrue(schema.properties["array_prop"] is Property.Array)
        assertTrue(schema.properties["object_prop"] is Property.Object)
        assertTrue(schema.properties["ref_prop"] is Property.Reference)
        
        val refProp = schema.properties["ref_prop"] as Property.Reference
        assertEquals("OtherType", refProp.ref)
    }
    
    @Test
    @DisplayName("Should handle enum schemas")
    fun testEnumParsing() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            components:
              schemas:
                Status:
                  type: string
                  enum:
                    - ACTIVE
                    - INACTIVE
                    - PENDING
                  description: Status enum
        """.trimIndent()
        
        val specFile = File(tempDir, "enum.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val enumSchema = spec.schemas["Status"] as SchemaDefinition.Enum
        assertEquals(3, enumSchema.values.size)
        assertTrue(enumSchema.values.contains("ACTIVE"))
        assertTrue(enumSchema.values.contains("INACTIVE"))
        assertTrue(enumSchema.values.contains("PENDING"))
        // Note: Enum doesn't have description in our model
    }
    
    @Test
    @DisplayName("Should handle oneOf schemas")
    fun testOneOfParsing() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            components:
              schemas:
                Result:
                  oneOf:
                    - type: object
                      properties:
                        success:
                          type: boolean
                    - type: object
                      properties:
                        error:
                          type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "oneof.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val oneOfSchema = spec.schemas["Result"] as SchemaDefinition.OneOf
        assertEquals(2, oneOfSchema.options.size)
        
        val firstOption = oneOfSchema.options[0] as SchemaDefinition.Object
        assertTrue(firstOption.properties.containsKey("success"))
        
        val secondOption = oneOfSchema.options[1] as SchemaDefinition.Object
        assertTrue(secondOption.properties.containsKey("error"))
    }
    
    @Test
    @DisplayName("Should handle anyOf schemas")
    fun testAnyOfParsing() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            components:
              schemas:
                FlexibleType:
                  anyOf:
                    - type: string
                    - type: number
                    - type: boolean
        """.trimIndent()
        
        val specFile = File(tempDir, "anyof.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val anyOfSchema = spec.schemas["FlexibleType"] as SchemaDefinition.AnyOf
        assertEquals(3, anyOfSchema.options.size)
        
        assertTrue(anyOfSchema.options[0] is SchemaDefinition.Primitive)
        assertTrue(anyOfSchema.options[1] is SchemaDefinition.Primitive)
        assertTrue(anyOfSchema.options[2] is SchemaDefinition.Primitive)
    }
    
    @Test
    @DisplayName("Should handle allOf schemas")
    fun testAllOfParsing() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            components:
              schemas:
                Base:
                  type: object
                  properties:
                    id:
                      type: string
                Extended:
                  allOf:
                    - ${'$'}ref: '#/components/schemas/Base'
                    - type: object
                      properties:
                        name:
                          type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "allof.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val allOfSchema = spec.schemas["Extended"] as SchemaDefinition.AllOf
        assertEquals(2, allOfSchema.schemas.size)
        
        assertTrue(allOfSchema.schemas[0] is SchemaDefinition.Reference)
        assertTrue(allOfSchema.schemas[1] is SchemaDefinition.Object)
    }
    
    @Test
    @DisplayName("Should handle required fields")
    fun testRequiredFields() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
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
        """.trimIndent()
        
        val specFile = File(tempDir, "required.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        val schema = spec.schemas["User"] as SchemaDefinition.Object
        assertEquals(2, schema.required.size)
        assertTrue(schema.required.contains("id"))
        assertTrue(schema.required.contains("name"))
        assertFalse(schema.required.contains("email"))
    }
    
    @Test
    @DisplayName("Should handle array schemas with various item types")
    fun testArraySchemas() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            components:
              schemas:
                StringArray:
                  type: array
                  items:
                    type: string
                ObjectArray:
                  type: array
                  items:
                    type: object
                    properties:
                      value:
                        type: string
                RefArray:
                  type: array
                  items:
                    ${'$'}ref: '#/components/schemas/Item'
                Item:
                  type: object
                  properties:
                    id:
                      type: string
        """.trimIndent()
        
        val specFile = File(tempDir, "arrays.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        // Check StringArray
        val stringArray = spec.schemas["StringArray"] as SchemaDefinition.Array
        assertTrue(stringArray.items is Property.Primitive)
        
        // Check ObjectArray
        val objectArray = spec.schemas["ObjectArray"] as SchemaDefinition.Array
        assertTrue(objectArray.items is Property.Object)
        
        // Check RefArray
        val refArray = spec.schemas["RefArray"] as SchemaDefinition.Array
        assertTrue(refArray.items is Property.Reference)
        val refItem = refArray.items as Property.Reference
        assertEquals("Item", refItem.ref)
    }
    
    @Test
    @DisplayName("Should handle empty object schemas")
    fun testEmptyObjectSchemas() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            components:
              schemas:
                EmptyObject1:
                  type: object
                  properties: {}
                EmptyObject2:
                  type: object
                EmptyObject3:
                  type: object
                  additionalProperties: false
        """.trimIndent()
        
        val specFile = File(tempDir, "empty.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        assertEquals(3, spec.schemas.size)
        
        // Parser now creates Empty types for objects with no properties
        assertTrue(spec.schemas["EmptyObject1"] is SchemaDefinition.Empty)
        assertTrue(spec.schemas["EmptyObject2"] is SchemaDefinition.Empty)  
        assertTrue(spec.schemas["EmptyObject3"] is SchemaDefinition.Empty)
    }
    
    @Test
    @DisplayName("Should handle spec without schemas section")
    fun testNoSchemas() {
        val specContent = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0
            paths:
              /test:
                get:
                  summary: Test endpoint
        """.trimIndent()
        
        val specFile = File(tempDir, "no-schemas.yaml")
        specFile.writeText(specContent)
        
        val parser = OpenApiParser()
        val spec = parser.parse(specFile)
        
        assertEquals("Test", spec.title)
        assertEquals("1.0", spec.version)
        assertTrue(spec.schemas.isEmpty())
    }
}