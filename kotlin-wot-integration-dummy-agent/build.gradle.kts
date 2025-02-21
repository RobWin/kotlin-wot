import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("plugin.spring") version "1.9.10"
    id("org.springframework.boot") version "3.1.5" // Use the latest compatible version
    id("io.spring.dependency-management") version "1.1.3"
    id("com.citi.helm") version "2.2.0"
    id("com.citi.helm-publish") version "2.2.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}

tasks.named<Test>("test") {
    enabled = false
}

tasks.withType<BootJar> {
    mainClass.set("integration.AgentApplicationKt")
}

tasks.named<BootBuildImage>("bootBuildImage") {
    if (project.hasProperty("REGISTRY_URL")) {
        val registryUrl = getProperty("REGISTRY_URL")
        val registryUsername = getProperty("REGISTRY_USERNAME")
        val registryPassword = getProperty("REGISTRY_PASSWORD")
        val registryNamespace = getProperty("REGISTRY_NAMESPACE")

        imageName.set("$registryUrl/$registryNamespace/dummy-agent:0.1.0-SNAPSHOT")
        publish = true
        docker {
            publishRegistry {
                url.set(registryUrl)
                username.set(registryUsername)
                password.set(registryPassword)
            }
        }
    } else {
        imageName.set("${rootProject.name}:${project.version}")
        publish = false
    }
}

helm {
    charts {
        create("main") {
            chartName.set("${project.name}-chart")
            chartVersion.set("${project.version}")
            sourceDir.set(file("src/main/helm"))
        }
    }
}

tasks.register("helmPush") {
    description = "Push Helm chart to OCI registry"
    group = "helm"
    dependsOn(tasks.named("helmPackageMainChart"))

    doLast {
        val registryUrl = getProperty("REGISTRY_URL")
        val registryUsername = getProperty("REGISTRY_USERNAME")
        val registryPassword = getProperty("REGISTRY_PASSWORD")
        val registryNamespace = getProperty("REGISTRY_NAMESPACE")

        helm.execHelm("registry", "login") {
            option("-u", registryUsername)
            option("-p", registryPassword)
            args(registryUrl)
        }

        helm.execHelm("push") {
            args(tasks.named("helmPackageMainChart").get().outputs.files.singleFile.toString())
            args("oci://$registryUrl/$registryNamespace")
        }

        helm.execHelm("registry", "logout") {
            args(registryUrl)
        }
    }
}

fun getProperty(propertyName: String) = System.getenv(propertyName) ?: project.findProperty(propertyName) as String
