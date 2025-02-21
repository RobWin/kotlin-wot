package ai.ancf.lmos.wot.integration

import ai.ancf.lmos.arc.agents.AgentProvider
import ai.ancf.lmos.arc.agents.ChatAgent
import ai.ancf.lmos.arc.agents.User
import ai.ancf.lmos.arc.agents.conversation.AssistantMessage
import ai.ancf.lmos.arc.agents.conversation.latest
import ai.ancf.lmos.arc.agents.conversation.toConversation
import ai.ancf.lmos.arc.agents.getAgentByName
import ai.ancf.lmos.arc.core.getOrThrow
import ai.ancf.lmos.wot.JsonMapper
import ai.ancf.lmos.wot.protocol.ConversationalAgent
import ai.ancf.lmos.wot.protocol.LMOSContext
import ai.ancf.lmos.wot.protocol.LMOSThingType
import ai.ancf.lmos.wot.reflection.annotations.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component


@Thing(id="chatagent", title="Chat Agent",
    description="A chat agent.", type= LMOSThingType.AGENT)
@Context(prefix = LMOSContext.prefix, url = LMOSContext.url)
@Link(href = "lmos/capabilities", rel = "service-meta", type = "application/json")
@VersionInfo(instance = "1.0.0")
@Component
class ChatAgent(agentProvider: AgentProvider, @Property(readOnly = true)
                 val modelConfiguration: ModelConfiguration = ModelConfiguration(0.5, 50))
    : ConversationalAgent<String, String>, ApplicationListener<AgentEvent> {

    private val agentEventFlow = MutableSharedFlow<String>(replay = 1) // Replay last emitted value

    val agent = agentProvider.getAgentByName("ChatAgent") as ChatAgent

    @Action(title = "Chat", description = "Ask the agent a question.")
    @ActionInput(title = "The question", description = "A question")
    @ActionOutput(title = "The question", description = "A question")
    override suspend fun chat(message: String) : String {
        val assistantMessage = agent.execute(message.toConversation(User("myId"))).getOrThrow().latest<AssistantMessage>() ?:
            throw RuntimeException("No Assistant response")
        return assistantMessage.content
    }

    @Event(title = "Agent Event", description = "An event from the agent.")
    fun agentEvent() : Flow<String> {
        return agentEventFlow
    }

    override fun onApplicationEvent(event: AgentEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            agentEventFlow.emit(JsonMapper.instance.writeValueAsString(event))
        }
    }
}

data class ModelConfiguration(val modelTemperature: Double, val maxTokens: Int)


