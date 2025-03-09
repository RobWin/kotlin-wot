dependencies {
    //implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.20")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Tracing
    api(platform("io.opentelemetry:opentelemetry-bom:1.47.0"))
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.13.1")
    api("io.opentelemetry:opentelemetry-extension-kotlin")
    api("io.opentelemetry:opentelemetry-api")

    implementation("org.slf4j:slf4j-api:2.0.16")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.4.1")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("app.cash.turbine:turbine:1.2.0")
}