plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.byakushield.app"

    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.byakushield.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
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
    }
}

dependencies {

    // ---------------------------------------------------------
    // Jetpack Compose
    // ---------------------------------------------------------

    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    // ---------------------------------------------------------
    // Android Core
    // ---------------------------------------------------------

    implementation(
        libs.androidx.core.ktx
    )


    // ---------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.11.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0"
    )


    // ---------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------

    implementation(
        "androidx.navigation:navigation-compose:2.9.3"
    )


    // ---------------------------------------------------------
    // Retrofit & OkHttp
    // ---------------------------------------------------------

    implementation(
        "com.squareup.retrofit2:retrofit:3.0.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:3.0.0"
    )

    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )


    // ---------------------------------------------------------
    // Encrypted Token Storage
    // ---------------------------------------------------------

    implementation(
        "androidx.security:security-crypto:1.1.0"
    )


    // ---------------------------------------------------------
    // CameraX
    // ---------------------------------------------------------

    implementation(
        "androidx.camera:camera-core:1.4.2"
    )

    implementation(
        "androidx.camera:camera-camera2:1.4.2"
    )

    implementation(
        "androidx.camera:camera-lifecycle:1.4.2"
    )

    implementation(
        "androidx.camera:camera-view:1.4.2"
    )


    // ---------------------------------------------------------
    // Google ML Kit - QR / Barcode Scanning
    // ---------------------------------------------------------

    implementation(
        "com.google.mlkit:barcode-scanning:17.3.0"
    )
    implementation(libs.androidx.tv.material)


    // ---------------------------------------------------------
    // Unit Tests
    // ---------------------------------------------------------

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}