package integration

import ai.ancf.lmos.arc.agents.AgentProvider
import ai.ancf.lmos.arc.agents.ChatAgent
import ai.ancf.lmos.arc.agents.User
import ai.ancf.lmos.arc.agents.conversation.AssistantMessage
import ai.ancf.lmos.arc.agents.conversation.latest
import ai.ancf.lmos.arc.agents.conversation.toConversation
import ai.ancf.lmos.arc.agents.getAgentByName
import ai.ancf.lmos.arc.core.getOrThrow
import ai.ancf.lmos.wot.protocol.LMOSContext
import ai.ancf.lmos.wot.protocol.LMOSThingType
import ai.ancf.lmos.wot.reflection.annotations.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.springframework.stereotype.Component


const val CAPABILITIES_LINK = "http://wot-agent.default.svc.cluster.local:8181/lmos/chatagent/capabilities"

@Thing(id="chatagent", title="Chat Agent",
    description="A chat agent.", type= LMOSThingType.AGENT)
@Context(prefix = LMOSContext.prefix, url = LMOSContext.url)
@Link(href = CAPABILITIES_LINK, rel = "service-meta", type = "application/json")
@VersionInfo(instance = "1.0.0")
@Component
class ChatAgent(agentProvider: AgentProvider, @Property(readOnly = true)

    val modelConfiguration: ModelConfiguration = ModelConfiguration(0.5, 50)) {

    private val messageFlow = MutableSharedFlow<String>(replay = 1) // Replay last emitted value

    val agent = agentProvider.getAgentByName("ChatAgent") as ChatAgent

    @Property(title = "Observable Property", readOnly = true)
    val observableProperty : MutableStateFlow<String> = MutableStateFlow("Hello World")

    @Action(title = "Ask", description = "Ask the agent a question.")
    suspend fun ask(chat : Chat) : String {
        val assistantMessage = agent.execute(chat.message.toConversation(User("myId"))).getOrThrow().latest<AssistantMessage>() ?:
            throw RuntimeException("No Assistant response")
        messageFlow.emit(assistantMessage.content)
        return assistantMessage.content
    }

    @Event(title = "Generated message")
    fun messageGenerated() : Flow<String> {
        return messageFlow
    }
}

data class ModelConfiguration(val modelTemperature: Double, val maxTokens: Int)

data class Chat(val message: String)

