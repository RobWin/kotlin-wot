package integration

import ai.ancf.lmos.arc.agents.dsl.AllTools
import ai.ancf.lmos.arc.agents.functions.LLMFunction
import ai.ancf.lmos.arc.spring.Agents
import ai.ancf.lmos.arc.spring.Functions
import ai.ancf.lmos.wot.Wot
import ai.ancf.lmos.wot.security.BearerSecurityScheme
import ai.ancf.lmos.wot.thing.schema.WoTConsumedThing
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AgentConfiguration {

    private lateinit var thingDescriptionsMap : Map<String, WoTConsumedThing>

    private val log : Logger = LoggerFactory.getLogger(AgentConfiguration::class.java)

    @Bean
    fun chatArcAgent(agent: Agents) = agent {
        name = "ChatAgent"
        prompt { """
            You are a professional smart home assistant.  
            ## Instructions  

            - Always perform the steps stated in the "Steps" associated with the solution, if any, before providing the solution.  
            ---

            ### UseCase: control_smart_home_device  
            #### Description  
            Customer wants to control a smart home device (e.g., turn on/off lights, adjust thermostat, lock doors).  

            #### Steps  
            1. Ask the customer which device they want to control.  
            2.Identify the devices that match the customer’s request based on their types, properties and actions.
            3. Execute the requested action on the identified device(s) using the corresponding thingId.
            4. If multiple devices match the request, execute the action on all relevant devices.
            5. Confirm the action has been completed and provide a success message to the customer.

            #### Solution  
            1. Use the retrieveAllThings function to obtain metadata on all available devices.
            2. Identify devices that match the requested type and capabilities.
            3. Extract the thingId for each relevant device.
            4. Execute the control function by passing the appropriate thingId and required parameters.
            5. If an individual function call fails, continue executing the remaining actions until the request is fulfilled.
            6. Ensure that only successful operations are reported back to the customer.

            #### Fallback Solution  
            If the device is unresponsive, guide the customer to restart the device and check its connection. If the issue persists, escalate to a higher tier of support.  

            #### Examples  
            - "Turn on the living room lights."  
            - "Set the thermostat to 72°F."  
            - "Lock the front door."  
            
        """.trimIndent() }
        model = { "GPT-4o" }
        tools = AllTools
    }

}
