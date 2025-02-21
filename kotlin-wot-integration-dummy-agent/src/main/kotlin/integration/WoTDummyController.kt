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
@RequestMapping("agent", produces = [MediaType.APPLICATION_JSON_VALUE])
class WoTDummyController {


    @GetMapping("/.well-known/wot")
    fun getThingDescription(): ResponseEntity<String> {
        return ResponseEntity.ok().body(thingDescription)
    }

    @GetMapping("/capabilities")
    fun getCapabilitiesDescription(): ResponseEntity<String> {
        return ResponseEntity.ok().body(capabilitiesDescription)
    }

    private val thingDescription =
        """
            {
              "description" : "A chat agent.",
              "events" : {
                "messageGenerated" : {
                  "title" : "Generated message",
                  "data" : {
                    "type" : "string"
                  },
                  "forms" : [ {
                    "href" : "ws://localhost:8080/ws",
                    "contentType" : "application/json",
                    "subprotocol" : "lmosprotocol",
                    "op" : [ "subscribeevent", "unsubscribeevent" ]
                  } ]
                }
              },
              "links": [{
                "rel": "lmos:Capabilities",
                "href": "/agent/capabilities",
                "type": "application/json"
              }],
              "version" : {
                "instance" : "1.0.0",
                "model" : ""
              },
              "properties" : {
                "modelConfiguration" : {
                  "type" : "object",
                  "forms" : [ {
                    "href" : "ws://localhost:8080/ws",
                    "contentType" : "application/json",
                    "subprotocol" : "lmosprotocol",
                    "op" : [ "readproperty" ]
                  } ],
                  "properties" : {
                    "maxTokens" : {
                      "type" : "integer"
                    },
                    "modelTemperature" : {
                      "type" : "number"
                    }
                  },
                  "required" : [ "maxTokens", "modelTemperature" ],
                  "readOnly" : true
                },
                "observableProperty" : {
                  "type" : "string",
                  "forms" : [ {
                    "href" : "ws://localhost:8080/ws",
                    "contentType" : "application/json",
                    "subprotocol" : "lmosprotocol",
                    "op" : [ "readproperty", "observeproperty", "unobserveproperty" ]
                  } ],
                  "observable" : true,
                  "title" : "Observable Property",
                  "readOnly" : true
                }
              },
              "actions" : {
                "ask" : {
                  "description" : "Ask the agent a question.",
                  "forms" : [ {
                    "href" : "ws://localhost:8080/ws",
                    "contentType" : "application/json",
                    "subprotocol" : "lmosprotocol",
                    "op" : [ "invokeaction" ]
                  } ],
                  "input" : {
                    "type" : "object",
                    "properties" : {
                      "message" : {
                        "type" : "string"
                      }
                    },
                    "required" : [ "message" ]
                  },
                  "output" : {
                    "type" : "string"
                  },
                  "synchronous" : true
                }
              },
              "id" : "chatagent",
              "title" : "Chat Agent",
              "@type" : "lmos:Agent",
              "@context" : [ "https://www.w3.org/2022/wot/td/v1.1", {
                "lmos" : "https://eclipse.dev/lmos/protocol/v1"
              } ]
            }
        """.trimIndent()


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
                  "name": "view-bill",
                  "version": "1.0.0",
                  "description": "Capability to view a bill"
                },
                {
                  "name": "download-bill",
                  "version": "1.1.0",
                  "description": "Capability to download a bill"
                }
              ]
            }

        """.trimIndent()
}
