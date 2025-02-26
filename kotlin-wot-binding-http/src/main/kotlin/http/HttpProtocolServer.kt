package ai.ancf.lmos.wot.binding.http

import ai.ancf.lmos.wot.JsonMapper
import ai.ancf.lmos.wot.Servient
import ai.ancf.lmos.wot.content.Content
import ai.ancf.lmos.wot.content.ContentCodecException
import ai.ancf.lmos.wot.content.ContentManager
import ai.ancf.lmos.wot.thing.ExposedThing
import ai.ancf.lmos.wot.thing.ThingDescription
import ai.ancf.lmos.wot.thing.form.Form
import ai.ancf.lmos.wot.thing.form.Operation
import ai.ancf.lmos.wot.thing.schema.ContentListener
import ai.ancf.lmos.wot.thing.schema.InteractionAffordance
import ai.ancf.lmos.wot.thing.schema.WoTExposedThing
import ai.anfc.lmos.wot.binding.ProtocolServer
import ai.anfc.lmos.wot.binding.ProtocolServerException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import org.slf4j.LoggerFactory
import kotlin.collections.set

/**
 * Allows exposing Things via HTTP.
 */
open class HttpProtocolServer(
    private val wait: Boolean = false,
    private val bindHost: String = "0.0.0.0",
    private val bindPort: Int = 8080,
    private var baseUrls: List<String> = listOf("http://localhost:8080"),
    private val createServer: (host: String, port: Int, servient: Servient) -> EmbeddedServer<*, *> = ::defaultServer
) : ProtocolServer {
    val things: MutableMap<String, ExposedThing> = mutableMapOf()
    var started = false
    private var server: EmbeddedServer<*, *>? = null

    companion object {
        private val log = LoggerFactory.getLogger(HttpProtocolServer::class.java)
        private const val HTTP_METHOD_NAME = "htv:methodName"
    }

    override suspend fun start(servient: Servient) {
        log.info("Starting on '{}' port '{}'", bindHost, bindPort)
        started = true
        server = createServer(bindHost, bindPort, servient).start(wait)
    }

    // Stop the server
    override suspend fun stop() {
        if (!started) throw ProtocolServerException("Server has not started yet")
        log.info("Stopping HttpProtocolServer")
        server?.stop(1000, 2000)
        started = false
    }

    // Expose a thing
    override suspend fun expose(thing: ExposedThing) {
        if (!started) throw ProtocolServerException("Server has not started yet")

        log.info("Exposing thing '{}' on HttpProtocolServer", thing.id)
        things[thing.id] = thing

        for (baseUrl in baseUrls) {
            for (contentType in ContentManager.offeredMediaTypes) {
                // make reporting of all properties optional?
                val href = "$baseUrl/${thing.id}/all/properties"
                val form = Form(href = href, contentType = contentType, op =  listOf( Operation.READ_ALL_PROPERTIES,
                    Operation.READ_MULTIPLE_PROPERTIES))

                thing.forms += form
                log.debug("Assign '{}' for reading all properties", href)

                exposeProperties(thing, baseUrl, contentType)
                exposeActions(thing, baseUrl, contentType)
                exposeEvents(thing, baseUrl, contentType)
            }
        }
    }

    internal fun exposeProperties(thing: ExposedThing, baseUrl: String, contentType: String) {
        thing.properties.forEach { (name, property) ->

            val href = getHrefWithVariablePattern(baseUrl, thing, "properties", name, property)

            // Determine the operations based on readOnly/writeOnly status
            val operations = when {
                property.readOnly -> listOf(Operation.READ_PROPERTY)
                property.writeOnly -> listOf(Operation.WRITE_PROPERTY)
                else -> listOf(Operation.READ_PROPERTY, Operation.WRITE_PROPERTY)
            }

            // Determine the HTTP method based on property attributes
            val httpMethod = when {
                property.readOnly -> "GET"
                property.writeOnly -> "PUT"
                else -> null // No specific HTTP method for both read/write properties
            }

            // Create the main form and add it to the property
            val form = Form(
                href = href,
                contentType = contentType,
                op = operations,
                optionalProperties = mutableMapOf(HTTP_METHOD_NAME to (httpMethod ?: ""))
            )
            property.forms += form
            log.debug("Assign '{}' to Property '{}'", href, name)

            // If the property is observable, add an additional form with an observable href
            if (property.observable) {
                val observableHref = "$href/observable"
                val observableForm = Form(
                    href = observableHref,
                    contentType = contentType,
                    op = listOf(Operation.OBSERVE_PROPERTY, Operation.UNOBSERVE_PROPERTY),
                    subprotocol = "longpoll"
                )
                property.forms += observableForm
                log.debug("Assign '{}' to observe Property '{}'", observableHref, name)
            }
        }
    }

    internal fun exposeActions(thing: ExposedThing, baseUrl: String, contentType: String) {
        thing.actions.forEach { (name, action) ->
            val href: String = getHrefWithVariablePattern(baseUrl, thing, "actions", name, action)
            // Initialize the form using named parameters
            val form = Form(
                href = href,
                contentType = contentType,
                op = listOf(Operation.INVOKE_ACTION)
            )

            // Add the form to the action
            action.forms += form
            log.debug("Assign '{}' to Action '{}'", href, name)
        }
    }

    internal fun exposeEvents(thing: ExposedThing, baseUrl: String, contentType: String) {
        thing.events.forEach { (name, event) ->
            val href = getHrefWithVariablePattern(baseUrl, thing, "events", name, event)

            // Create the form using named parameters directly
            val form = Form(
                href = href,
                contentType = contentType,
                subprotocol = "longpoll",
                op = listOf(Operation.SUBSCRIBE_EVENT)
            )

            // Add the form to the event
            event.forms += form
            log.debug("Assign '{}' to Event '{}'", href, name)
        }
    }

    private fun getHrefWithVariablePattern(
        baseUrl: String,
        thing: ExposedThing,
        type: String,
        interactionName: String,
        interaction: InteractionAffordance
    ): String {
        var variables = ""
        val uriVariables = interaction.uriVariables?.keys
        if (!uriVariables.isNullOrEmpty()) {
            variables = "{?" + java.lang.String.join(",", uriVariables) + "}"
        }
        return "$baseUrl/${thing.id}/$type/$interactionName$variables"
    }

    // Destroy a thing
    override suspend fun destroy(thing: ExposedThing) {
        log.info("Removing thing '{}'", thing.id)
        things.remove(thing.id)
    }
}

// Default server function
fun defaultServer(host: String, port: Int, servient: Servient): EmbeddedServer<*, *> {
    return embeddedServer(Netty, port = port, host = host) {
        setupRouting(servient)
    }
}

fun Application.setupRouting(servient: Servient) {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
            throw cause // re-throw if you want it to be logged
        }
    }
    install(MicrometerMetrics) {
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics()
        )
    }
    setupJackson()
    routing {
        route("/.well-known/wot") {
            get {
                call.respond(servient.things.values.toList().first(), typeInfo<List<WoTExposedThing>>())
            }
        }
        route("/{id}") {
            get {
                val id = call.parameters["id"]
                val thing: ExposedThing? = servient.things[id]
                if (thing != null) {
                    call.respond(thing, typeInfo<ThingDescription>())
                } else {
                    call.response.status(HttpStatusCode.NotFound)
                }
            }
            route("/properties") {
                get {
                    val id = call.parameters["id"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                    val thing = servient.things[id] ?: return@get call.response.status(HttpStatusCode.NotFound)
                    val properties : Map<String, Content> = thing.handleReadAllProperties()
                    val response: MutableMap<String, Any?> = mutableMapOf()
                    for ((key, value) in properties) {
                        // Assuming content is not null as it's checked earlier
                        when (val jsonNode: JsonNode = ContentManager.contentToValue(value, null)) {
                            is BooleanNode -> {
                                response[key] = jsonNode.asBoolean()
                            }
                            is IntNode -> {
                                response[key] = jsonNode.asInt()
                            }
                            is LongNode -> {
                                response[key] = jsonNode.asLong()
                            }
                            is TextNode -> {
                                response[key] = jsonNode.asText()
                            }
                            is ObjectNode -> {
                                response[key] = JsonMapper.instance.convertValue(jsonNode, Map::class.java)
                            }
                            is ArrayNode -> {
                                response[key] = JsonMapper.instance.convertValue(jsonNode, Array::class.java)
                            }
                            is NullNode -> {
                                response[key] = null
                            }
                        }
                    }
                    call.respond(response)
                }
            }
            route("/properties/{name}") {
                get("/observable") {
                    val id = call.parameters["id"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                    val propertyName = call.parameters["name"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                    val thing = servient.things[id] ?: return@get call.response.status(HttpStatusCode.NotFound)
                    val property = thing.properties[propertyName] ?: return@get call.response.status(HttpStatusCode.NotFound)
                    val contentListener = ContentListener { content: Content ->
                        call.respondBytes { content.body }
                    }
                    thing.handleObserveProperty(propertyName = propertyName, listener = contentListener)
                }
                delete("/observable") {
                    val id = call.parameters["id"] ?: return@delete call.response.status(HttpStatusCode.BadRequest)
                    val propertyName = call.parameters["name"] ?: return@delete call.response.status(HttpStatusCode.BadRequest)
                    val thing = servient.things[id] ?: return@delete call.response.status(HttpStatusCode.NotFound)
                    val property = thing.properties[propertyName] ?: return@delete call.response.status(HttpStatusCode.NotFound)
                    val contentListener = ContentListener { content: Content ->
                        call.respondBytes { content.body }
                    }
                    thing.handleUnobserveProperty(propertyName = propertyName)
                }
                get {
                    val id = call.parameters["id"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                    val propertyName = call.parameters["name"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                    val thing = servient.things[id] ?: return@get call.response.status(HttpStatusCode.NotFound)
                    val property = thing.properties[propertyName] ?: return@get call.response.status(HttpStatusCode.NotFound)
                    if (!property.writeOnly) {
                        try {
                            val content = thing.handleReadProperty(propertyName)
                            call.respondBytes { content.body }
                        }
                        catch (e: ContentCodecException) {
                            call.response.status(HttpStatusCode.InternalServerError)
                        }
                    } else {
                        call.response.status(HttpStatusCode.BadRequest)
                    }
                }
                put {
                    val id = call.parameters["id"] ?: return@put call.response.status(HttpStatusCode.BadRequest)
                    val propertyName = call.parameters["name"] ?: return@put call.response.status(HttpStatusCode.BadRequest)
                    val thing = servient.things[id] ?: return@put call.response.status(HttpStatusCode.NotFound)
                    val property = thing.properties[propertyName] ?: return@put call.response.status(HttpStatusCode.NotFound)
                    val contentType = getOrDefaultRequestContentType(call.request)
                    val content = Content(contentType.toString(), call.receiveChannel().toByteArray())
                    if (!property.readOnly && content.body.isNotEmpty()) {
                        call.respondBytes { thing.handleWriteProperty(propertyName, content).body }
                    } else {
                        call.response.status(HttpStatusCode.BadRequest)
                    }
                }
            }
            post("/actions/{name}") {
                val id = call.parameters["id"] ?: return@post call.response.status(HttpStatusCode.BadRequest)
                val actionName = call.parameters["name"] ?: return@post call.response.status(HttpStatusCode.BadRequest)
                val thing = servient.things[id] ?: return@post call.response.status(HttpStatusCode.NotFound)
                val action = thing.actions[actionName] ?: return@post call.response.status(HttpStatusCode.NotFound)
                val contentType = getOrDefaultRequestContentType(call.request)
                val content = Content(contentType.toString(), call.receiveChannel().toByteArray())
                if(action.input != null && content.body.isEmpty()) {
                    call.response.status(HttpStatusCode.BadRequest)
                }else{
                    val actionResult = thing.handleInvokeAction(actionName, content)
                    if(actionResult.body.isNotEmpty()) {
                        call.respondBytes { actionResult.body }
                    } else{
                        call.response.status(HttpStatusCode.NoContent)
                    }
                }

            }
            get("/events/{name}") {
                val id = call.parameters["id"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                val eventName = call.parameters["name"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                val thing = servient.things[id] ?: return@get call.response.status(HttpStatusCode.NotFound)
                val event = thing.events[eventName] ?: return@get call.response.status(HttpStatusCode.NotFound)
                val contentListener = ContentListener { content: Content ->
                    call.respondBytes { content.body }
                }
                thing.handleSubscribeEvent(eventName = eventName, listener = contentListener)
                call.response.status(HttpStatusCode.OK)
            }
            delete("/events/{name}") {
                val id = call.parameters["id"] ?: return@delete call.response.status(HttpStatusCode.BadRequest)
                val eventName = call.parameters["name"] ?: return@delete call.response.status(HttpStatusCode.BadRequest)
                val thing = servient.things[id] ?: return@delete call.response.status(HttpStatusCode.NotFound)
                val event = thing.events[eventName] ?: return@delete call.response.status(HttpStatusCode.NotFound)
                val contentListener = ContentListener { content: Content ->
                    call.respondBytes { content.body }
                }
                thing.handleUnsubscribeEvent(eventName = eventName)
                call.response.status(HttpStatusCode.OK)
            }
        }
    }
}

private fun Application.setupJackson() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}

private fun getOrDefaultRequestContentType(request: RoutingRequest): ContentType {
    val contentType = request.contentType()
    // Check if the content type is of type `Any` and return the default
    return if (contentType == ContentType.Any) {
        ContentType.Application.Json
    } else {
        contentType
    }
}

/*
private fun parseUrlParameters(
    urlParams: Map<String, Array<String>>,
    uriVariables: MutableMap<String, DataSchema<Any>>?
): Map<String, Any> {
    return urlParams.mapNotNull { (name, urlValue) ->
        val uriVariable = uriVariables?.get(name)
        val type = uriVariable["type"]

        val value: Any = when (type) {
            "integer", "number" -> urlValue.firstOrNull()?.toIntOrNull()
            "string" -> urlValue.firstOrNull()
            else -> {
                null
            }
        }
        name to value
    }.toMap()
}
*/