package ai.ancf.lmos.wot.thing

import ai.ancf.lmos.wot.JsonMapper
import ai.ancf.lmos.wot.WoTDSL
import ai.ancf.lmos.wot.security.BasicSecurityScheme
import ai.ancf.lmos.wot.security.SecurityScheme
import ai.ancf.lmos.wot.thing.action.ThingAction
import ai.ancf.lmos.wot.thing.event.ThingEvent
import ai.ancf.lmos.wot.thing.form.Form
import ai.ancf.lmos.wot.thing.schema.*
import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*

const val DEFAULT_CONTEXT = "https://www.w3.org/2022/wot/td/v1.1"
const val DEFAULT_TYPE = "Thing"

/**
 * Represents a Thing entity in the Web of Things (WoT) model.
 *
 * A `Thing` is a digital representation of a physical or abstract entity, containing metadata, capabilities,
 * and affordances such as properties, actions, and events.
 *
 * @property id Unique identifier for the Thing, typically a UUID URI.
 * @property objectType Specifies the type of the Thing using a URI.
 * @property objectContext Defines the JSON-LD context to expand terms within the Thing's description.
 * @property title Human-readable title of the Thing.
 * @property titles Multilingual titles of the Thing, with language codes as keys.
 * @property description Human-readable description of the Thing.
 * @property descriptions Multilingual descriptions of the Thing, with language codes as keys.
 * @property properties A map of properties that describe the state of the Thing.
 * @property actions A map of actions that the Thing can perform.
 * @property events A map of events that the Thing can emit.
 * @property forms List of interaction patterns that specify how to interact with the Thing.
 * @property security Security mechanisms used by the Thing, represented by identifiers defined in securityDefinitions.
 * @property securityDefinitions Definitions of security schemes, specifying how security is applied to interactions.
 * @property base Base URI to resolve relative URIs within the Thing description.
 * @property version Version information for the Thing description.
 * @property created Creation timestamp for the Thing in ISO 8601 format.
 * @property modified Last modification timestamp for the Thing in ISO 8601 format.
 * @property support URL to obtain support for the Thing.
 * @property links List of additional links to resources related to the Thing.
 * @property profile List of profiles that describe the capabilities and interactions of the Thing.
 * @property schemaDefinitions Definitions for data schemas used by the Thing’s properties, actions, or events.
 * @property uriVariables URI variables for dynamic references within forms.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@WoTDSL
data class ThingDescription @JsonCreator constructor(
    @JsonProperty("id") override var id: String = getRandomThingId(),
    @JsonProperty("@type") @JsonInclude(NON_NULL) override var objectType: Type? = Type(
        DEFAULT_TYPE),
    @JsonProperty("@context") @JsonInclude(NON_NULL) override var objectContext: Context? = Context(
        DEFAULT_CONTEXT),
    @JsonInclude(NON_EMPTY) override var title: String? = null,
    @JsonInclude(NON_EMPTY) override var titles: MutableMap<String, String>? = null,
    @JsonInclude(NON_EMPTY) override var description: String? = null,
    @JsonInclude(NON_EMPTY) override var descriptions: MutableMap<String, String>? = null,
    @JsonInclude(NON_EMPTY) override var properties: MutableMap<String, PropertyAffordance<*>> = mutableMapOf(),
    @JsonInclude(NON_EMPTY) override var actions: MutableMap<String, ActionAffordance<*, *>> = mutableMapOf(),
    @JsonInclude(NON_EMPTY) override var events: MutableMap<String, EventAffordance<*, *, *>> = mutableMapOf(),
    @JsonInclude(NON_EMPTY) override var forms: List<Form> = emptyList(),
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY]) @JsonInclude(NON_EMPTY) override var security: List<String> = emptyList(),
    @JsonInclude(NON_EMPTY) override var securityDefinitions: MutableMap<String, SecurityScheme> = mutableMapOf(),
    @JsonInclude(NON_EMPTY) override var base: String? = null,
    @JsonInclude(NON_EMPTY) override var version: VersionInfo? = null,
    @JsonInclude(NON_EMPTY) override var created: String? = null,
    @JsonInclude(NON_EMPTY) override var modified: String? = null,
    @JsonInclude(NON_EMPTY) override var support: String? = null,
    @JsonInclude(NON_EMPTY) override var links: MutableList<Link> = mutableListOf(),
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY]) @JsonInclude(NON_EMPTY) override var profile: List<String>? = null,
    @JsonInclude(NON_EMPTY) override var schemaDefinitions: MutableMap<String, DataSchema<Any>>? = null,
    @JsonInclude(NON_EMPTY) override var uriVariables: MutableMap<String, DataSchema<Any>>? = null
) : WoTThingDescription {

    fun stringProperty(name: String, configure: StringProperty.() -> Unit) {
        this.properties[name] = StringProperty().apply(configure)
    }
    fun intProperty(name: String, configure: IntProperty.() -> Unit) {
        this.properties[name] = IntProperty().apply(configure)
    }

    fun booleanProperty(name: String, configure: BooleanProperty.() -> Unit) {
        this.properties[name] = BooleanProperty().apply(configure)
    }
    fun objectProperty(name: String, configure: ObjectProperty.() -> Unit) {
        this.properties[name] = ObjectProperty().apply(configure)
    }
    fun numberProperty(name: String, configure: NumberProperty.() -> Unit) {
        this.properties[name] = NumberProperty().apply(configure)
    }

    fun nullProperty(name: String, configure: NullProperty.() -> Unit) {
        this.properties[name] = NullProperty().apply(configure)
    }

    fun <T> arrayProperty(name: String, configure: ArrayProperty<T>.() -> Unit) {
        this.properties[name] = ArrayProperty<T>().apply(configure)
    }

    fun <I : Any, O : Any> action(name: String, configure: ThingAction<I, O>.() -> Unit) {
        val action = ThingAction<I, O>().apply(configure)
        actions[name] = action
    }

    fun <T, S, C> event(name: String, configure: ThingEvent<T, S, C>.() -> Unit) {
        val event = ThingEvent<T, S, C>().apply(configure)
        events[name] = event
    }

    fun basicSecurityScheme(name: String, configure: BasicSecurityScheme.() -> Unit) {
        val securityScheme = BasicSecurityScheme().apply(configure)
        securityDefinitions[name] = securityScheme
    }

    fun getPropertiesByObjectType(objectType: String): Map<String, PropertyAffordance<*>> {
        return getPropertiesByExpandedObjectType(getExpandedObjectType(objectType))
    }

    private fun getPropertiesByExpandedObjectType(objectType: String): Map<String, PropertyAffordance<*>> {
        return properties.filter { (_, property) ->
            property.objectType?.defaultType?.let { getExpandedObjectType(it) } == objectType
        }.toMap()
    }

    fun getExpandedObjectType(objectType: String): String {

        val parts = objectType.split(":", limit = 2)
        val prefix = if (parts.size == 2) parts[0] else null
        val suffix = parts.last()

        return objectContext?.getUrl(prefix)?.let { "$it#$suffix" } ?: objectType
    }

    fun toJson() : String{
        return try {
            JsonMapper.instance.writeValueAsString(this)
        } catch (e: JsonProcessingException) {
            log.warn("Unable to write json", e)
            throw e
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ThingDescription::class.java)

        /**
         * Parses a JSON string into a Thing object.
         *
         * @param json JSON string to parse.
         * @return A Thing instance if parsing is successful, otherwise null.
         */
        fun fromJson(json: String): ThingDescription {
            return try {
                JsonMapper.instance.readValue<ThingDescription>(json)
            } catch (e: IOException) {
                log.warn("Unable to read json", e)
                throw e
            }
        }

        /**
         * Parses bytes into a Thing object.
         *
         * @param bytes bytes to parse.
         * @return A Thing instance if parsing is successful, otherwise null.
         */
        fun fromBytes(bytes: ByteArray): ThingDescription {
            return try {
                JsonMapper.instance.readValue<ThingDescription>(bytes)
            } catch (e: IOException) {
                log.warn("Unable to read json", e)
                throw e
            }
        }

        /**
         * Parses a JSON file into a Thing object.
         *
         * @param file JSON file to parse.
         * @return A Thing instance if parsing is successful, otherwise null.
         */
        fun fromJson(file: File): ThingDescription {
            return try {
                JsonMapper.instance.readValue<ThingDescription>(file)
            } catch (e: IOException) {
                log.warn("Unable to read json", e)
                throw e
            }
        }

        /**
         * Converts a Map into a Thing object.
         *
         * @param map Map representing the Thing structure.
         * @return A Thing instance converted from the map.
         */
        fun fromMap(map: Map<*, *>): ThingDescription {
            return JsonMapper.instance.convertValue<ThingDescription>(map)
        }
    }
}

fun getRandomThingId() = "urn:uuid:" + UUID.randomUUID().toString()


/**
 * DSL function to create and configure a [ThingDescription] instance.
 *
 * @param id The unique identifier for the Thing. Defaults to a random UUID.
 * @param configure Lambda expression to configure the Thing properties.
 * @return Configured [ThingDescription] instance.
 */
fun thingDescription(configure: ThingDescription.() -> Unit): ThingDescription {
    return ThingDescription().apply(configure)
}