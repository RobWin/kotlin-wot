package ai.ancf.lmos.wot.integration

import ai.ancf.lmos.arc.agents.AgentProvider
import ai.ancf.lmos.wot.Servient
import ai.ancf.lmos.wot.Wot
import ai.ancf.lmos.wot.binding.http.HttpProtocolClientFactory
import ai.ancf.lmos.wot.binding.http.HttpProtocolServer
import ai.ancf.lmos.wot.reflection.ExposedThingBuilder.createExposedThing
import ai.ancf.lmos.wot.thing.schema.toInteractionInputValue
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AgentBuilderTest {

    @Test
    fun `Should create thing description from class`() = runTest {

        val servient =  Servient(
            servers = listOf(HttpProtocolServer()),
            clientFactories = listOf(
                HttpProtocolClientFactory()
            )
        )
        servient.start()

        val wot = Wot.create(servient)
        val exposedThing = createExposedThing(wot, ThingAgent(mockk<AgentProvider>()), ThingAgent::class)
        if(exposedThing != null){

            servient.addThing(exposedThing)
            servient.expose("agent")

            val httpAgentTD = wot
                .requestThingDescription("http://localhost:8080/agent")
            val httpAgent = wot.consume(httpAgentTD)
            val output = httpAgent.invokeAction("ask",
                "What is Paris?".toInteractionInputValue())
            println(output.value())
        }

    }
}