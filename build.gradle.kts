import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    java
    application
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.3"
    id("org.jooq.jooq-codegen-gradle") version "3.19.5"
}

val javaVersion = 21

group = "net.forgecraft.services"
version = "1.0.0"

sourceSets {
    create("generated")
    create("database")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.javacord:javacord:3.8.0")

    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.22.1")
    sourceSets.getByName("database").implementationConfigurationName("org.apache.logging.log4j:log4j-to-slf4j:2.22.1")

    runtimeOnly("ch.qos.logback:logback-classic:1.5.0")
    sourceSets.getByName("database").runtimeOnlyConfigurationName("ch.qos.logback:logback-classic:1.5.0")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.16.1")

    implementation("org.jooq:jooq:3.19.5")
    sourceSets.getByName("database").implementationConfigurationName("org.jooq:jooq:3.19.5")
    sourceSets.getByName("generated").implementationConfigurationName("org.jooq:jooq:3.19.5")

    runtimeOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    sourceSets.getByName("database").runtimeOnlyConfigurationName("org.xerial:sqlite-jdbc:3.45.1.0")
    jooqCodegen("org.xerial:sqlite-jdbc:3.45.1.0")

    // Utils
    implementation("commons-io:commons-io:2.15.1")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("it.unimi.dsi:fastutil:8.5.13")

    implementation("info.picocli:picocli:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")

    compileOnly("org.jetbrains:annotations:24.1.0")
    sourceSets.getByName("database").compileOnlyConfigurationName("org.jetbrains:annotations:24.1.0")

    implementation(sourceSets.getByName("database").output)
    implementation(sourceSets.getByName("generated").output)
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.named("compileJava", JavaCompile::class).configure {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

jooq {
    version = "3.19.5"

    configuration {

        jdbc {
            driver = "org.sqlite.JDBC"
            url = "jdbc:sqlite:${project.file("run/data/sqlite.db").absolutePath}"
        }

        generator {
            database {
                name = "org.jooq.meta.sqlite.SQLiteDatabase"
                includes = ".*"

                // exclude the migrations table from schema generation
                excludes = """
                    __migrations
                """.trimIndent()
            }

            target {
                packageName = "net.forgecraft.services.ember.db.schema"
                directory = project.file("src/generated/java").absolutePath
            }
        }
    }

    project.file("run/data").mkdirs()
}

idea.project.settings {
    runConfigurations {
        register("Start Ember", Application::class) {
            project.file("run").mkdirs()
            mainClass = "net.forgecraft.services.ember.Main"
            programParameters = "--config=config.json"
            moduleName = "${project.name}.main"
            workingDirectory = project.file("run").absolutePath
        }

        register("Bootstrap Database", Application::class) {
            project.file("run").mkdirs()
            mainClass = "net.forgecraft.services.ember.db.Main"
            moduleName = "${project.name}.database"
            workingDirectory = project.file("run").absolutePath
        }
    }
}

// configure the java application plugin for CI and anyone not using IDEA
application {
    mainClass.set("net.forgecraft.services.ember.Main")
    executableDir = project.file("run").absolutePath
}

tasks.named("run", JavaExec::class) {
    args("--config=config.json")
}

tasks.register("boostrapDatabase", JavaExec::class) {
    group = "application"
    mainClass.set("net.forgecraft.services.ember.db.Main")
    classpath = sourceSets.getByName("database").runtimeClasspath
    workingDir = project.file("run")
}
tasks.getByName("jooqCodegen").dependsOn("boostrapDatabase")

tasks.test {
    useJUnitPlatform()
}
