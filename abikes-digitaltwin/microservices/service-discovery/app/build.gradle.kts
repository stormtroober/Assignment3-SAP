plugins {
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    java
}

group = "com.eurekaserver.application"
version = "1.0.0-SNAPSHOT"
description = "Eureka Server Application"

java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.3"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    // Spring Boot Actuator (to expose metrics and health endpoints)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Micrometer Prometheus Registry (to enable Prometheus integration)
    implementation("io.micrometer:micrometer-registry-prometheus")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
