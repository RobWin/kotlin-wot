/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.thingweb.thing.credentials


import org.eclipse.thingweb.credentials.BasicCredentials
import org.eclipse.thingweb.credentials.BearerCredentials
import org.eclipse.thingweb.credentials.DefaultCredentialsProvider
import org.eclipse.thingweb.credentials.NoCredentialsFound
import org.eclipse.thingweb.security.BasicSecurityScheme
import org.eclipse.thingweb.security.BearerSecurityScheme
import org.eclipse.thingweb.security.SecurityScheme
import org.eclipse.thingweb.thing.form.Form
import kotlin.test.*

class DefaultCredentialsProviderTest {

    @Test
    fun `should return BasicCredentials when href matches key`() {
        val securitySchemes = listOf(BasicSecurityScheme())
        val credentials = mapOf(
            "https://example.com/device1" to BasicCredentials("user1", "pass1")
        )
        val provider = DefaultCredentialsProvider(securitySchemes, credentials)
        val form = Form(href = "https://example.com/device1/status")

        val result = provider.getCredentials(form)

        assertTrue(result is BasicCredentials)
        assertEquals("user1", result.username)
        assertEquals("pass1", result.password)
    }

    @Test
    fun `should return BearerCredentials when href matches key`() {
        val securitySchemes = listOf(BearerSecurityScheme())
        val credentials = mapOf(
            "https://secure.com/api" to BearerCredentials("secureToken123")
        )
        val provider = DefaultCredentialsProvider(securitySchemes, credentials)
        val form = Form(href = "https://secure.com/api/resource")

        val result = provider.getCredentials(form)

        assertTrue(result is BearerCredentials)
        assertEquals("secureToken123", result.token)
    }

    @Test
    fun `should throw exception when no matching credentials found`() {
        val securitySchemes = listOf(BasicSecurityScheme())
        val credentials = mapOf(
            "https://example.com/device1" to BasicCredentials("user1", "pass1")
        )
        val provider = DefaultCredentialsProvider(securitySchemes, credentials)
        val form = Form(href = "https://unknown.com/deviceX")

        assertFailsWith<NoCredentialsFound> { provider.getCredentials(form) }
    }

    @Test
    fun `should return null when securitySchemes is empty`() {
        val securitySchemes = emptyList<SecurityScheme>()
        val credentials = mapOf(
            "https://example.com/device1" to BasicCredentials("user1", "pass1")
        )
        val provider = DefaultCredentialsProvider(securitySchemes, credentials)
        val form = Form(href = "https://example.com/device1/status")

        val result = provider.getCredentials(form)

        assertNull(result)
    }

    @Test
    fun `should throw exception for mismatched credential type`() {
        val securitySchemes = listOf(BearerSecurityScheme()) // Expected BearerCredentials
        val credentials = mapOf(
            "https://example.com/device1" to BasicCredentials("user1", "pass1") // Providing BasicCredentials
        )
        val provider = DefaultCredentialsProvider(securitySchemes, credentials)
        val form = Form(href = "https://example.com/device1/status")

        val exception = assertFailsWith<NoCredentialsFound> {
            provider.getCredentials(form)
        }
        assertTrue(exception.message!!.contains("Expected BearerCredentials but found BasicCredentials"))
    }
}