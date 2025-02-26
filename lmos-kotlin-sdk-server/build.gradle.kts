dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    api(project(":kotlin-wot"))
    api(project(":lmos-kotlin-sdk-base"))
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("io.ktor:ktor-bom:3.1.0"))
    testImplementation(project(":kotlin-wot-binding-http"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")
    testImplementation("io.ktor:ktor-client-okhttp")
    testImplementation("io.ktor:ktor-client-json")
    testImplementation("io.ktor:ktor-client-jackson")
    testImplementation("io.ktor:ktor-client-serialization")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-jackson")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.register("listConfigurations") {
    doLast {
        configurations.forEach { config ->
            println("Configuration: ${config.name}")
            println("  Can be resolved: ${config.isCanBeResolved}")
            println("  Can be consumed: ${config.isCanBeConsumed}")
            println("  Extends from: ${config.extendsFrom.joinToString { it.name }}")
            println()
        }
    }
}
