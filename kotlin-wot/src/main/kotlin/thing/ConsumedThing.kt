/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.thingweb.thing

import AugmentedForm
import org.eclipse.thingweb.JsonMapper
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.ServientException
import org.eclipse.thingweb.content.Content
import org.eclipse.thingweb.content.ContentManager
import org.eclipse.thingweb.credentials.DefaultCredentialsProvider
import org.eclipse.thingweb.parseInteractionOptions
import org.eclipse.thingweb.thing.form.Form
import org.eclipse.thingweb.thing.form.Operation
import org.eclipse.thingweb.thing.schema.*
import ai.anfc.lmos.wot.binding.ProtocolClient
import ai.anfc.lmos.wot.binding.ProtocolClientException
import ai.anfc.lmos.wot.binding.Resource
import ai.anfc.lmos.wot.binding.ResourceType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Represents an object that extends a Thing with methods for client interactions (send request for
 * reading and writing Properties), invoke Actions, subscribe and unsubscribe for Property changes
 * and Events. https://w3c.github.io/wot-scripting-api/#the-consumedthing-interface
 */
data class ConsumedThing(
    @JsonIgnore
    private val servient: Servient,
    private val thingDescription: WoTThingDescription = ThingDescription(),
) : WoTConsumedThing, WoTThingDescription by thingDescription  {

    private val subscribedEvents: ConcurrentMap<String, Subscription> = ConcurrentHashMap()
    private val observedProperties: ConcurrentMap<String, Subscription> = ConcurrentHashMap()
    private val clients: MutableMap<String, ProtocolClient> = mutableMapOf()

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun readProperty(@SpanAttribute("wot.property.name") propertyName: String, options: InteractionOptions?): WoTInteractionOutput {
        Span.current().setAttribute("wot.thing.id", id)
        val property = this.properties[propertyName]
        requireNotNull(property) { "ConsumedThing '${this.title}' does not have property $propertyName" }

        return try {
            // Ensure the property exists
            // Retrieve the client and form for the property
            val (client, form) = getClientFor(property.forms, Operation.READ_PROPERTY, options)

            // Log the action
            log.debug("ConsumedThing '{}' reading {}", this.title, form.href)

            // Handle URI variables if present
            val finalForm = handleUriVariables(this, property, form, options)

            // Use the client to read the resource
            val content = client.readResource(Resource(id, propertyName, finalForm))

            // Process and handle the interaction output

            handleInteractionOutput(content, finalForm, property)
        } catch (e: Exception) {
            throw ConsumedThingException("Error while processing property for ${property.title}. ${e.message}", e)
        }
    }

    private fun handleInteractionOutput(
        content: Content,
        form: WoTForm,
        outputDataSchema: DataSchema<*>?
    ): WoTInteractionOutput {

        // Check if returned media type matches the expected media type from TD
        form.response?.let { response ->
            if (ContentManager.getMediaType(content.type) != ContentManager.getMediaType(response.contentType)) {
                throw IllegalArgumentException(
                    "Unexpected type '${content.type}' in response. Expected '${response.contentType}'"
                )
            }
        }

        return InteractionOutput(content, outputDataSchema)
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun readAllProperties(options: InteractionOptions?): PropertyReadMap {
        Span.current().setAttribute("wot.thing.id", id)
        val propertyNames = mutableListOf<String>()

        // Iterate over all properties
        for ((propertyName, property) in properties) {
            try {
                // Get the form for the "readproperty" action
                val form = getClientFor(property.forms, Operation.READ_PROPERTY, options)
                // If a valid form is found, add the property name to the list
                propertyNames.add(propertyName)
            } catch (e: Exception) {
                // Handle any error that might occur during processing
                println("Error processing property $propertyName: ${e.message}")
            }
        }

        // Call the internal function to read properties and return the result
        return readProperties(propertyNames)
    }

    @WithSpan(kind = SpanKind.CLIENT)
    private suspend fun readProperties(propertyNames: List<String>): PropertyReadMap {
        Span.current().setAttribute("wot.thing.id", id)
        // Create a list of promises (in Kotlin, this is coroutines)
        val promises: List<Deferred<WoTInteractionOutput>> = propertyNames.map { propertyName ->
            coroutineScope {
                async { readProperty(propertyName) }
            }
        }

        // Wait for all promises to complete and create the result map
        val output = mutableMapOf<String, WoTInteractionOutput>()
        try {
            // Await all promises and collect the results
            val results = promises.awaitAll()
            propertyNames.forEachIndexed { index, propertyName ->
                output[propertyName] = results[index]
            }
            return output
        } catch (e: Exception) {
            throw Exception("ConsumedThing '${title}', failed to read properties: $propertyNames.\n Error: ${e.message}")
        }
    }

    // Function to read multiple properties
    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun readMultipleProperties(propertyNames: List<String>, options: InteractionOptions?): PropertyReadMap {
        return try {
            // Simply call the internal function for reading the specified properties
            Span.current().setAttribute("wot.thing.id", id)
            readProperties(propertyNames)
        } catch (e: Exception) {
            // Handle error when reading multiple properties
            println("Error reading multiple properties: ${e.message}")
            throw e  // Propagate the error after logging
        }
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun writeProperty(@SpanAttribute("wot.property.name")propertyName: String, input: InteractionInput, options: InteractionOptions?) {
        val interactionValue = input as? InteractionInput.Value
            ?: throw UnsupportedOperationException("Streaming input is not supported for property: $propertyName")
        writeProperty(propertyName, interactionValue.value)
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun writeProperty(
        @SpanAttribute("wot.property.name") propertyName: String,
        value: JsonNode,
        options: InteractionOptions?) {
        Span.current().setAttribute("wot.thing.id", id)
        val property = this.properties[propertyName]
        requireNotNull(property) { "ConsumedThing '${this.title}' does not have property $propertyName" }

        return try {
            // Ensure the property exists
            // Retrieve the client and form for the property
            val (client, form) = getClientFor(property.forms, Operation.WRITE_PROPERTY, options)

            // Log the action
            log.debug("ConsumedThing '{}' reading {}", this.title, form.href)

            // Handle URI variables if present
            val finalForm = handleUriVariables(this, property, form, options)

            val content = ContentManager.valueToContent(value, finalForm.contentType)

            client.writeResource(Resource(id, propertyName, finalForm), content)

        } catch (e: Exception) {
            throw ConsumedThingException("Error while processing property for ${property.title}. ${e.message}", e)
        }
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun writeMultipleProperties(valueMap: PropertyWriteMap, options: InteractionOptions?) {
        Span.current().setAttribute("wot.thing.id", id)
        // Collect all deferred write operations into a list
        val deferredWrites = mutableListOf<Deferred<Unit>>()

        coroutineScope {
            for ((propertyName, value) in valueMap) {
                deferredWrites.add(async { writeProperty(propertyName, value) })
            }
        }

        // Wait for all deferred operations to complete
        try {
            deferredWrites.awaitAll()
        } catch (e: Exception) {
            throw ConsumedThingException("ConsumedThing '$title', failed to write multiple properties. Error: ${e.message}", e)
        }
    }

    private suspend fun invokeActionInternal(
        actionName: String,
        value: JsonNode,
        options: InteractionOptions?
    ): WoTInteractionOutput {
        Span.current().setAttribute("wot.thing.id", id)
        val action = this.actions[actionName]
        requireNotNull(action) { "ConsumedThing '${this.title}' does not have action $actionName" }

        try {
            // Retrieve the client and form for the action
            val (client, form) = getClientFor(action.forms, Operation.INVOKE_ACTION, options)

            // Log the action
            log.debug("ConsumedThing '{}' invoke {}", this.title, form.href)

            // Handle URI variables if present
            val finalForm = handleUriVariables(this, action, form, options)

            val content = ContentManager.valueToContent(value, finalForm.contentType)

            // Invoke the action
            val response = client.invokeResource(Resource(id, actionName, finalForm), content)

            return InteractionOutput(response, action.output)
        } catch (e: Exception) {
            throw ConsumedThingException("Error while invoking action for ${action.title}. ${e.message}", e)
        }
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun invokeAction(
        @SpanAttribute("wot.action.name") actionName: String,
        input: InteractionInput,
        options: InteractionOptions?
    ): WoTInteractionOutput {
        val interactionValue = input as? InteractionInput.Value
            ?: throw UnsupportedOperationException("Streaming input is not supported for action: $actionName")
        return invokeActionInternal(actionName, interactionValue.value, options)
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun invokeAction(
        @SpanAttribute("wot.action.name")  actionName: String,
        input: JsonNode,
        options: InteractionOptions?
    ): JsonNode {
        val output = invokeActionInternal(actionName, input, options)
        return output.value()
    }

     suspend inline fun <reified I, reified O> invokeAction(actionName: String, input: I, options: InteractionOptions? = InteractionOptions()): O {
        val inputAsJsonNode : JsonNode = JsonMapper.instance.valueToTree(input)
        val output : JsonNode = invokeAction(actionName, input = inputAsJsonNode, options)
        return JsonMapper.instance.treeToValue<O>(output)
    }

    suspend inline fun <reified I> invokeUnitAction(
        actionName: String,
        input: I,
        options: InteractionOptions? = InteractionOptions()
    ) {
        invokeAction<I, Unit>(actionName, input, options)
    }

    override suspend fun invokeAction(
        actionName: String,
        options: InteractionOptions?
    ): JsonNode {
        val output = invokeActionInternal(actionName, NullNode.instance, options)
        return output.value()
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun observeProperty(
        @SpanAttribute("wot.property.name") propertyName: String,
        listener: InteractionListener,
        errorListener: ErrorListener?,
        options: InteractionOptions?
    ): Subscription {
        Span.current().setAttribute("wot.thing.id", id)
        val property = this.properties[propertyName]
        requireNotNull(property) { "ConsumedThing '${this.title}' does not have property $propertyName" }

        val (client, form) = getClientFor(property.forms, Operation.OBSERVE_PROPERTY, options)

        if (observedProperties.containsKey(propertyName)) {
            throw IllegalStateException("ConsumedThing '$title' already has a function subscribed to $property. You can only observe once.")
        }

        log.debug("ConsumedThing '$title' observing to ${form.href}")

        // Process URI variables if present
        val formWithoutURITemplates = handleUriVariables(this, property, form, options)

        // Subscribe to the resource
       
        client.subscribeResource(Resource(id, propertyName, formWithoutURITemplates), ResourceType.PROPERTY)
            .catch { error ->
                // Handle any error by passing it to the errorListener if defined
                errorListener?.handle(error)
                log.warn("Error while processing observe property for ${property.title}: ${error.message}", error)
            }
            .onEach { content ->
                // Pass each result to the listener
                listener.handle(handleInteractionOutput(content, form, property))
            }.launchIn(CoroutineScope(Dispatchers.IO))

        val subscription = InternalPropertySubscription(this, propertyName, client, form)
        observedProperties[propertyName] = subscription
        return subscription
    }

    @WithSpan(kind = SpanKind.CLIENT)
    suspend fun consumeEvent(
        @SpanAttribute("wot.event.name") eventName: String,
        options: InteractionOptions = InteractionOptions()
    ): Flow<WoTInteractionOutput> {

        val eventAffordance = requireNotNull(events[eventName]) {
            "ConsumedThing '$title', no event found for '$eventName'"
        }

        val (client, form) = getClientFor(eventAffordance.forms, Operation.SUBSCRIBE_EVENT, options)

        if (subscribedEvents.containsKey(eventName)) {
            return flow {
                throw ConsumedThingException("ConsumedThing '$title' already has a function subscribed to $eventName. You can only subscribe once.")
            }
        }

        log.debug("ConsumedThing '$title' subscribing to ${form.href}")

        val formWithoutURITemplates = handleUriVariables(this, eventAffordance, form, options)

        val subscription = InternalEventSubscription(this, eventName, client, form)
        subscribedEvents[eventName] = subscription

         val flow = client.subscribeResource(Resource(id, eventName, formWithoutURITemplates), ResourceType.EVENT)
            .map { handleInteractionOutput(it, form, eventAffordance.data) }
            .onCompletion {
                error -> if (error != null && error !is CancellationException) { // Ignore cancellations
                    log.warn("Error while processing observe property for ${eventAffordance.title}: ${error.message}", error)
                }
                subscription.stop(options)
            }

        return flow
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun subscribeEvent(
        @SpanAttribute("wot.event.name") eventName: String,
        listener: InteractionListener,
        errorListener: ErrorListener?,
        options: InteractionOptions?
    ): Subscription {
        val eventAffordance = requireNotNull(events[eventName]) {
            "ConsumedThing '$title', no event found for '$eventName'"
        }

        val (client, form) = getClientFor(eventAffordance.forms, Operation.SUBSCRIBE_EVENT, options)

        if (subscribedEvents.containsKey(eventName)) {
            throw ConsumedThingException("ConsumedThing '$title' already has a function subscribed to $eventName. You can only subscribe once.")
        }

        log.debug("ConsumedThing '$title' subscribing to ${form.href}")

        // Process URI variables if present
        val formWithoutURITemplates = handleUriVariables(this, eventAffordance, form, options)

        // Subscribe to the resource
        client.subscribeResource(Resource(id, eventName, formWithoutURITemplates), ResourceType.EVENT)
            .catch { error ->
                errorListener?.handle(error)
                log.warn(
                    "Error while processing observe property for ${eventAffordance.title}: ${error.message}",
                    error
                )
            }.onEach { content ->
                listener.handle(handleInteractionOutput(content, form, eventAffordance.data))
            }.launchIn(CoroutineScope(Dispatchers.IO))

        val subscription = InternalEventSubscription(this, eventName, client, form)
        subscribedEvents[eventName] = subscription
        return subscription
    }

    @JsonIgnore
    override fun getThingDescription(): WoTThingDescription {
        return this
    }

    fun getClientFor(
        form: WoTForm,
        op: Operation
    ): Pair<ProtocolClient?, WoTForm> {
        return getClientFor(listOf(form), op, InteractionOptions())
    }

    /**
     * Searches and returns a ProtocolClient in given `forms` that matches the given
     * `op`. Throws an exception when no client can be found.
     *
     * @param forms
     * @param op
     * @return
     * @throws ConsumedThingException
     */

    private fun getClientFor(forms: List<WoTForm>, op: Operation, options: InteractionOptions?): Pair<ProtocolClient, WoTForm> {
        require(forms.isNotEmpty()) { "No forms available for operation $op on $id" }

        // Get supported schemes in the order of preference
        val supportedSchemes = servient.getClientSchemes()

        // Find schemes in forms sorted by supported schemes preference
        val augmentedForms = forms
            .map { AugmentedForm(it, thingDescription) }

        val schemes = augmentedForms
            .mapNotNull { it.hrefScheme }
            .distinct()
            .sortedBy { supportedSchemes.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
            .toSet()

        require(schemes.isNotEmpty()) { "No schemes in forms found" }

        // Try finding a cached client for one of the schemes
        val (scheme, client) = schemes
            .firstNotNullOfOrNull { scheme -> clients[scheme]?.let { scheme to it } }
            ?: initNewClientFor(schemes).also { (newScheme, newClient) ->
                clients[newScheme] = newClient
                log.debug("'$id' got new client for scheme '$newScheme'")
            }

        log.debug("'$id' chose client for scheme '$scheme'")

        val form = getFormForOpAndScheme(augmentedForms, op, scheme)
        return client to form
    }


    private fun initNewClientFor(schemes: Set<String>): Pair<String, ProtocolClient> {
        try {
            schemes.forEach { scheme ->
                val client = servient.getClientFor(scheme)
                if (client != null) {
                    // Initialize client security system if security details are provided
                    security.takeIf { it.isNotEmpty() }?.let {
                        log.debug("'{}' setting credentials for '{}'", id, client)
                        val securitySchemes = security.mapNotNull { key -> securityDefinitions[key] }
                        client.setCredentialsProvider(DefaultCredentialsProvider(securitySchemes, servient.credentialStore))

                    }
                    return scheme to client
                }
            }
            throw NoClientFactoryForSchemesException(id, schemes)
        } catch (e: ProtocolClientException) {
            throw ConsumedThingException("Unable to create client: ${e.message}")
        }
    }

    private fun getFormForOpAndScheme(
        forms: List<AugmentedForm>,
        op: Operation,
        scheme: String?
    ): AugmentedForm {
        // Find a form that matches the operation and scheme
        return forms.firstOrNull { it.op?.contains(op) == true && it.hrefScheme == scheme }
            ?: forms.firstOrNull { it.op.isNullOrEmpty() && it.hrefScheme == scheme }
            ?: throw NoFormForInteractionException(id, op)
    }

    /**
     * Creates new form (if needed) for URI Variables http://192.168.178.24:8080/counter/actions/increment{?step}
     * with '{'step' : 3}' -&gt; http://192.168.178.24:8080/counter/actions/increment?step=3.<br></br>
     * see RFC6570 (https://tools.ietf.org/html/rfc6570) for URI Template syntax
     */
    fun handleUriVariables(form: Form, parameters: Map<String, String>): Form {
        val href: String = form.href
        val uriTemplate: UriTemplate = UriTemplate.fromTemplate(href)
        val updatedHref: String = uriTemplate.expand(parameters)
        if (href != updatedHref) {
            // "clone" form to avoid modifying original form
            val updatedForm = Form(updatedHref)
            log.debug("'{}' update URI to '{}'", href, updatedHref)
            return updatedForm
        }
        return form
    }

    fun handleUriVariables(
        thing: WoTConsumedThing,
        ti: InteractionAffordance,
        form: WoTForm,
        options: InteractionOptions?
    ): WoTForm {
        val uriTemplate = UriTemplate.fromTemplate(form.href)
        val uriVariables = parseInteractionOptions(thingDescription, ti, options).uriVariables ?: emptyMap()
        val updatedHref = uriTemplate.expand(uriVariables)

        return if (updatedHref != form.href) {
            // Create a shallow copy and update href
            /*
            form.copy(href = updatedHref).also {
                log.debug("ConsumedThing '${thingDescription.title}' updated form URI to ${it.href}")
            }
            */
            form
        } else {
            form
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConsumedThing

        if (id != other.id) return false
        if (objectType != other.objectType) return false
        if (objectContext != other.objectContext) return false
        if (title != other.title) return false
        if (titles != other.titles) return false
        if (description != other.description) return false
        if (descriptions != other.descriptions) return false
        if (properties != other.properties) return false
        if (actions != other.actions) return false
        if (events != other.events) return false
        if (forms != other.forms) return false
        if (security != other.security) return false
        if (securityDefinitions != other.securityDefinitions) return false
        if (base != other.base) return false
        if (version != other.version) return false
        if (created != other.created) return false
        if (modified != other.modified) return false
        if (support != other.support) return false
        if (links != other.links) return false
        if (profile != other.profile) return false
        if (schemaDefinitions != other.schemaDefinitions) return false
        if (uriVariables != other.uriVariables) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (objectType?.hashCode() ?: 0)
        result = 31 * result + (objectContext?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (titles?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (descriptions?.hashCode() ?: 0)
        result = 31 * result + properties.hashCode()
        result = 31 * result + actions.hashCode()
        result = 31 * result + events.hashCode()
        result = 31 * result + forms.hashCode()
        result = 31 * result + security.hashCode()
        result = 31 * result + securityDefinitions.hashCode()
        result = 31 * result + (base?.hashCode() ?: 0)
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (created?.hashCode() ?: 0)
        result = 31 * result + (modified?.hashCode() ?: 0)
        result = 31 * result + (support?.hashCode() ?: 0)
        result = 31 * result + (links?.hashCode() ?: 0)
        result = 31 * result + (profile?.hashCode() ?: 0)
        result = 31 * result + (schemaDefinitions?.hashCode() ?: 0)
        result = 31 * result + (uriVariables?.hashCode() ?: 0)
        return result
    }

    abstract class InternalSubscription(
        protected val thing: ConsumedThing,
        protected val name: String,
        protected val client: ProtocolClient
    ) : Subscription

    class InternalPropertySubscription(
        thing: ConsumedThing,
        name: String,
        client: ProtocolClient,
        form: WoTForm,
        override var active: Boolean = true
    ) : InternalSubscription(thing, name, client) {

        private var formIndex: Int = -1

        init {
            val index = thing.properties[name]?.forms?.indexOfFirst { it.href == form.href }
            if (index == null || index < 0) {
                throw Error("Could not find form ${form.href} in property $name")
            }
            formIndex = index
        }

        override suspend fun stop(options: InteractionOptions) {
            unobserveProperty(options)
            thing.observedProperties.remove(name)
        }

        private suspend fun unobserveProperty(options: InteractionOptions = InteractionOptions()) {
            val property = thing.properties[name]
            requireNotNull(property) { "ConsumedThing '${thing.title}' does not have property $name" }
            options.formIndex = options.formIndex ?: matchingUnsubscribeForm()
            val ( client, form ) = thing.getClientFor(property.forms, Operation.UNOBSERVE_PROPERTY, options)


            val formWithoutURIvariables = thing.handleUriVariables(thing, property, form, options)
            log.debug("ConsumedThing '${thing.title}' unobserving to ${form.href}")
            client.unlinkResource(Resource(thing.id, name, formWithoutURIvariables), ResourceType.PROPERTY)
            active = false
        }

        private fun matchingUnsubscribeForm(): Int {
            val refForm = thing.properties[name]?.forms?.get(formIndex)
            return if (refForm?.op == null || refForm.op.contains(Operation.UNOBSERVE_PROPERTY)) {
                formIndex
            } else {
                val bestFormMatch = findFormIndexWithScoring(formIndex, thing.properties[name]?.forms ?: emptyList(), Operation.UNOBSERVE_PROPERTY)
                if (bestFormMatch == -1) throw Error("Could not find matching form for unsubscribe")
                bestFormMatch
            }
        }
    }

    class InternalEventSubscription(
        thing: ConsumedThing,
        name: String,
        client: ProtocolClient,
        private val form: WoTForm,
        override var active: Boolean = true
    ) : InternalSubscription(thing, name, client) {

        private var formIndex: Int = -1

        init {
            val forms = thing.events[name]?.forms
            val index = forms?.indexOfFirst { it.href == form.href }
            if (index == null || index < 0) {
                throw Error("Could not find form ${form.href} in event $name")
            }
            formIndex = index
        }

        override suspend fun stop(options: InteractionOptions) {
            unsubscribeEvent(options)
            thing.subscribedEvents.remove(name)
        }

        suspend fun unsubscribeEvent(options: InteractionOptions = InteractionOptions()){
            val event = requireNotNull(thing.events[name]) {
                "ConsumedThing '${thing.title}', no event found for '$name'"
            }
            options.formIndex = options.formIndex ?: matchingUnsubscribeForm()
            val ( client, form ) = thing.getClientFor(event.forms, Operation.UNSUBSCRIBE_EVENT, options)

            val formWithoutURIvariables = thing.handleUriVariables(thing, event, form, options)
            log.debug("ConsumedThing '${thing.title}' unsubscribing to ${form.href}")
            client.unlinkResource(Resource(thing.id, name, formWithoutURIvariables), ResourceType.EVENT)
            active = false
        }

        private fun matchingUnsubscribeForm(): Int {
            val refForm = thing.events[name]?.forms?.get(formIndex)
            return if (refForm?.op == null || refForm.op.contains(Operation.UNSUBSCRIBE_EVENT)) {
                formIndex
            } else {
                val bestFormMatch = findFormIndexWithScoring(formIndex, thing.events[name]?.forms ?: emptyList(), Operation.UNSUBSCRIBE_EVENT)
                if (bestFormMatch == -1) throw Error("Could not find matching form for unsubscribe")
                bestFormMatch
            }
        }

    }

    companion object {
        private val log = LoggerFactory.getLogger(ConsumedThing::class.java)

        /**
         * Parses a JSON string into a Thing object.
         *
         * @param json JSON string to parse.
         * @return A Thing instance if parsing is successful, otherwise null.
         */
        fun fromJson(json: String?): ConsumedThing? {
            return try {
                JsonMapper.instance.readValue(json, ConsumedThing::class.java)
            } catch (e: IOException) {
                log.warn("Unable to read json", e)
                null
            }
        }

        /**
         * Parses a JSON file into a Thing object.
         *
         * @param file JSON file to parse.
         * @return A Thing instance if parsing is successful, otherwise null.
         */
        fun fromJson(file: File?): ConsumedThing? {
            return try {
                JsonMapper.instance.readValue(file, ConsumedThing::class.java)
            } catch (e: IOException) {
                log.warn("Unable to read json", e)
                null
            }
        }

        /**
         * Converts a Map into a Thing object.
         *
         * @param map Map representing the Thing structure.
         * @return A Thing instance converted from the map.
         */
        fun fromMap(map: Map<*, *>): ConsumedThing {
            return JsonMapper.instance.convertValue(map, ConsumedThing::class.java)
        }
    }
}

class NoFormForInteractionException : ConsumedThingException {
    constructor(title: String, op: Operation) : super("'$title' has no form for interaction '$op'")
    constructor(message: String) : super(message)
}


class NoClientFactoryForSchemesException(title: String, schemes: Set<String?>) :
    ConsumedThingException("'$title': Missing ClientFactory for schemes '$schemes'")

open class ConsumedThingException : ServientException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable?) : super(cause)

    constructor(message: String, cause: Throwable?) : super(cause)
}


fun findFormIndexWithScoring(
    formIndex: Int,
    forms: List<Form>,
    operation: Operation
): Int {
    val refForm = forms[formIndex]
    var maxScore = 0
    var maxScoreIndex = -1

    for (i in forms.indices) {
        var score = 0
        val form = forms[i]

        // Check if the form's operation matches the desired operation
        if ((form.op?.contains(operation) == true)) {
            score += 1
        }

        // Compare the origins of the URLs
        try {
            val formUrl = URI(form.href).toURL()
            val refFormUrl = URI(refForm.href).toURL()
            if (formUrl.protocol == refFormUrl.protocol && formUrl.host == refFormUrl.host) {
                score += 1
            }
        } catch (e: MalformedURLException) {
            // Handle invalid URLs, if needed
            e.printStackTrace()
        }

        // Compare the content types
        if (form.contentType == refForm.contentType) {
            score += 1
        }

        // Update max score and index if the current score is higher
        if (score > maxScore) {
            maxScore = score
            maxScoreIndex = i
        }
    }

    return maxScoreIndex
}

