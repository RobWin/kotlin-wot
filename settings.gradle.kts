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
include("kotlin-wot-reflection")
include("kotlin-wot-spring-boot-starter")
include("kotlin-wot-tool-example")
include("kotlin-wot-lmos-protocol")
include("lmos-kotlin-sdk-base")
include("lmos-kotlin-sdk-client")
include("lmos-kotlin-sdk-server")