package ai.ancf.lmos.wot.integration

import ai.ancf.lmos.wot.Wot
import ai.ancf.lmos.wot.reflection.ExposedThingBuilder
import ai.ancf.lmos.wot.thing.schema.WoTExposedThing
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/*
fun main(): Unit = runBlocking {

    val agent = ThingAgent()

    // Protocol can be "HTTP" or "MQTT"
    val servient = createServient("HTTP")

    // Register a shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Application is shutting down. Performing cleanup...")
        launch { servient.shutdown() }
    })

    val wot = Wot.create(servient)
    val exposedThing = ExposedThingBuilder.createExposedThing(wot, agent, ThingAgent::class)

    // Start `servient` in a separate coroutine
    servient.start()
    // Add and expose the thing after `start()` has had time to begin
    servient.addThing(exposedThing as WoTExposedThing)
    servient.expose("agent")

    println("Exposed Agent on HTTP Server")
    // Keep the application running until process is stopped
    Job().join()
}*/