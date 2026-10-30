plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mm2vault.vyra"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mm2vault.vyra"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    val signingReady = listOf("ANDROID_KEYSTORE_PATH", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD").all { !System.getenv(it).isNullOrBlank() }
    signingConfigs {
        if (signingReady) {
            create("release") {
                storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH")!!)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (signingReady) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Media3 playback foundation. Keep the version aligned across all Media3 artifacts.
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-session:1.10.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.10.1")
    implementation("com.google.guava:guava:33.6.0-android")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
