/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package integration

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("lmos/chatagent", produces = [MediaType.APPLICATION_JSON_VALUE])
class WoTDummyController {

    @GetMapping("/capabilities")
    fun getCapabilitiesDescription(): ResponseEntity<String> {
        return ResponseEntity.ok().body(capabilitiesDescription)
    }

    private val capabilitiesDescription =
        """
            {
              "supportedTenants": [
                "acme"
              ],
              "supportedChannels": [
                "web",
                "ivr"
              ],
              "providedCapabilities": [
                {
                  "name": "chat",
                  "version": "1.0.0",
                  "description": "Capability to chat"
                },
                {
                  "name": "ask-question",
                  "version": "1.0.0",
                  "description": "Capability to ask questions"
                }
              ]
            }

        """.trimIndent()
}
