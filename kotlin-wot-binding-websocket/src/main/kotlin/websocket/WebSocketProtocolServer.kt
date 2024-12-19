package ai.ancf.lmos.wot.binding.websocket


import ai.ancf.lmos.wot.JsonMapper
import ai.ancf.lmos.wot.Servient
import ai.ancf.lmos.wot.content.ContentManager
import ai.ancf.lmos.wot.thing.ExposedThing
import ai.ancf.lmos.wot.thing.ThingDescription
import ai.ancf.lmos.wot.thing.form.Form
import ai.ancf.lmos.wot.thing.form.Operation
import ai.ancf.lmos.wot.thing.schema.ContentListener
import ai.ancf.lmos.wot.thing.schema.WoTExposedThing
import ai.anfc.lmos.wot.binding.ProtocolServer
import ai.anfc.lmos.wot.binding.ProtocolServerException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory
import java.util.*

class WebSocketProtocolServer(
    private val wait: Boolean = false,
    private val bindHost: String = "0.0.0.0",
    private val bindPort: Int = 8080,
    private val createServer: (host: String, port: Int, servient: Servient) -> EmbeddedServer<*, *> = ::defaultWebSocketServer
) : ProtocolServer {
    private val things: MutableMap<String, ExposedThing> = mutableMapOf()

    var started = false
    private var server: EmbeddedServer<*, *>? = null
    private var actualAddresses: List<String> = listOf("ws://$bindHost:$bindPort")

    companion object {
        private val log = LoggerFactory.getLogger(WebSocketProtocolServer::class.java)
    }

    override suspend fun start(servient: Servient) {
        log.info("Starting WebSocket server on '{}' port '{}'", bindHost, bindPort)
        started = true
        server = createServer(bindHost, bindPort, servient).start(wait)
    }

    override suspend fun stop() {
        if (!started) throw ProtocolServerException("Server has not started yet")
        log.info("Stopping WebSocket server on '{}' port '{}'", bindHost, bindPort)
        server?.stop(1000, 2000)
        started = false
    }

    override suspend fun expose(thing: ExposedThing) {
        if (!started) throw ProtocolServerException("Server has not started yet")

        log.info("Exposing thing '{}'", thing.id)
        things[thing.id] = thing
        for (address in actualAddresses) {
            exposeProperties(thing, address)
            exposeActions(thing, address)
            exposeEvents(thing, address)
        }
    }

    override suspend fun destroy(thing: ExposedThing) {
        log.info("Removing thing '{}'", thing.id)
        things.remove(thing.id)
    }

    internal fun exposeProperties(thing: ExposedThing, address: String) {
        thing.properties.forEach { (name, property) ->

            val href = "$address/ws"

            // Combine all operations (read, write, observe, unobserve) into a single form
            val operations = mutableListOf<Operation>()
            if (!property.writeOnly) {
                operations += Operation.READ_PROPERTY
            }
            if (!property.readOnly) {
                operations += Operation.WRITE_PROPERTY
            }
            if (property.observable) {
                operations += Operation.OBSERVE_PROPERTY
                operations += Operation.UNOBSERVE_PROPERTY
            }

            // Create a single form that includes all operations and the subprotocol
            val form = Form(
                href = href,
                contentType = "application/json",
                op = operations,
                subprotocol = "webthingprotocol"
            )

            property.forms += form
            log.debug("Assign '{}' with operations '{}' to Property '{}'", href, operations, name)
        }
    }

    internal fun exposeActions(thing: ExposedThing, address: String) {
        thing.actions.forEach { (name, action) ->
            // Construct the href for the action
            val href = "$address/ws" // WebSocket path for actions

            // Create a form for invoking the action
            val form = Form(
                href = href,
                contentType = "application/json",
                op = listOf(Operation.INVOKE_ACTION), // Operation type specific to actions
                subprotocol = "webthingprotocol" // Specific subprotocol for actions
            )

            // Add the form to the action's forms
            action.forms += form
            log.debug("Assign '{}' with subprotocol '{}' to Action '{}'", href, "webthingprotocol", name)
        }
    }

    internal fun exposeEvents(thing: ExposedThing, address: String) {
        thing.events.forEach { (name, event) ->
            // Construct the href for the event
            val href = "$address/ws" // WebSocket path for events

            // Create a form for subscribing to the event
            val form = Form(
                href = href,
                contentType = "application/json",
                subprotocol = "webthingprotocol",
                op = listOf(Operation.SUBSCRIBE_EVENT, Operation.UNSUBSCRIBE_EVENT) // Operation type specific to events
            )

            // Add the form to the event's forms
            event.forms += form
            log.debug("Assign '{}' with subprotocol '{}' to Event '{}'", href, "webthingprotocol", name)
        }
    }
}

// Default server setup
fun defaultWebSocketServer(host: String, port: Int, servient: Servient): EmbeddedServer<*, *> {
    return embeddedServer(Netty, port = port, host = host) {
        setupRoutingWithWebSockets(servient)
    }
}

fun Application.setupRoutingWithWebSockets(servient: Servient) {
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    install(WebSockets) {
        contentConverter = JacksonWebsocketContentConverter(JsonMapper.instance)
    }
    routing {
        route("/") {
            get {
                call.respond(servient.things.values.toList(), typeInfo<List<WoTExposedThing>>())
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
        }

        webSocket("/ws") {
            val sessionId = UUID.randomUUID().toString()

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    try {
                        // Deserialize the message to WoTMessage
                        val message: WoTMessage = JsonMapper.instance.readValue(frame.readText())
                        // Retrieve the thingId from the message
                        val thingId = message.thingId
                        val thing = servient.things[thingId]

                        if (thing == null) {
                            sendError(thingId, ErrorType.THING_NOT_FOUND)
                            return@webSocket
                        }

                        // Handle the message based on its type
                        when (message) {
                            is ReadAllPropertiesMessage -> handleReadAllProperties(thing, message, thingId)
                            is ReadPropertyMessage -> handleReadProperty(thing, message, thingId)
                            is WritePropertyMessage -> handleWriteProperty(thing, message, thingId)
                            is ObservePropertyMessage -> handleObserveProperty(thing, message, thingId, sessionId)
                            is UnobservePropertyMessage -> handleUnobserveProperty(thing, message, thingId, sessionId)
                            is InvokeActionMessage -> handleInvokeAction(thing, message, thingId)
                            is SubscribeEventMessage -> handleSubscribeEvent(thing, message, thingId, sessionId)
                            is UnsubscribeEventMessage -> handleUnsubscribeEvent(thing, message, thingId, sessionId)
                            else -> sendError(thingId, ErrorType.UNSUPPORTED_MESSAGE_TYPE)
                        }
                    } catch (e: Exception) {
                        val errorMessage = "Failed to read message"
                        log.warn(errorMessage, e)
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, errorMessage))
                    }
                }
            }
        }
    }
}

// Error Type enum to make error handling more consistent
enum class ErrorType {
    THING_NOT_FOUND,
    PROPERTY_NOT_FOUND,
    PROPERTY_IS_READ_ONLY,
    PROPERTY_IS_WRITE_ONLY,
    ACTION_NOT_FOUND,
    EVENT_NOT_FOUND,
    INTERNAL_SERVER_ERROR,
    UNSUPPORTED_MESSAGE_TYPE
}

suspend fun DefaultWebSocketServerSession.sendError(thingId: String, errorType: ErrorType, message: String? = null) {
    val errorJson = when (errorType) {
        ErrorType.THING_NOT_FOUND -> createThingNotFound(thingId)
        ErrorType.PROPERTY_NOT_FOUND -> createPropertyNotFound(thingId, message)
        ErrorType.PROPERTY_IS_READ_ONLY -> createPropertyIsReadOnly(thingId, message)
        ErrorType.PROPERTY_IS_WRITE_ONLY -> createPropertyIsWriteOnly(thingId, message)
        ErrorType.ACTION_NOT_FOUND -> createActionNotFound(thingId, message as String)
        ErrorType.EVENT_NOT_FOUND -> createEventNotFound(thingId, message as String)
        ErrorType.INTERNAL_SERVER_ERROR -> createInternalServerError(thingId, message)
        ErrorType.UNSUPPORTED_MESSAGE_TYPE -> createUnsupportedMessageType(thingId)
    }
    sendSerialized(errorJson)
}

private suspend fun readProperty(
    thing: ExposedThing,
    propertyName: String,
    thingId: String,
): PropertyReadingMessage {
    val content = thing.handleReadProperty(propertyName)
    return PropertyReadingMessage(
        thingId = thingId,
        property = propertyName,
        data = JsonMapper.instance.readTree(content.body)
    )
}

suspend fun DefaultWebSocketServerSession.handleReadAllProperties(thing: ExposedThing, message: ReadAllPropertiesMessage, thingId: String) {

    try {
        val propertyMap = thing.handleReadAllProperties().mapValues { entry ->
            JsonMapper.instance.readTree(entry.value.body)
        }
        val propertyReadingsMessage = PropertyReadingsMessage(
            thingId = thingId,
            data = propertyMap
        )
        sendSerialized(propertyReadingsMessage)
    } catch (e: Exception) {
        sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
    }
}

suspend fun DefaultWebSocketServerSession.handleReadProperty(thing: ExposedThing, message: ReadPropertyMessage, thingId: String) {
    val propertyName = message.property
    val property = thing.properties[propertyName]

    if (property == null) {
        sendError(thingId, ErrorType.PROPERTY_NOT_FOUND, propertyName)
    } else if (property.writeOnly) {
        sendError(thingId, ErrorType.PROPERTY_IS_WRITE_ONLY, propertyName)
    } else {
        try {
            val response = readProperty(thing, propertyName, thingId)
            sendSerialized(response)
        } catch (e: Exception) {
            sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

suspend fun DefaultWebSocketServerSession.handleWriteProperty(thing: ExposedThing, message: WritePropertyMessage, thingId: String) {
    val propertyName = message.property
    val data = message.data
    val property = thing.properties[propertyName]

    if (property == null) {
        sendError(thingId, ErrorType.PROPERTY_NOT_FOUND, propertyName)
    } else if (property.readOnly) {
        sendError(thingId, ErrorType.PROPERTY_IS_READ_ONLY, propertyName)
    } else {
        try {
            thing.handleWriteProperty(propertyName, ContentManager.valueToContent(data))
            val response = PropertyReadingMessage(thingId = thingId, property = propertyName, data = data)
            sendSerialized(response)
        } catch (e: Exception) {
            sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

suspend fun DefaultWebSocketServerSession.handleObserveProperty(thing: ExposedThing, message: ObservePropertyMessage, thingId: String, sessionId: String) {
    val propertyName = message.property
    val property = thing.properties[propertyName]

    if (property == null) {
        sendError(thingId, ErrorType.PROPERTY_NOT_FOUND, propertyName)
    } else {
        try {
            val contentListener = ContentListener { content ->
                try {
                    val response = PropertyReadingMessage(
                        thingId = thingId,
                        property = propertyName,
                        data = JsonMapper.instance.readTree(content.body)
                    )
                    sendSerialized(response)
                } catch (e: Exception) {
                    sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
                }
            }
            thing.handleObserveProperty(sessionId = sessionId, propertyName = propertyName, listener = contentListener)
            val response = readProperty(thing, propertyName, thingId)
            sendSerialized(response)
        } catch (e: Exception) {
            sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

suspend fun DefaultWebSocketServerSession.handleUnobserveProperty(thing: ExposedThing, message: UnobservePropertyMessage, thingId: String, sessionId: String) {
    val propertyName = message.property
    val property = thing.properties[propertyName]

    if (property == null) {
        sendError(thingId, ErrorType.PROPERTY_NOT_FOUND, propertyName)
    } else {
        try {
            thing.handleUnobserveProperty(sessionId = sessionId, propertyName = propertyName)
            val acknowledgement = Acknowledgement(thingId = thingId, message = MessageTypes.UNOBSERVE_PROPERTY)
            sendSerialized(acknowledgement)
        } catch (e: Exception) {
            sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

suspend fun DefaultWebSocketServerSession.handleInvokeAction(thing: ExposedThing, message: InvokeActionMessage, thingId: String) {
    val actionName = message.action
    val action = thing.actions[actionName]

    if (action == null) {
        sendError(thingId, ErrorType.ACTION_NOT_FOUND, actionName)
    } else {
        try {
            val inputContent = ContentManager.valueToContent(message.input)
            val actionResult = thing.handleInvokeAction(actionName, inputContent)

            val response = ActionStatusMessage(
                thingId = thingId,
                action = actionName,
                output = JsonMapper.instance.readTree(actionResult.body)
            )
            sendSerialized(response)
        } catch (e: Exception) {
            sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

suspend fun DefaultWebSocketServerSession.handleSubscribeEvent(thing: ExposedThing, message: SubscribeEventMessage, thingId: String, sessionId: String) {
    val eventName = message.event
    val event = thing.events[eventName]

    if (event == null) {
        sendError(thingId, ErrorType.EVENT_NOT_FOUND, eventName)
    } else {
        try {
            val contentListener = ContentListener { content ->
                try {
                    val eventMessage = EventMessage(
                        thingId = thingId,
                        event = eventName,
                        data = JsonMapper.instance.readTree(content.body)
                    )
                    sendSerialized(eventMessage)
                } catch (e: Exception) {
                    sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
                }
            }
            thing.handleSubscribeEvent(sessionId = sessionId, eventName = eventName, listener = contentListener)
            val acknowledgement = Acknowledgement(thingId = thingId, message = MessageTypes.SUBSCRIBE_EVENT)
            sendSerialized(acknowledgement)
        } catch (e: Exception) {
            sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

suspend fun DefaultWebSocketServerSession.handleUnsubscribeEvent(thing: ExposedThing, message: UnsubscribeEventMessage, thingId: String, sessionId: String) {
    val eventName = message.event
    val event = thing.events[eventName]

    if (event == null) {
        sendError(thingId, ErrorType.EVENT_NOT_FOUND, eventName)
    } else {
        try {
            thing.handleUnsubscribeEvent(sessionId = sessionId, eventName = eventName)
            val acknowledgement = Acknowledgement(thingId = thingId, message = MessageTypes.UNSUBSCRIBE_EVENT)
            sendSerialized(acknowledgement)
        } catch (e: Exception) {
            sendError(thingId, ErrorType.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

private fun createUnsupportedMessageType(thingId: String): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#unsupported-message-type",
        title = "Unsupported Message Type",
        status = "400",
        detail = "Unsupported message type",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}

private fun createPropertyIsReadOnly(thingId: String, propertyName: String?): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#operation-not-allowed",
        title = "Property Read-Only",
        status = "405",
        detail = "Property '$propertyName' is read-only.",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}

private fun createPropertyIsWriteOnly(thingId: String, propertyName: String?): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#operation-not-allowed",
        title = "Property is write-only",
        status = "405",
        detail = "Property '$propertyName' is write-only.",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}

private fun createThingNotFound(thingId: String): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#not-found",
        title = "Thing Not Found",
        status = "404",
        detail = "Thing with ID '$thingId' not found.",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}

private fun createInternalServerError(thingId: String, exceptionMessage: String?): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#internal-server-error",
        title = "Internal Server Error",
        status = "500",
        detail = "Error reading property: $exceptionMessage",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}

private fun createPropertyNotFound(thingId: String, propertyName: String?): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#not-found",
        title = "Property Not Found",
        status = "404",
        detail = "Property '$propertyName' not found.",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}

private fun createEventNotFound(thingId: String, eventName: String?): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#not-found",
        title = "Event Not Found",
        status = "404",
        detail = "Event '$eventName' not found.",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}

private fun createActionNotFound(thingId: String, actionName: String?): ErrorMessage {
    return ErrorMessage(
        thingId = thingId,
        type = "https://w3c.github.io/web-thing-protocol/errors#not-found",
        title = "Action Not Found",
        status = "404",
        detail = "Action '$actionName' not found.",
        instance = "https://mythingserver.com/errors/${UUID.randomUUID()}"
    )
}