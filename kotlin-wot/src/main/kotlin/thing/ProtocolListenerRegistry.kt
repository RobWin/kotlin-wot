package ai.ancf.lmos.wot.thing

import ai.ancf.lmos.wot.content.ContentManager
import ai.ancf.lmos.wot.thing.schema.ContentListener
import ai.ancf.lmos.wot.thing.schema.DataSchema
import ai.ancf.lmos.wot.thing.schema.InteractionAffordance
import ai.ancf.lmos.wot.thing.schema.InteractionInput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ProtocolListenerRegistry {

    private val log: Logger = LoggerFactory.getLogger(ProtocolListenerRegistry::class.java)

    // Map affordances to form indices and their associated listener
    internal val listeners: ConcurrentMap<InteractionAffordance, MutableMap<Int, ContentListener>> = ConcurrentHashMap()

    /**
     * Registers a listener for a specific affordance and form index.
     * If a listener already exists for the specified form index, it will be replaced.
     *
     * @param affordance The interaction affordance.
     * @param formIndex The index of the form.
     * @param listener The listener to register.
     */
    fun register(affordance: InteractionAffordance, formIndex: Int, listener: ContentListener) {
        val form = affordance.forms.getOrNull(formIndex)
            ?: throw IllegalArgumentException("Can't register listener; no form at index $formIndex for the affordance")

        val formMap = listeners.getOrPut(affordance) { mutableMapOf() }
        formMap[formIndex] = listener // Replaces any existing listener for the form index
    }

    /**
     * Unregisters the listener for a specific affordance and form index.
     *
     * @param affordance The interaction affordance.
     * @param formIndex The index of the form.
     * @throws IllegalStateException if no listener is found for the affordance or form index.
     */
    fun unregister(affordance: InteractionAffordance, formIndex: Int) {
        val formMap = listeners[affordance] ?: throw IllegalStateException("Listener not found for affordance")
        val wasRemoved = formMap.remove(formIndex) != null
        if (!wasRemoved) throw IllegalStateException("Listener not found at form index $formIndex")
    }

    /**
     * Unregisters all listeners for all affordances.
     */
    fun unregisterAll() {
        listeners.clear()
    }

    /**
     * Notifies the listener for a given affordance and optional form index.
     *
     * @param affordance The interaction affordance.
     * @param data The input data.
     * @param schema The schema for validation (optional).
     * @param formIndex The index of the form to notify the listener for (optional).
     */
    suspend fun <T> notify(
        affordance: InteractionAffordance,
        data: InteractionInput,
        schema: DataSchema<T>? = null,
        formIndex: Int? = null
    ) {
        log.debug("Notify listeners for affordance with title '${affordance.title}'")
        val formMap = listeners[affordance] ?: emptyMap()

        val interactionInputValue = data as InteractionInput.Value

        if (formIndex != null) {
            formMap[formIndex]?.let { listener ->
                val contentType = affordance.forms[formIndex].contentType
                try {
                    val content = ContentManager.valueToContent(interactionInputValue.value, contentType)
                    listener.handle(content)
                } catch (e: Exception) {
                    log.error("Error while notifying listener", e)
                }
            }
            return
        }

        formMap.forEach { (index, listener) ->
            log.debug("Notify listener for form {}", affordance.forms[index])
            val contentType = affordance.forms[index].contentType
            try {
                val content = ContentManager.valueToContent(interactionInputValue.value, contentType)
                listener.handle(content)
            } catch (e: Exception) {
                log.error("Error while notifying listener", e)
            }
        }
    }
}