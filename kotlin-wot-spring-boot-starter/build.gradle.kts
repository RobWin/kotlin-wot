plugins {
    kotlin("plugin.spring") version "1.9.10"
    id("org.springframework.boot") version "3.1.5" // Use the latest compatible version
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    api(project(":kotlin-wot"))
    implementation(project(":kotlin-wot-reflection"))
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-logging")
    compileOnly(project(":kotlin-wot-binding-http"))
    compileOnly(project(":kotlin-wot-binding-mqtt"))
}