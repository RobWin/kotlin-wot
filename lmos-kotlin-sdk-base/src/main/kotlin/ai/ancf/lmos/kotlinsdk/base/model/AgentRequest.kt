package ai.ancf.lmos.kotlinsdk.base.model

data class AgentRequest(
    val messages: List<Message>,
    val conversationContext: ConversationContext,
    val systemContext: List<SystemContextEntry>,
    val userContext: UserContext,
)

data class UserContext(
    val userId: String? = null,
    val userToken: String? = null,
    val profile: List<ProfileEntry>,
)

data class ConversationContext(
    val conversationId: String,
    val turnId: String? = null,
    val anonymizationEntities: List<AnonymizationEntity>? = null,
)

data class ProfileEntry(
    val key: String,
    val value: String,
)

data class SystemContextEntry(
    val key: String,
    val value: String,
)