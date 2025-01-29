plugins {
    kotlin("plugin.spring") version "2.1.10"
    id("org.springframework.boot") version "3.1.5" // Use the latest compatible version
    id("io.spring.dependency-management") version "1.1.3"
}

dependencies {
    api(project(":kotlin-wot"))
    api(project(":kotlin-wot-reflection"))
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-logging")
    compileOnly(project(":kotlin-wot-binding-http"))
    compileOnly(project(":kotlin-wot-binding-mqtt"))
    compileOnly(project(":kotlin-wot-binding-websocket"))
}