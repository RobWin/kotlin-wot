dependencies {
    api(project(":kotlin-wot"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("com.hivemq:hivemq-mqtt-client:1.3.3")
    testImplementation("ch.qos.logback:logback-classic:1.5.12")
    testImplementation("app.cash.turbine:turbine:1.2.0")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
}
