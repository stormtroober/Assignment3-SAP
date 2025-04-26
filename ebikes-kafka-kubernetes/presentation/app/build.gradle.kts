plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
repositories {
    mavenCentral()
}

javafx {
    modules("javafx.controls", "javafx.fxml")
}

dependencies {


    // Vert.x
    implementation(platform("io.vertx:vertx-stack-depchain:4.4.0"))
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-web-client")
    implementation("io.vertx:vertx-service-discovery")
    implementation("io.vertx:vertx-circuit-breaker")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("org.mockito:mockito-core:5.3.1")
}

application {
    mainClass.set("org.Main")
}

tasks.test {
    useJUnitPlatform()
}
