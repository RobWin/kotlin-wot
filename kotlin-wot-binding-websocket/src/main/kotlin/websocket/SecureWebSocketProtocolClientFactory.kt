package ai.ancf.lmos.wot.binding.websocket

/**
 * Creates new [WebSocketProtocolClient] instances that allow consuming Things via WSS.
 */
class SecureWebSocketProtocolClientFactory() : WebSocketProtocolClientFactory() {

    override fun toString(): String {
        return "SecureWebSocketProtocolClient"
    }

    override val scheme: String
        get() = "wss"
}
