package ai.ancf.lmos.kotlinsdk.server

import ai.ancf.lmos.kotlinsdk.base.model.AgentRequest
import ai.ancf.lmos.kotlinsdk.base.model.AgentResult
import ai.ancf.lmos.wot.thing.schema.WoTExposedThing
import kotlinx.coroutines.flow.Flow

data class LmosAgent(
    var id: String,
    var title: String,
    var description: String,
    var capabilities: String,
    var chat: (input: AgentRequest) -> AgentResult,
    // var events: Flow<String>
)