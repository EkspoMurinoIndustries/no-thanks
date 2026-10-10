import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "com.expo"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val artworkTools = layout.projectDirectory.dir("tools/card-artwork")
val generatedArtwork = layout.buildDirectory.dir("generated/card-artwork")
val installArtworkTools by tasks.registering(Exec::class) {
    workingDir(artworkTools)
    inputs.files(artworkTools.file("package.json"), artworkTools.file("package-lock.json"))
    outputs.dir(artworkTools.dir("node_modules"))
    if (System.getProperty("os.name").startsWith("Windows")) {
        commandLine("cmd", "/c", "npm.cmd", "ci", "--no-audit", "--no-fund")
    } else {
        commandLine("npm", "ci", "--no-audit", "--no-fund")
    }
}
val generateCardArtwork by tasks.registering(Exec::class) {
    dependsOn(installArtworkTools)
    inputs.dir("src/main/resources/static/img/cards")
    inputs.files(artworkTools.file("generate.cjs"), artworkTools.file("package-lock.json"))
    outputs.dir(generatedArtwork)
    commandLine("node", artworkTools.file("generate.cjs").asFile.absolutePath)
}

tasks.processResources {
    dependsOn(generateCardArtwork)
    exclude("static/img/cards/*.png")
    from(generatedArtwork)
    // Remove PNGs left in the output by builds predating this pipeline.
    doFirst {
        delete(fileTree(destinationDir.resolve("static/img/cards")) { include("*.png") })
    }
}

tasks {
    bootJar {
        archiveFileName.set("${project.name}.jar")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
