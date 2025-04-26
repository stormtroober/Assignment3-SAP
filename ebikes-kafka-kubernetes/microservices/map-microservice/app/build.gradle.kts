plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    java
    application
    id("com.diffplug.spotless") version "6.25.0"
}

tasks.test {
    useJUnitPlatform()
}

java {
    // Use Java 21.
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.7.1")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.12")

    implementation("io.vertx:vertx-core:4.5.3")
    implementation("io.vertx:vertx-web:4.4.0")
    implementation("io.vertx:vertx-web-client:4.4.0")
    implementation("io.vertx:vertx-config:4.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    // Add Micrometer and Prometheus dependencies
    implementation("io.micrometer:micrometer-core:1.12.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")
    implementation("io.vertx:vertx-micrometer-metrics:4.4.0")
    testImplementation("io.vertx:vertx-junit5:4.4.0")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "Main"
}

tasks.jar {
    archiveFileName.set("app.jar")
    manifest {
        attributes["Main-Class"] = application.mainClass.get() // or specify your main class directly
    }

    // Include all runtime dependencies into the JAR file
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })

    // Optionally, include your compiled classes (if not already included by default)
    from(sourceSets.main.get().output)

    // Ensure the JAR is built as a single fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

spotless {
    java {
        googleJavaFormat()
        target("src/**/*.java")
        targetExclude("**/build/**")
    }
}