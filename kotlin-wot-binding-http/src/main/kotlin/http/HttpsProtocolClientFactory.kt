/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.thingweb.binding.http

/**
 * Creates new [HttpProtocolClient] instances that allow consuming Things via HTTPS.
 */
class HttpsProtocolClientFactory() : HttpProtocolClientFactory() {

    override fun toString(): String {
        return "HttpsClient"
    }

    override val scheme: String
        get() = "https"
}
