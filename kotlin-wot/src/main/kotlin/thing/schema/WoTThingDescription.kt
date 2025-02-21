package ai.ancf.lmos.wot.thing.schema

import ai.ancf.lmos.wot.WoTDSL
import ai.ancf.lmos.wot.security.SecurityScheme
import ai.ancf.lmos.wot.thing.action.ThingAction
import ai.ancf.lmos.wot.thing.event.ThingEvent
import ai.ancf.lmos.wot.thing.form.Form
import ai.ancf.lmos.wot.thing.form.Operation
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * Interface representing a Thing Description (TD) in a Web of Things context.
 */
interface WoTThingDescription : BaseSchema {

    /**
     * JSON-LD keyword to define short-hand names called terms that are used throughout a TD document.
     *
     * @return a URI or an array of URIs representing the context.
     */
    @get:JsonProperty("@context")
    var objectContext: Context? // Optional: anyURI or Array

    /**
     * Identifier of the Thing in form of a URI RFC3986.
     *
     * @return an optional URI identifier.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var id: String // Optional: anyURI

    /**
     * Provides version information.
     *
     * @return optional version information.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var version: VersionInfo? // Optional: VersionInfo

    /**
     * Provides information when the TD instance was created.
     *
     * @return the creation date and time.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var created: String? // Optional: dateTime

    /**
     * Provides information when the TD instance was last modified.
     *
     * @return the last modified date and time.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var modified: String? // Optional: dateTime

    /**
     * Provides information about the TD maintainer as URI scheme (e.g., mailto, tel, https).
     *
     * @return an optional support URI.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var support: String? // Optional: anyURI

    /**
     * Define the base URI that is used for all relative URI references throughout a TD document.
     *
     * @return an optional base URI.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var base: String? // Optional: anyURI

    /**
     * All Property-based Interaction Affordances of the Thing.
     *
     * @return a map of property affordances.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var properties: MutableMap<String, PropertyAffordance<*>> // Optional: Map of PropertyAffordance

    /**
     * All Action-based Interaction Affordances of the Thing.
     *
     * @return a map of action affordances.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var actions: MutableMap<String, ActionAffordance<*, *>> // Optional: Map of ActionAffordance

    /**
     * All Event-based Interaction Affordances of the Thing.
     *
     * @return a map of event affordances.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var events: MutableMap<String, EventAffordance<*, *, *>> // Optional: Map of EventAffordance

    /**
     * Provides Web links to arbitrary resources that relate to the specified Thing Description.
     *
     * @return an array of links.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var links: MutableList<Link> // Optional: Array of Link

    /**
     * Set of form hypermedia controls that describe how an operation can be performed.
     *
     * @return an array of forms.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var forms: List<Form> // Optional: Array of Form

    /**
     * Set of security definition names, chosen from those defined in securityDefinitions.
     *
     * @return a string or an array of strings representing security definitions, mandatory.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var security: List<String> // Mandatory: string or Array of string

    /**
     * Set of named security configurations (definitions only).
     *
     * @return a map of security schemes, mandatory.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var securityDefinitions: MutableMap<String, SecurityScheme> // Mandatory: Map of SecurityScheme

    /**
     * Indicates the WoT Profile mechanisms followed by this Thing Description and the corresponding Thing implementation.
     *
     * @return an optional profile URI or an array of URIs.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var profile: List<String>? // Optional: anyURI or Array of anyURI

    /**
     * Set of named data schemas to be used in a schema name-value pair.
     *
     * @return a map of data schemas, optional.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var schemaDefinitions: MutableMap<String, DataSchema<Any>>? // Optional: Map of DataSchema

    /**
     * Define URI template variables according to RFC6570 as collection based on DataSchema declarations.
     *
     * @return a map of URI variables.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var uriVariables: MutableMap<String, DataSchema<Any>>? // Optional: Map of DataSchema
}

interface WoTForm {
    val href: String
    val contentType: String
    val contentCoding: String?
    val security: List<String>?
    val scopes: List<String>?
    val response: WoTExpectedResponse?
    val additionalResponses: List<WoTAdditionalExpectedResponse>?
    val subprotocol: String?
    val op: List<Operation>?
    val optionalProperties: MutableMap<String, Any>
}

interface WoTExpectedResponse {
    val contentType: String
    val description: String?
}

interface WoTAdditionalExpectedResponse {
    val success: Boolean
    val contentType: String
    val schema: String?
    val description: String?
}

/**
 * An interface representing an interaction affordance for a thing.
 * This interface defines properties and methods that provide metadata about the interaction affordance.
 */
sealed interface InteractionAffordance : BaseSchema {

    /**
     * Set of form hypermedia controls that describe how an operation can be performed.
     * Forms are serializations of Protocol Bindings. The array cannot be empty.
     *
     * Mandatory.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var forms: MutableList<Form>

    /**
     * Define URI template variables according to [RFC6570] as a collection based
     * on DataSchema declarations. The individual variable DataSchemas cannot be an
     * ObjectSchema or an ArraySchema since each variable needs to be serialized to a
     * string inside the href upon the execution of the operation. If the same variable
     * is both declared in Thing level uriVariables and in Interaction Affordance level,
     * the Interaction Affordance level variable takes precedence.
     *
     * Optional.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var uriVariables: MutableMap<String, DataSchema<Any>>?
}

/**
 * Interface representing the details of an Action in a Web of Things context.
 */
@JsonDeserialize(`as` = ThingAction::class)
interface ActionAffordance<I, O> : InteractionAffordance {

    /**
     * Used to define the input data schema of the Action.
     *
     * @return an optional data schema for input.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var input: DataSchema<I>? // Optional: DataSchema

    /**
     * Used to define the output data schema of the Action.
     *
     * @return an optional data schema for output.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var output: DataSchema<O>? // Optional: DataSchema

    /**
     * Signals if the Action is safe (true) or not.
     *
     * Used to indicate if no internal state (cf. resource state) is changed when invoking an Action.
     * In that case, responses can be cached, for example.
     *
     * @return true if the Action is safe; false otherwise.
     */
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    var safe: Boolean // Default: true

    /**
     * Indicates whether the Action is idempotent (true) or not.
     *
     * Informs whether the Action can be called repeatedly with the same result, based on the same input.
     *
     * @return true if the Action is idempotent; false otherwise.
     */
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    var idempotent: Boolean // Default: true

    /**
     * Indicates whether the action is synchronous (true) or not.
     *
     * A synchronous action means that the response of the action contains all the information about the result
     * of the action, and no further querying about the status of the action is needed.
     * Lack of this keyword means that no claim on the synchronicity of the action can be made.
     *
     * @return true if the Action is synchronous; false otherwise.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var synchronous: Boolean? // Optional: boolean
}

/**
 * Interface representing the details of an Event in a Web of Things context.
 */
@JsonDeserialize(`as` = ThingEvent::class)
interface EventAffordance<T, S, C> : InteractionAffordance {

    /**
     * Defines data that needs to be passed upon subscription, e.g., filters or message format for setting up Webhooks.
     *
     * @return an optional data schema for subscription.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var subscription: DataSchema<S>? // Optional: DataSchema

    /**
     * Defines the data schema of the Event instance messages pushed by the Thing.
     *
     * @return an optional data schema for event messages.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var data: DataSchema<T>? // Optional: DataSchema

    /**
     * Defines any data that needs to be passed to cancel a subscription, e.g., a specific message to remove a Webhook.
     *
     * @return an optional data schema for cancellation.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var cancellation: DataSchema<C>? // Optional: DataSchema
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = StringProperty::class, name = "string") ,
    JsonSubTypes.Type(value = IntProperty::class, name = "integer"),
    JsonSubTypes.Type(value = BooleanProperty::class, name = "boolean"),
    JsonSubTypes.Type(value = NumberProperty::class, name = "number"),
    JsonSubTypes.Type(value = ArrayProperty::class, name = "array"),
    JsonSubTypes.Type(value = ObjectProperty::class, name = "object"),
    JsonSubTypes.Type(value = NullProperty::class, name = "null")
)
@WoTDSL
interface PropertyAffordance<T> : InteractionAffordance, DataSchema<T> {

    /**
     * A hint that indicates whether Servients hosting the Thing and Intermediaries should provide a Protocol Binding that supports the observeproperty and unobserveproperty operations for this Property.
     *
     * Optional.
     */
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    var observable: Boolean
}

data class StringProperty(
    override var forms: MutableList<Form> = mutableListOf(),
    override var uriVariables: MutableMap<String, DataSchema<Any>>? = mutableMapOf(),
    override var observable: Boolean = false
) : PropertyAffordance<String>, StringSchema()

data class IntProperty(
    override var forms: MutableList<Form> = mutableListOf(),
    override var uriVariables: MutableMap<String, DataSchema<Any>>? = mutableMapOf(),
    override var observable: Boolean = false
) : PropertyAffordance<Int>, IntegerSchema()

data class BooleanProperty(
    override var forms: MutableList<Form> = mutableListOf(),
    override var uriVariables: MutableMap<String, DataSchema<Any>>? = mutableMapOf(),
    override var observable: Boolean = false
) : PropertyAffordance<Boolean>, BooleanSchema()

data class NumberProperty(
    override var forms: MutableList<Form> = mutableListOf(),
    override var uriVariables: MutableMap<String, DataSchema<Any>>? = mutableMapOf(),
    override var observable: Boolean = false
) : PropertyAffordance<Number>, NumberSchema()

data class ArrayProperty<T>(
    override var forms: MutableList<Form> = mutableListOf(),
    override var uriVariables: MutableMap<String, DataSchema<Any>>? = mutableMapOf(),
    override var observable: Boolean = false,
) : PropertyAffordance<List<*>>, ArraySchema<T>()

data class NullProperty(
    override var forms: MutableList<Form> = mutableListOf(),
    override var uriVariables: MutableMap<String, DataSchema<Any>>? = mutableMapOf(),
    override var observable: Boolean = false
) : PropertyAffordance<Any>, NullSchema()

data class ObjectProperty(
    override var forms: MutableList<Form> = mutableListOf(),
    override var uriVariables: MutableMap<String, DataSchema<Any>>? = mutableMapOf(),
    override var observable: Boolean = false
) : PropertyAffordance<Map<*, *>>, ObjectSchema()



/**
 * Data class representing version information of a Thing Description (TD) and its underlying Thing Model (TM).
 *
 * @property instance Provides a version indicator of this TD. This field is mandatory.
 * @property model Provides a version indicator of the underlying TM. This field is optional.
 */
data class VersionInfo(
    val instance: String, // Mandatory: string
    val model: String? = null // Optional: string
)