package io.near.generator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

/**
 * Parses OpenAPI specification and provides access to schemas
 */
class OpenApiParser {
    private val jsonMapper = ObjectMapper()
    private val yamlMapper = ObjectMapper(YAMLFactory())
    
    fun parse(specFile: File): OpenApiSpec {
        val mapper = if (specFile.extension == "yaml" || specFile.extension == "yml") {
            yamlMapper
        } else {
            jsonMapper
        }
        
        val root = mapper.readTree(specFile)
        val schemas = mutableMapOf<String, SchemaDefinition>()
        
        // Parse components/schemas
        root.path("components").path("schemas").fields().forEach { (name, node) ->
            schemas[name] = parseSchema(name, node)
        }
        
        return OpenApiSpec(
            title = root.path("info").path("title").asText(""),
            version = root.path("info").path("version").asText(""),
            schemas = schemas
        )
    }
    
    private fun parseSchema(name: String, node: JsonNode): SchemaDefinition {
        return when {
            // Handle anyOf
            node.has("anyOf") -> {
                val options = node.path("anyOf").map { parseSchema("", it) }
                SchemaDefinition.AnyOf(name, options)
            }
            
            // Handle oneOf
            node.has("oneOf") -> {
                val options = node.path("oneOf").map { parseSchema("", it) }
                SchemaDefinition.OneOf(name, options)
            }
            
            // Handle allOf
            node.has("allOf") -> {
                val schemas = node.path("allOf").map { parseSchema("", it) }
                SchemaDefinition.AllOf(name, schemas)
            }
            
            // Handle enum
            node.has("enum") -> {
                val values = node.path("enum").map { it.asText() }
                SchemaDefinition.Enum(
                    name = name,
                    values = values,
                    type = node.path("type").asText("string")
                )
            }
            
            // Handle $ref
            node.has("\$ref") -> {
                val ref = node.path("\$ref").asText()
                SchemaDefinition.Reference(name, ref.substringAfterLast("/"))
            }
            
            // Handle object type
            node.path("type").asText() == "object" || node.has("properties") -> {
                val properties = mutableMapOf<String, Property>()
                node.path("properties").fields().forEach { (propName, propNode) ->
                    properties[propName] = parseProperty(propName, propNode)
                }
                
                val required = node.path("required").map { it.asText() }.toSet()
                
                SchemaDefinition.Object(
                    name = name,
                    properties = properties,
                    required = required,
                    description = node.path("description").asText(null)
                )
            }
            
            // Handle array type
            node.path("type").asText() == "array" -> {
                val items = node.path("items")
                SchemaDefinition.Array(
                    name = name,
                    items = if (items.isMissingNode) {
                        Property.Primitive("item", "string", false)
                    } else {
                        parseProperty("item", items)
                    }
                )
            }
            
            // Handle primitive types
            node.has("type") -> {
                val type = node.path("type").asText()
                SchemaDefinition.Primitive(
                    name = name,
                    type = type,
                    format = node.path("format").asText(null),
                    description = node.path("description").asText(null)
                )
            }
            
            // Empty schema - treat as string
            else -> {
                SchemaDefinition.Empty(name)
            }
        }
    }
    
    private fun parseProperty(name: String, node: JsonNode): Property {
        return when {
            node.has("\$ref") -> {
                val ref = node.path("\$ref").asText()
                Property.Reference(name, ref.substringAfterLast("/"))
            }
            
            node.path("type").asText() == "array" -> {
                val items = node.path("items")
                Property.Array(
                    name = name,
                    items = if (items.isMissingNode) {
                        Property.Primitive("item", "string", false)
                    } else {
                        parseProperty("item", items)
                    }
                )
            }
            
            node.path("type").asText() == "object" -> {
                val schema = parseSchema(name, node)
                Property.Object(name, schema as? SchemaDefinition.Object ?: SchemaDefinition.Empty(name))
            }
            
            else -> {
                Property.Primitive(
                    name = name,
                    type = node.path("type").asText("string"),
                    nullable = node.path("nullable").asBoolean(false),
                    format = node.path("format").asText(null),
                    description = node.path("description").asText(null)
                )
            }
        }
    }
}

/**
 * Represents the parsed OpenAPI specification
 */
data class OpenApiSpec(
    val title: String,
    val version: String,
    val schemas: Map<String, SchemaDefinition>
)

/**
 * Represents different types of schema definitions
 */
sealed class SchemaDefinition {
    abstract val name: String
    
    data class Object(
        override val name: String,
        val properties: Map<String, Property>,
        val required: Set<String>,
        val description: String? = null
    ) : SchemaDefinition()
    
    data class Enum(
        override val name: String,
        val values: List<String>,
        val type: String = "string"
    ) : SchemaDefinition()
    
    data class Array(
        override val name: String,
        val items: Property
    ) : SchemaDefinition()
    
    data class Primitive(
        override val name: String,
        val type: String,
        val format: String? = null,
        val description: String? = null
    ) : SchemaDefinition()
    
    data class AnyOf(
        override val name: String,
        val options: List<SchemaDefinition>
    ) : SchemaDefinition()
    
    data class OneOf(
        override val name: String,
        val options: List<SchemaDefinition>
    ) : SchemaDefinition()
    
    data class AllOf(
        override val name: String,
        val schemas: List<SchemaDefinition>
    ) : SchemaDefinition()
    
    data class Reference(
        override val name: String,
        val ref: String
    ) : SchemaDefinition()
    
    data class Empty(
        override val name: String
    ) : SchemaDefinition()
}

/**
 * Represents a property in an object schema
 */
sealed class Property {
    abstract val name: String
    
    data class Primitive(
        override val name: String,
        val type: String,
        val nullable: Boolean,
        val format: String? = null,
        val description: String? = null
    ) : Property()
    
    data class Array(
        override val name: String,
        val items: Property
    ) : Property()
    
    data class Object(
        override val name: String,
        val schema: SchemaDefinition
    ) : Property()
    
    data class Reference(
        override val name: String,
        val ref: String
    ) : Property()
}