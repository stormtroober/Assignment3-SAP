plugins {
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    java
}

group = "com.eurekagateway.application"
version = "1.0.0-SNAPSHOT"
description = "Eureka Gateway Application"

dependencies {
    // Spring Cloud Gateway Starter
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // Spring Cloud Eureka Discovery Client Starter
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Spring Boot Starter
    implementation("org.springframework.boot:spring-boot-starter")

    // Spring Boot Actuator (to expose metrics and health endpoints)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Micrometer Prometheus Registry (to enable Prometheus integration)
    implementation("io.micrometer:micrometer-registry-prometheus")
}

extra["springCloudVersion"] = "2023.0.3"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}