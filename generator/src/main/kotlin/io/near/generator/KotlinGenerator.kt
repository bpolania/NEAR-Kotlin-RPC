package io.near.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Generates Kotlin code using KotlinPoet from parsed OpenAPI schemas
 */
class KotlinGenerator(
    private val packageName: String,
    private val outputDir: File
) {
    private val processedTypes = mutableSetOf<String>()
    
    fun generate(spec: OpenApiSpec) {
        // Generate each schema
        spec.schemas.forEach { (name, schema) ->
            if (name !in processedTypes) {
                generateType(name, schema, spec.schemas)
            }
        }
    }
    
    private fun generateType(name: String, schema: SchemaDefinition, allSchemas: Map<String, SchemaDefinition>) {
        if (name in processedTypes) return
        processedTypes.add(name)
        
        val typeSpec = when (schema) {
            is SchemaDefinition.Object -> generateDataClass(schema, allSchemas)
            is SchemaDefinition.Enum -> generateEnum(schema)
            is SchemaDefinition.Empty -> generateEmptyType(schema)
            is SchemaDefinition.Primitive -> generateTypeAlias(schema)
            is SchemaDefinition.Array -> generateArrayType(schema, allSchemas)
            is SchemaDefinition.AnyOf -> generateUnionType(schema, allSchemas)
            is SchemaDefinition.OneOf -> generateUnionType(schema, allSchemas)
            is SchemaDefinition.AllOf -> generateMergedType(schema, allSchemas)
            is SchemaDefinition.Reference -> null // Skip references, they point to other types
        }
        
        typeSpec?.let { type ->
            val fileSpec = FileSpec.builder(packageName, name)
                .addType(type)
                .addImport("kotlinx.serialization", "Serializable", "SerialName")
                .addImport("kotlinx.serialization.json", "JsonElement")
                .build()
            
            fileSpec.writeTo(outputDir)
        }
    }
    
    private fun generateDataClass(schema: SchemaDefinition.Object, allSchemas: Map<String, SchemaDefinition>): TypeSpec {
        val className = ClassName(packageName, schema.name)
        
        // Object schemas should always have properties (empty ones become Empty type in parser)
        val builder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(Serializable::class)
        
        schema.description?.let {
            builder.addKdoc("%L", it)
        }
        
        val properties = schema.properties
        
        // Create primary constructor
        val constructorBuilder = FunSpec.constructorBuilder()
        
        properties.forEach { (propName, prop) ->
            val kotlinName = propName.snakeToCamelCase()
            var propertyType = getPropertyType(prop, allSchemas)
            val isRequired = propName in schema.required
            
            // Make optional types nullable
            if (!isRequired) {
                propertyType = propertyType.copy(nullable = true)
            }
            
            // Add property with SerialName annotation if needed
            val propertyBuilder = PropertySpec.builder(kotlinName, propertyType)
                .initializer(kotlinName)
                .mutable(false)
            
            if (kotlinName != propName) {
                propertyBuilder.addAnnotation(
                    AnnotationSpec.builder(SerialName::class)
                        .addMember("%S", propName)
                        .build()
                )
            }
            
            // Add description as KDoc
            when (prop) {
                is Property.Primitive -> prop.description?.let { 
                    propertyBuilder.addKdoc("%L", it) 
                }
                else -> {}
            }
            
            // Add to constructor with default value if not required
            val paramBuilder = ParameterSpec.builder(kotlinName, propertyType)
            if (!isRequired) {
                paramBuilder.defaultValue("null")
            }
            
            constructorBuilder.addParameter(paramBuilder.build())
            builder.addProperty(propertyBuilder.build())
        }
        
        builder.primaryConstructor(constructorBuilder.build())
        
        return builder.build()
    }
    
    private fun generateEnum(schema: SchemaDefinition.Enum): TypeSpec {
        val enumBuilder = TypeSpec.enumBuilder(schema.name)
            .addAnnotation(Serializable::class)
        
        schema.values.forEach { value ->
            val enumName = value.toUpperSnakeCase()
            val enumConstant = TypeSpec.anonymousClassBuilder()
                .addAnnotation(
                    AnnotationSpec.builder(SerialName::class)
                        .addMember("%S", value)
                        .build()
                )
                .build()
            
            enumBuilder.addEnumConstant(enumName, enumConstant)
        }
        
        return enumBuilder.build()
    }
    
    private fun generateEmptyType(schema: SchemaDefinition.Empty): TypeSpec {
        // Generate as object (singleton) for empty schemas
        val builder = TypeSpec.objectBuilder(schema.name)
            .addAnnotation(Serializable::class)
        
        // Add description if present
        val description = schema.description ?: "Empty schema converted to object"
        builder.addKdoc("%L", description)
        
        return builder.build()
    }
    
    private fun generateTypeAlias(schema: SchemaDefinition.Primitive): TypeSpec? {
        // For simple primitives, we could generate type aliases, but Kotlin doesn't serialize them well
        // Instead, generate a value class
        val kotlinType = mapPrimitiveType(schema.type, schema.format)
        
        return TypeSpec.classBuilder(schema.name)
            .addModifiers(KModifier.VALUE)
            .addAnnotation(Serializable::class)
            .addAnnotation(JvmInline::class)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", kotlinType)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("value", kotlinType)
                    .initializer("value")
                    .build()
            )
            .build()
    }
    
    private fun generateArrayType(schema: SchemaDefinition.Array, allSchemas: Map<String, SchemaDefinition>): TypeSpec? {
        // Arrays are handled inline, no separate type needed
        return null
    }
    
    private fun generateUnionType(schema: SchemaDefinition, allSchemas: Map<String, SchemaDefinition>): TypeSpec {
        // For anyOf/oneOf, create a sealed class with subclasses
        val sealedBuilder = TypeSpec.classBuilder(schema.name)
            .addModifiers(KModifier.SEALED)
            .addAnnotation(Serializable::class)
        
        when (schema) {
            is SchemaDefinition.AnyOf -> {
                // For anyOf, create a data class with all possible properties as nullable
                val allProperties = mutableMapOf<String, Property>()
                schema.options.forEach { option ->
                    when (option) {
                        is SchemaDefinition.Object -> {
                            option.properties.forEach { (name, prop) ->
                                allProperties[name] = prop
                            }
                        }
                        is SchemaDefinition.Primitive -> {
                            allProperties[option.type] = Property.Primitive(option.type, option.type, true)
                        }
                        else -> {}
                    }
                }
                
                if (allProperties.isNotEmpty()) {
                    return generateDataClass(
                        SchemaDefinition.Object(schema.name, allProperties, emptySet()),
                        allSchemas
                    )
                }
            }
            is SchemaDefinition.OneOf -> {
                // For oneOf, create actual subclasses
                schema.options.forEachIndexed { index, option ->
                    val subclassName = "${schema.name}Option${index + 1}"
                    val subclass = when (option) {
                        is SchemaDefinition.Object -> generateDataClass(
                            option.copy(name = subclassName),
                            allSchemas
                        )
                        else -> TypeSpec.objectBuilder(subclassName)
                            .addAnnotation(Serializable::class)
                            .build()
                    }
                    sealedBuilder.addType(subclass)
                }
            }
            else -> {}
        }
        
        return sealedBuilder.build()
    }
    
    private fun generateMergedType(schema: SchemaDefinition.AllOf, allSchemas: Map<String, SchemaDefinition>): TypeSpec {
        // Merge all schemas into one
        val mergedProperties = mutableMapOf<String, Property>()
        val required = mutableSetOf<String>()
        
        schema.schemas.forEach { subSchema ->
            when (subSchema) {
                is SchemaDefinition.Object -> {
                    mergedProperties.putAll(subSchema.properties)
                    required.addAll(subSchema.required)
                }
                is SchemaDefinition.Reference -> {
                    allSchemas[subSchema.ref]?.let { referenced ->
                        if (referenced is SchemaDefinition.Object) {
                            mergedProperties.putAll(referenced.properties)
                            required.addAll(referenced.required)
                        }
                    }
                }
                else -> {}
            }
        }
        
        return generateDataClass(
            SchemaDefinition.Object(schema.name, mergedProperties, required),
            allSchemas
        )
    }
    
    private fun getPropertyType(property: Property, allSchemas: Map<String, SchemaDefinition>): TypeName {
        return when (property) {
            is Property.Primitive -> {
                val baseType = mapPrimitiveType(property.type, property.format)
                if (property.nullable) baseType.copy(nullable = true) else baseType
            }
            is Property.Array -> {
                val itemType = getPropertyType(property.items, allSchemas)
                LIST.parameterizedBy(itemType)
            }
            is Property.Reference -> {
                ClassName(packageName, property.ref)
            }
            is Property.Object -> {
                // For inline objects, use JsonElement
                ClassName("kotlinx.serialization.json", "JsonElement")
            }
        }
    }
    
    private fun mapPrimitiveType(type: String, format: String?): TypeName {
        return when (type) {
            "string" -> when (format) {
                "byte" -> STRING  // Base64 encoded
                "binary" -> BYTE_ARRAY
                "date" -> STRING  // Could use LocalDate
                "date-time" -> STRING  // Could use Instant
                else -> STRING
            }
            "integer" -> when (format) {
                "int32" -> INT
                "int64" -> LONG
                else -> INT
            }
            "number" -> when (format) {
                "float" -> FLOAT
                "double" -> DOUBLE
                else -> DOUBLE
            }
            "boolean" -> BOOLEAN
            "array" -> LIST.parameterizedBy(ANY.copy(nullable = true))
            "object" -> ClassName("kotlinx.serialization.json", "JsonElement")
            else -> ANY.copy(nullable = true)
        }
    }
}

// Extension functions for string conversions
private fun String.snakeToCamelCase(): String {
    return split("_").mapIndexed { index, part ->
        if (index == 0) part else part.replaceFirstChar { it.uppercase() }
    }.joinToString("")
}

private fun String.toUpperSnakeCase(): String {
    return replace("-", "_")
        .replace(" ", "_")
        .uppercase()
        .let { if (it.first().isDigit()) "_$it" else it }
}