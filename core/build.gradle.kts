
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.proyectorbiblico.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
