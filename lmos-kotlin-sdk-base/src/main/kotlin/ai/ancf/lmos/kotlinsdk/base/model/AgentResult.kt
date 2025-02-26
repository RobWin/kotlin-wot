package ai.ancf.lmos.kotlinsdk.base.model

data class AgentResult(
    val status: String? = null,
    val responseTime: Double = -1.0,
    val messages: List<Message>,
    val anonymizationEntities: List<AnonymizationEntity>? = null,
)
