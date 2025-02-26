package ai.ancf.lmos.kotlinsdk.base

import ai.ancf.lmos.wot.thing.schema.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

object DataSchemaBuilder {

    // TODO: Move to kotlin-wot

    fun mapTypeToSchema(type: KType): DataSchema<out Any> {
        return when (type.classifier) {
            Int::class -> IntegerSchema()
            Float::class -> NumberSchema()
            Double::class -> NumberSchema()
            Long::class -> NumberSchema()
            String::class -> StringSchema()
            Boolean::class -> BooleanSchema()
            List::class, Set::class -> {
                val genericType = type.arguments.firstOrNull()?.type
                val itemSchema = if (genericType != null) {
                    mapTypeToSchema(genericType)
                } else {
                    StringSchema()
                }
                ArraySchema(items = itemSchema)
            }
            IntArray::class -> ArraySchema(items = IntegerSchema())
            FloatArray::class -> ArraySchema(items = NumberSchema())
            DoubleArray::class -> ArraySchema(items = NumberSchema())
            NumberSchema::class -> ArraySchema(items = NumberSchema())
            Array<String>::class -> ArraySchema(items = StringSchema())
            Array<Number>::class -> ArraySchema(items = NumberSchema())
            Array<Boolean>::class -> ArraySchema(items = BooleanSchema())
            Array<Int>::class -> ArraySchema(items = IntegerSchema())
            Array<Float>::class -> ArraySchema(items = NumberSchema())
            Array<Long>::class -> ArraySchema(items = NumberSchema())
            Unit::class -> NullSchema()
            else -> buildObjectSchema(type)
        }
    }

    private fun buildObjectSchema(type: KType): ObjectSchema {
        if (type.classifier == Map::class) {
            return buildMapSchema(type)
        }

        val kClass = type.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Type is not a valid class: $type")

        val (properties, required) = extractProperties(kClass)
        return ObjectSchema(properties, required)
    }

    private fun buildMapSchema(type: KType): ObjectSchema {
        val keyType = type.arguments.getOrNull(0)?.type
        val valueType = type.arguments.getOrNull(1)?.type

        val keySchema = if (keyType != null) mapTypeToSchema(keyType) else StringSchema()
        val valueSchema = if (valueType != null) mapTypeToSchema(valueType) else StringSchema()

        return ObjectSchema(
            properties = mutableMapOf(
                "keys" to ArraySchema(items = keySchema),
                "values" to ArraySchema(items = valueSchema)
            )
        )
    }

    private fun extractProperties(kClass: KClass<*>): Pair<MutableMap<String, DataSchema<*>>, MutableList<String>> {
        val properties: MutableMap<String, DataSchema<*>> = mutableMapOf()
        val required: MutableList<String> = mutableListOf()

        kClass.memberProperties.forEach { property ->
            val returnType = property.returnType
            val schemaForParam = mapTypeToSchema(returnType)
            properties[property.name] = schemaForParam
            if (!returnType.isMarkedNullable) {
                required.add(property.name)
            }
        }
        return Pair(properties, required)
    }
}