package ai.ancf.lmos.wot.reflection

import ai.ancf.lmos.wot.Servient
import ai.ancf.lmos.wot.Wot
import ai.ancf.lmos.wot.binding.http.HttpProtocolServer
import ai.ancf.lmos.wot.content.ContentManager
import ai.ancf.lmos.wot.content.toJsonContent
import ai.ancf.lmos.wot.reflection.things.SimpleThing
import ai.ancf.lmos.wot.thing.ExposedThing
import ai.ancf.lmos.wot.thing.schema.ContentListener
import ai.ancf.lmos.wot.thing.schema.Link
import ai.ancf.lmos.wot.thing.schema.StringSchema
import ai.ancf.lmos.wot.thing.schema.WoTThingDescription
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

class SimpleThingTest {

    lateinit var servient: Servient
    lateinit var wot: Wot
    lateinit var simpleThing: SimpleThing
    lateinit var exposedThing: ExposedThing
    lateinit var thingDescription: WoTThingDescription

    @BeforeTest
    fun setUp() = runTest {
        // Set up the servient and WoT instance
        val httpProtocolServer = HttpProtocolServer()
        httpProtocolServer.started = true
        servient = Servient(servers = listOf(httpProtocolServer))
        wot = Wot.create(servient)

        // Create an instance of ComplexThing
        simpleThing = SimpleThing()

        // Generate ThingDescription from the class
        exposedThing = ExposedThingBuilder.createExposedThing(wot, simpleThing, SimpleThing::class) as ExposedThing
        thingDescription = exposedThing.getThingDescription()

        servient.addThing(exposedThing)
        servient.expose("simpleThing")
    }

    @Test
    fun `test ThingDescription creation for SimpleThing`() {
        // Validate Thing metadata
        assertEquals("simpleThing", thingDescription.id, "ThingDescription ID should match the class ID")
        assertEquals("Simple Thing", thingDescription.title, "ThingDescription title should match")
        assertEquals("A thing with complex properties, actions, and events.", thingDescription.description, "ThingDescription description should match")
        assertEquals("1.0.0", thingDescription.version?.instance)
        assertEquals(listOf(Link("my/link", "my/type", "my-rel", "my-anchor", "my-sizes", listOf("my-lang-1", "my-lang-2"))), thingDescription.links)
    }

    @Test
    fun `Read mutable property`() = runTest {
        val content = exposedThing.handleReadProperty("mutableProperty")
        val response = ContentManager.contentToValue(content, StringSchema())
        assertEquals("test", response.asText())
    }

    @Test
    fun `Read observable property`() = runTest {
        val content = exposedThing.handleReadProperty("observableProperty")
        val response = ContentManager.contentToValue(content, StringSchema())
        assertEquals("Hello World", response.asText())
        simpleThing.changeObservableProperty()
        val updatedContent = exposedThing.handleReadProperty("observableProperty")
        val updatedResponse = ContentManager.contentToValue(updatedContent, StringSchema())
        assertEquals("Hello from action!", updatedResponse.asText())
    }

    @Test
    fun `Get observable property update`() = runTest {
        var lock = CountDownLatch(1);
        val contentListener: ContentListener = (ContentListener { lock.countDown() })

        exposedThing.handleObserveProperty(propertyName = "observableProperty", listener = contentListener)

        // Wait for the events to be handled, with a timeout.
        val completedInTime = lock.await(2000, TimeUnit.MILLISECONDS)

        // Assert that the events were handled within the timeout period.
        assertTrue(completedInTime, "Expected events were not received within the timeout period.")

        lock = CountDownLatch(1);

        simpleThing.changeObservableProperty()

        // Wait for the events to be handled, with a timeout.
        val completedInTime2 = lock.await(2000, TimeUnit.MILLISECONDS)

        // Assert that the events were handled within the timeout period.
        assertTrue(completedInTime2, "Expected events were not received within the timeout period.")
    }

    @Test
    fun `Write writeOnly property`() = runTest {
        val content = exposedThing.handleWriteProperty("writeOnlyProperty", "newValue".toJsonContent())
        val response = ContentManager.contentToValue(content, StringSchema())
        assertEquals("newValue", response.asText())
        assertEquals("newValue", simpleThing.writeOnlyProperty)
    }

    @Test
    fun `Write mutable property`() = runTest {
        val content = exposedThing.handleWriteProperty("mutableProperty", "newValue".toJsonContent())
        val response = ContentManager.contentToValue(content, StringSchema())
        assertEquals("newValue", response.asText())
        assertEquals("newValue", simpleThing.mutableProperty)
    }

    @Test
    fun `Write readonly property`() = runTest {
        assertFailsWith<IllegalArgumentException>("ExposedThing 'Simple Thing' has no writerHandler for Property 'readyOnlyProperty'") {
            exposedThing.handleWriteProperty("readyOnlyProperty", "newValue".toJsonContent())
        }
    }

    @Test
    fun `Read writeOnly property`() = runTest {
        assertFailsWith<IllegalArgumentException>("ExposedThing 'Simple Thing' has no readHandler for Property 'writeOnlyProperty'") {
            exposedThing.handleReadProperty("writeOnlyProperty")
        }
    }

    @Test
    fun `Invoke voidAction`() = runTest {
        exposedThing.handleInvokeAction("voidAction")
        assertEquals(1, simpleThing.counter)
    }

    @Test
    fun `Invoke inputAction`() = runTest {
        exposedThing.handleInvokeAction("inputAction", "test".toJsonContent())
        assertEquals(1, simpleThing.counter)
    }

    @Test
    fun `Invoke outputAction`() = runTest {
        val content = exposedThing.handleInvokeAction("outputAction")
        val response = ContentManager.contentToValue(content, StringSchema())
        assertEquals("test", response.asText())
    }

    @Test
    fun `Invoke inOutAction`() = runTest {
        val content = exposedThing.handleInvokeAction("inOutAction", "test".toJsonContent())
        val response = ContentManager.contentToValue(content, StringSchema())
        assertEquals("test output", response.asText())
    }

    @Test
    fun `Subscribe to statusUpdated event `() = runBlocking {
        var lock = CountDownLatch(1);
        val contentListener: ContentListener = (ContentListener { lock.countDown() })

        exposedThing.handleSubscribeEvent(eventName = "statusUpdated", listener = contentListener)


        // Wait for the events to be handled, with a timeout.
        val completedInTime = lock.await(2000, TimeUnit.MILLISECONDS)

        // Assert that the events were handled within the timeout period.
        assertTrue(completedInTime, "Expected events were not received within the timeout period.")
    }
}