plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)// <--- ESTA LÍNEA
}

android {
    namespace = "com.adsweb.proxismart"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adsweb.proxismart"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    kapt {
        arguments {
            arg("konvert.jvmTarget", "17")
        }
    }
}

dependencies {
    // 1. Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.firebase.firestore)

    // 2. Compose Engine (Nombres corregidos a DOTS)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.foundation)
    implementation(libs.compose.layout)

    // 3. ADSGO Maps & GPS (Punto 13 del PDF)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)

    // 4. ADSGO Data Engine (Persistencia y Red)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.ktor.android)
    implementation(libs.ktor.json)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.coil.compose)
    implementation(libs.guava.android)
    // camerax
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}