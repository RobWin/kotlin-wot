package ai.ancf.lmos.kotlinsdk.base.model

data class Message(
    val role: String,
    val content: String,
    val format: String = "text",
    val turnId: String? = null,
    val binaryData: List<BinaryData>? = null,
)

class BinaryData(val mimeType: String, val dataAsBase64: String? = null, val source: String? = null)