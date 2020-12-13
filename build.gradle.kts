import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven {
            name = "KotDis"
            url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
        }
    }
}

plugins {
    application
    `maven-publish`

    kotlin("jvm") version "1.4.10"

    id("com.github.jakemarsden.git-hooks") version "0.0.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.8.0"
}

group = "com.kotlindiscord.bot"
version = "1.0-SNAPSHOT"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.11"

    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()

    maven {
        name = "KotDis"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.8.0")

    implementation("com.kotlindiscord.api:client:+")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.1-SNAPSHOT")

    implementation("com.uchuhimo:konf:0.22.1")
    implementation("com.uchuhimo:konf-toml:0.22.1")

    implementation("org.apache.commons:commons-text:1.8")
    implementation("org.nibor.autolink:autolink:0.10.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    // Logging dependencies
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("io.sentry:sentry-logback:1.7.30")
    implementation("org.codehaus.groovy:groovy:3.0.4")  // For logback config
}

application {
    mainClassName = "com.kotlindiscord.bot.KDBotKt"
}

detekt {
    buildUponDefaultConfig = true
    config = files("detekt.yml")
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.kotlindiscord.bot.KDBotKt"
        )
    }
}

tasks.processResources {
    dependsOn("generateBuildInfo")

    from("src/main/resources/build.properties")
}

val generateBuildInfo = task("generateBuildInfo") {
    outputs.file("src/main/resources/build.properties")

    doLast {
        File("src/main/resources/build.properties").writeText(
            "version=$project.version"
        )
    }
}
