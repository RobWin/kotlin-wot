/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package websocket

import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.binding.websocket.WebSocketProtocolClientFactory
import org.eclipse.thingweb.binding.websocket.WebSocketProtocolServer
import org.eclipse.thingweb.thing.ExposedThing
import org.eclipse.thingweb.thing.exposedThing
import org.eclipse.thingweb.thing.schema.*
import com.fasterxml.jackson.databind.node.NullNode
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

private const val PROPERTY_NAME = "property1"

private const val PROPERTY_NAME_2 = "property2"

private const val ACTION_NAME = "action1"

private const val ACTION_NAME_2 = "action2"

private const val ACTION_NAME_3 = "action3"

private const val ACTION_NAME_4 = "action4"

private const val EVENT_NAME = "event1"

class WebSocketProtocolClientTest {

    private lateinit var thing: WoTConsumedThing

    private lateinit var servient : Servient

    private lateinit var exposedThing : ExposedThing

    private var property1 : Int = 0
    private var property2 : String = ""

    @BeforeTest
    fun setUp() = runTest {

        servient = Servient(
            servers = listOf(WebSocketProtocolServer()),
            clientFactories = listOf(HttpProtocolClientFactory(), WebSocketProtocolClientFactory())
        )

        exposedThing = exposedThing(servient, id="test") {
            intProperty(PROPERTY_NAME) {
                observable = true
            }
            stringProperty(PROPERTY_NAME_2) {
                observable = true
            }
            action<String, String>(ACTION_NAME)
            {
                title = ACTION_NAME
                input = stringSchema {
                    title = "Action Input"
                    minLength = 10
                    default = "test"
                }
                output = StringSchema()
            }
            action<String, String>(ACTION_NAME_2)
            {
                title = ACTION_NAME_2
                output = StringSchema()
            }
            action<String, String>(ACTION_NAME_3)
            {
                title = ACTION_NAME_3
                input = StringSchema()
            }
            action<String, String>(ACTION_NAME_4)
            {
                title = ACTION_NAME_4
            }
            event<String, Nothing, Nothing>(EVENT_NAME){
                data = StringSchema()
            }
        }.setPropertyReadHandler(PROPERTY_NAME) {
            property1.toInteractionInputValue()
        }.setPropertyReadHandler(PROPERTY_NAME_2) {
            "test".toInteractionInputValue()
        }.setActionHandler(ACTION_NAME) { input, _->
            val inputString = input.value()
            "${inputString.asText()} 10".toInteractionInputValue()
        }.setPropertyWriteHandler(PROPERTY_NAME) { input, _->
            val inputInt = input.value()
            property1 = inputInt.asInt()
            property1.toInteractionInputValue()
        }.setPropertyWriteHandler(PROPERTY_NAME_2) { input, _->
            val inputInt = input.value()
            property2 = inputInt.asText()
            property2.toInteractionInputValue()
        }.setActionHandler(ACTION_NAME_2) { input, _->
            "test test".toInteractionInputValue()
        }.setActionHandler(ACTION_NAME_3) { input, _->
            val inputString = input.value()
            property2 = inputString.asText()
            InteractionInput.Value(NullNode.instance)
        }.setActionHandler(ACTION_NAME_4) { _, _->
            InteractionInput.Value(NullNode.instance)
        }.setEventSubscribeHandler(EVENT_NAME) { _ ->
        }

        property1 = 10

        servient.addThing(exposedThing)
        servient.start()
        servient.expose("test")

        val wot = Wot.create(servient)

        val thingDescription = wot.requestThingDescription("http://localhost:8080/test")
        thing = wot.consume(thingDescription)

    }

    @AfterTest
    fun tearDown() = runTest {
        clearAllMocks()
        servient.shutdown()
    }

    @Test
    fun `should get property`() = runBlocking {

        val readProperty1 = thing.readProperty(PROPERTY_NAME).value()
        assertEquals(10, (readProperty1).asInt())

        val readProperty2 = thing.readProperty(PROPERTY_NAME_2).value()
        assertEquals("test", (readProperty2).asText())

    }

    @Test
    fun `should get all properties`() = runBlocking {
        val readPropertyMap = thing.readAllProperties()
        assertEquals(10, readPropertyMap[PROPERTY_NAME]?.value()?.asInt())
        assertEquals("test", readPropertyMap[PROPERTY_NAME_2]?.value()?.asText())
    }

    @Test
    fun `should write property`() = runBlocking {
        thing.writeProperty(PROPERTY_NAME, 20.toInteractionInputValue())

        assertEquals(20, property1)
    }

    @Test
    fun `should write multiple properties`() = runBlocking {

        thing.writeMultipleProperties(mapOf(PROPERTY_NAME to 30.toDataSchemeValue(), PROPERTY_NAME_2 to "new".toDataSchemeValue()))

        assertEquals(30, property1)
        assertEquals("new", property2)
    }


    @Test
    fun `should invoke action`() = runBlocking {
        val response = thing.invokeAction(ACTION_NAME, "test".toDataSchemeValue())

        assertEquals("test 10", response.asText())
    }

    @Test
    fun `should invoke action without input`() =  runBlocking {
        val response = thing.invokeAction(ACTION_NAME_2)

        assertEquals("test test", response.asText())
    }

    @Test
    fun `should invoke action without output`(): Unit = runBlocking {
        val response = thing.invokeAction(ACTION_NAME_3, "test".toDataSchemeValue())
        assertEquals("test", property2)
        assertIs<NullNode>(response)
    }


    @Test
    fun `should subscribe to event`() = runBlocking {

        val lock = CountDownLatch(2);

        val subscription = thing.subscribeEvent(EVENT_NAME, listener = {
            println("TESTETST")
            lock.countDown() }
        )

        exposedThing.emitEvent(EVENT_NAME, "test".toInteractionInputValue())
        exposedThing.emitEvent(EVENT_NAME, "test".toInteractionInputValue())

        // Wait for the events to be handled, with a timeout.
        val completedInTime = lock.await(2000, TimeUnit.MILLISECONDS)

        // Assert that the events were handled within the timeout period.
        assertTrue(completedInTime, "Expected events were not received within the timeout period.")
    }

    @Test
    fun `should observe property`() = runBlocking {

        val lock = CountDownLatch(2);

        thing.observeProperty(PROPERTY_NAME, listener = { lock.countDown() })

        exposedThing.emitPropertyChange(PROPERTY_NAME, 1.toInteractionInputValue())
        exposedThing.emitPropertyChange(PROPERTY_NAME, 2.toInteractionInputValue())

        // Wait for the property change events to be handled, with a timeout.
        val completedInTime = lock.await(2000, TimeUnit.MILLISECONDS)

        // Assert that the events were handled within the timeout period.
        assertTrue(completedInTime, "Expected events were not received within the timeout period.")
    }
}