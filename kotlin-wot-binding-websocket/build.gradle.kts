plugins {
    id("io.ktor.plugin") version "3.0.1"
}

dependencies {
    api(project(":kotlin-wot"))
    //implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.slf4j:slf4j-api")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-jackson")
    implementation("io.ktor:ktor-server-auto-head-response")
    //implementation("io.insert-koin:koin-ktor:4.0.0")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("ch.qos.logback:logback-classic:1.5.12")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
}