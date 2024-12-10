plugins {
    kotlin("plugin.spring") version "1.9.10"
    id("org.springframework.boot") version "3.1.5" // Use the latest compatible version
    id("io.spring.dependency-management") version "1.1.3"
}

dependencies {
    api(project(":kotlin-wot"))
    api(project(":kotlin-wot-binding-http"))
    api(project(":kotlin-wot-binding-mqtt"))
    api(project(":kotlin-wot-reflection"))
    api(project(":kotlin-wot-spring-boot-starter"))
    implementation("ai.ancf.lmos:arc-azure-client:0.111.0")
    api("ai.ancf.lmos:arc-spring-boot-starter:0.119.0")

    //implementation("dev.langchain4j:langchain4j-azure-open-ai:0.35.0")
    //implementation("dev.langchain4j:langchain4j:0.35.0")
    testImplementation("com.hivemq:hivemq-mqtt-client:1.3.3")
    implementation("org.testcontainers:testcontainers:1.20.3")
}