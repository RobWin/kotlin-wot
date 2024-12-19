plugins {
    id("io.ktor.plugin") version "3.0.3"
}

dependencies {
    api(project(":kotlin-wot"))
    implementation("org.slf4j:slf4j-api")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-client-websocket:1.1.4")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-logging")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-serialization-jackson")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(project(":kotlin-wot-binding-http"))
    testImplementation("ch.qos.logback:logback-classic:1.5.12")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.0")
}
