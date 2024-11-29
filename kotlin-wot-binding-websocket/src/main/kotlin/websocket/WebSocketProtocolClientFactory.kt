package websocket

import ai.anfc.lmos.wot.binding.ProtocolClientFactory
import io.ktor.client.*

/**
 * Creates new [WebSocketProtocolClient] instances.
 */
open class WebSocketProtocolClientFactory(private val httpClientConfig: HttpClientConfig? = null) : ProtocolClientFactory {
    override fun toString(): String {
        return "WebSocketProtocolClient"
    }
    override val scheme: String
        get() = "ws"
    override val client: WebSocketProtocolClient
        get() = WebSocketProtocolClient(httpClientConfig)

    override suspend fun init() {
       // TODO
    }

    override suspend fun destroy() {
        // TODO
    }
}