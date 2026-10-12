plugins {
    `java`
    `kotlin-dsl`
}

group = "com.mcjava"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://jitpack.io")
}

dependencies {
    // Decompilation tools
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

// Apply decompilation plugin
apply(from = "tools/gradle/setupDecompWorkspace.gradle")

tasks.register("cleanAll", Delete::class) {
    delete(rootProject.buildDir)
    delete(file("minecraft/client/src"))
    delete(file("minecraft/client/src-mapped"))
    delete(file("android/app/build"))
}

tasks.register("fullSetup") {
    dependsOn("cleanAll", "setupDecompWorkspace")
    group = "mcJava"
    description = "Clean and full setup of decompilation workspace"
}
