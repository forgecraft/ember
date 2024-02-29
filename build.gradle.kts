import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    id("java")
    id("application")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.3"
}

val javaVersion = 21

group = "net.forgecraft.services"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.javacord:javacord:3.8.0")

    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.22.1")
    implementation("ch.qos.logback:logback-classic:1.5.0")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    // Utils
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.jetbrains:annotations:24.1.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

// Runs for intelij
idea.project.settings {
    runConfigurations {
        register("Start Ember", Application::class) {
            mainClass = "net.forgecraft.services.ember.Main"
            programParameters = "--config=config.json"
            moduleName = "ember.main"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
