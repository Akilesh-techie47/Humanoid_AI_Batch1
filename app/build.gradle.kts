import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
val groqApiKey = localProperties.getProperty("GROQ_API_KEY") ?: ""
val openrouterApiKey = localProperties.getProperty("OPENROUTER_API_KEY") ?: ""
val cerebrasApiKey = localProperties.getProperty("CEREBRAS_API_KEY") ?: ""
val mistralApiKey = localProperties.getProperty("MISTRAL_API_KEY") ?: ""
val nvidiaApiKey = localProperties.getProperty("NVIDIA_API_KEY") ?: ""

android {
    namespace = "com.humanoidai"
    compileSdk = 35
    ndkVersion = "30.0.14904198-beta1"

    defaultConfig {
        applicationId = "com.humanoidai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"$geminiApiKey\""
        )
        buildConfigField(
            "String",
            "GROQ_API_KEY",
            "\"$groqApiKey\""
        )
        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"$openrouterApiKey\""
        )
        buildConfigField(
            "String",
            "CEREBRAS_API_KEY",
            "\"$cerebrasApiKey\""
        )
        buildConfigField(
            "String",
            "MISTRAL_API_KEY",
            "\"$mistralApiKey\""
        )
        buildConfigField(
            "String",
            "NVIDIA_API_KEY",
            "\"$nvidiaApiKey\""
        )
        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_shared")
            }
        }
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    androidResources {
        noCompress += "tflite"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "humanoidai123"
            keyAlias = "releaseKey"
            keyPassword = "humanoidai123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    configurations.all {
        resolutionStrategy {
            force("org.tensorflow:tensorflow-lite:2.16.1")
            force("org.tensorflow:tensorflow-lite-api:2.16.1")
            force("org.tensorflow:tensorflow-lite-support:0.4.4")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.auth)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // CameraX
    val cameraVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // ML integration
    implementation(libs.mlkit.face.detection)
    implementation(libs.tensorflow.lite)

    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.generativeai)
    implementation(project(":opencv"))

    // Room DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Jetpack Security (AES-256 key management)
        implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // SQLCipher (encrypted database)
        implementation("net.zetetic:android-database-sqlcipher:4.5.4")
        implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Gson (JSON serialisation for context snapshots)
        implementation("com.google.code.gson:gson:2.10.1")

    // WorkManager (background pattern analysis)
        implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore (UI Customization persistence)
        implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
