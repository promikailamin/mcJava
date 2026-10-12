plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("fabric-loom") version "1.4-SNAPSHOT" apply false
}

android {
    namespace = "com.mcjava.minecraft"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mcjava.minecraft"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    externalNativeBuild {
        cmake {
            path = "src/main/cpp/CMakeLists.txt"
            version = "3.22.1"
        }
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/*.kotlin_module"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets {
        main {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-graphics:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // OpenGL ES
    implementation("androidx.glance:glance:1.0.0-alpha05")

    // Fabric Loader (will be added via loom)
    // implementation("net.fabricmc:fabric-loader:0.15.11")

    // Minecraft libraries
    implementation("org.lwjgl:lwjgl:3.3.3")
    implementation("org.lwjgl:lwjgl-glfw:3.3.3")
    implementation("org.lwjgl:lwjgl-opengl:3.3.3")
    implementation("org.lwjgl:lwjgl-stb:3.3.3")
    implementation("org.lwjgl:lwjgl-jemalloc:3.3.3")
    implementation("org.lwjgl:lwjgl-tinyfd:3.3.3")

    // Native libraries for Android
    implementation("org.lwjgl:lwjgl:3.3.3:natives-android-arm64")
    implementation("org.lwjgl:lwjgl:3.3.3:natives-android-arm32")
    implementation("org.lwjgl:lwjgl:3.3.3:natives-android-x86")
    implementation("org.lwjgl:lwjgl:3.3.3:natives-android-x64")

    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-android:2.0.9")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }
}
