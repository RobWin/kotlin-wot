plugins {
    kotlin("plugin.spring") version "2.1.10"
    id("org.springframework.boot") version "3.1.5" // Use the latest compatible version
    id("io.spring.dependency-management") version "1.1.3"
}

dependencies {
    api(project(":kotlin-wot-binding-websocket"))
    api(project(":kotlin-wot-spring-boot-starter"))
    api(project(":kotlin-wot-lmos-protocol"))
    implementation("org.jsoup:jsoup:1.7.2")
}