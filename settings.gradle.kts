pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "kotlin-wot"
include("kotlin-wot")
include("kotlin-wot-binding-http")
include("kotlin-wot-binding-mqtt")
include("kotlin-wot-binding-websocket")
include("kotlin-wot-integration-tests")
include("kotlin-wot-integration-dummy-agent")
include("kotlin-wot-integration-wot-agent")
include("kotlin-wot-reflection")
include("kotlin-wot-spring-boot-starter")
include("kotlin-wot-tool-example")
include("kotlin-wot-lmos-protocol")