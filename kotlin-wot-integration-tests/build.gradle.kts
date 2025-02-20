import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("plugin.spring") version "1.9.10"
    id("org.springframework.boot") version "3.1.5" // Use the latest compatible version
    id("io.spring.dependency-management") version "1.1.3"
}

tasks.named<Test>("test") {
    enabled = false
}

dependencies {
    api(project(":kotlin-wot-binding-http"))
    api(project(":kotlin-wot-binding-websocket"))
    api(project(":kotlin-wot-binding-mqtt"))
    api(project(":kotlin-wot-spring-boot-starter"))
    api(project(":kotlin-wot-lmos-protocol"))
    implementation("ai.ancf.lmos:arc-azure-client:0.111.0")
    api("ai.ancf.lmos:arc-spring-boot-starter:0.111.0")

    //implementation("dev.langchain4j:langchain4j-azure-open-ai:0.35.0")
    //implementation("dev.langchain4j:langchain4j:0.35.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.hivemq:hivemq-mqtt-client:1.3.3")
    implementation("org.testcontainers:testcontainers:1.20.5")
}

tasks.withType<BootJar> {
    mainClass.set("integration.AgentApplication")
}
