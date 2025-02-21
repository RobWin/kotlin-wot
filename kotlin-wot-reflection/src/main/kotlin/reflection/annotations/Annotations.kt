package ai.ancf.lmos.wot.reflection.annotations

import ai.ancf.lmos.wot.thing.DEFAULT_TYPE

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Thing(val id: String, val title: String, val description: String, val type : String= DEFAULT_TYPE)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Context(val prefix: String, val url : String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class VersionInfo(val instance : String, val model : String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Link(
    val href: String,
    val type: String = "",
    val rel: String = "",
    val anchor: String = "",
    val sizes: String = "",
    val hreflang: Array<String> = []
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Links(val values: Array<Link>)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Property(val title: String = "", val description: String = "", val readOnly: Boolean = false, val writeOnly: Boolean = false)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Action(val title: String = "", val description: String = "",
    val safe : Boolean = false, val idempotent : Boolean = false, val synchronous: Boolean = true
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionInput(val title: String = "", val description: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionOutput(val title: String = "", val description: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Event(val title: String = "", val description: String = "")

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegerSchema(
    val minimum: Int = Int.MIN_VALUE,
    val maximum: Int = Int.MAX_VALUE,
    val multipleOf: Int = 1
)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NumberSchema(
    val minimum: Double = Double.NEGATIVE_INFINITY,
    val maximum: Double = Double.POSITIVE_INFINITY,
    val multipleOf: Double = 0.0 // 0.0 means "not restricted"
)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class StringSchema(
    val minLength: Int = 0,
    val maxLength: Int = Int.MAX_VALUE,
    val pattern: String = "" // Regular expression
)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ArraySchema(
    val minItems: Int = 0,
    val maxItems: Int = Int.MAX_VALUE,
    val uniqueItems: Boolean = false
)