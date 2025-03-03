/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.thingweb


import org.eclipse.thingweb.security.SecurityScheme
import org.eclipse.thingweb.thing.ConsumedThing
import org.eclipse.thingweb.thing.ExposedThing
import org.eclipse.thingweb.thing.ThingDescription
import org.eclipse.thingweb.thing.filter.DiscoveryMethod
import org.eclipse.thingweb.thing.filter.ThingFilter
import org.eclipse.thingweb.thing.schema.WoTExposedThing
import org.eclipse.thingweb.thing.schema.WoTThingDescription
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.flow.Flow
import java.net.URI
import java.net.URISyntaxException

/**
 * Standard implementation of [Wot].
 */
class DefaultWot(private val servient: Servient) : Wot {

    override fun toString(): String {
        return "DefaultWot{" +
                "servient=" + servient +
                '}'
    }
    @Throws(WotException::class)
    @WithSpan(kind = SpanKind.CLIENT)
    override fun discover(filter: ThingFilter): Flow<WoTThingDescription> {
        return servient.discover(filter)
    }

    @Throws(WotException::class)
    @WithSpan(kind = SpanKind.CLIENT)
    override fun discover(): Flow<WoTThingDescription> {
        return discover(ThingFilter(method = DiscoveryMethod.ANY))
    }

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun exploreDirectory(directoryUrl: String, securityScheme: SecurityScheme): Set<WoTThingDescription> {
        return servient.exploreDirectory(directoryUrl, securityScheme)
    }

    override fun produce(thingDescription: WoTThingDescription): WoTExposedThing {
        val exposedThing = ExposedThing(servient, thingDescription)
        return if (servient.addThing(exposedThing)) {
            exposedThing
        } else {
            throw WotException("Thing already exists: " + thingDescription.id)
        }
    }

    override fun produce(configure: ThingDescription.() -> Unit): WoTExposedThing {
        val thingDescription = ThingDescription().apply(configure)
        return produce(thingDescription)
    }

    override fun consume(thingDescription: WoTThingDescription) = ConsumedThing(servient, thingDescription)

    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun requestThingDescription(url: URI, securityScheme: SecurityScheme): WoTThingDescription {
        return servient.fetch(url, securityScheme)
    }

    @Throws(URISyntaxException::class)
    @WithSpan(kind = SpanKind.CLIENT)
    override suspend fun requestThingDescription(url: String, securityScheme: SecurityScheme): WoTThingDescription {
        return servient.fetch(url, securityScheme)
    }

    suspend fun destroy() {
        return servient.shutdown()
    }
}
