plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dynamicislandpro.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dynamicislandpro.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Redmi 12 (Helio G88) adalah ARM64
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // APK pribadi (sideload): ditandatangani dengan kunci debug agar langsung bisa dipasang.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // Lint tidak boleh menggagalkan build APK; laporan tetap dibuat.
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Lifecycle / MVVM
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")

    // Coroutines + StateFlow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Animasi spring
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")

    testImplementation("junit:junit:4.13.2")
}
