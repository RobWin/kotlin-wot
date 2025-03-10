/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.thingweb

import org.eclipse.thingweb.thing.ExposedThing
import org.eclipse.thingweb.thing.ThingDescription
import org.eclipse.thingweb.thing.filter.DiscoveryMethod
import org.eclipse.thingweb.thing.filter.ThingFilter
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.net.URI
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class WotTest {

    // Mocked dependency
    private lateinit var servient: Servient
    private lateinit var defaultWot: DefaultWot

    @BeforeTest
    fun setup() {
        servient = mockk()  // Mock the Servient class
        defaultWot = DefaultWot(servient)
    }

    @Test
    fun `test discover with filter`() = runTest {
        // Given
        val filter = ThingFilter(method = DiscoveryMethod.ANY)
        val thing = mockk<ExposedThing>()
        coEvery { servient.discover(filter) } returns flowOf(thing)

        // When
        val result = defaultWot.discover(filter).toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(thing, result.first())
        coVerify { servient.discover(filter) }
    }

    @Test
    fun `test discover without filter`() = runTest {
        // Given
        val filter = ThingFilter(method = DiscoveryMethod.ANY)
        val thing = mockk<ExposedThing>()
        coEvery { servient.discover(filter) } returns flowOf(thing)

        // When
        val result = defaultWot.discover().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(thing, result.first())
        coVerify { servient.discover(filter) }
    }

    @Test
    fun `test fetch with URI`() = runTest {
        // Given
        val url = URI("http://example.com")
        val thingDescription = mockk<ThingDescription>()
        coEvery { servient.fetch(url) } returns thingDescription

        // When
        val result = defaultWot.requestThingDescription(url)

        // Then
        assertEquals(thingDescription, result)
        coVerify { servient.fetch(url) }
    }

    @Test
    fun `test fetch with String URL`() = runTest {
        // Given
        val urlString = "http://example.com"
        val thingDescription = mockk<ThingDescription>()
        coEvery { servient.fetch(urlString) } returns thingDescription

        // When
        val result = defaultWot.requestThingDescription(urlString)

        // Then
        assertEquals(thingDescription, result)
        coVerify { servient.fetch(urlString) }
    }

    @Test
    fun `test destroy`() = runTest {
        // Given
        coEvery { servient.shutdown() } just runs

        // When
        defaultWot.destroy()

        // Then
        coVerify { servient.shutdown() }
    }


    @Test
    fun `produce should return ExposedThing when adding is successful`() = runTest {
        // Arrange

        // Mocking servient.addThing to return true, indicating the Thing is added successfully
        every { servient.addThing(ofType(ExposedThing::class)) } returns true

        // Act
        val exposedThing = defaultWot.produce{
            title = "testTitle"
            stringProperty("propA") {
                title = "some title"
            }
            intProperty("propB") {
                title = "some title"
            }
        }

        // Assert
        assertEquals(exposedThing.getThingDescription().title, "testTitle")
    }

    @Test
    fun `produce should throw WotException when thing already exists`() = runTest {
        // Arrange
        val thingDescription = ThingDescription(id = "existingThing")

        // Mocking servient.addThing to return false, indicating the Thing already exists
        coEvery { servient.addThing(ofType(ExposedThing::class)) } returns false

        // Act and Assert
        val exception = assertFailsWith<WotException> {
            defaultWot.produce(thingDescription)
        }
        assertEquals("Thing already exists: existingThing", exception.message)
    }
}