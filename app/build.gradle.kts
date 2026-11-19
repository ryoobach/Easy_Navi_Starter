plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ryoobach.easynavistarter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ryoobach.easynavistarter"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.2.4"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("ANDROID_KEYSTORE_PATH")
                ?: error("Persistent release keystore is required. Configure GitHub Actions secrets.")
            storeFile = file(storePath)
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                ?: error("ANDROID_KEYSTORE_PASSWORD is missing.")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                ?: error("ANDROID_KEY_ALIAS is missing.")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                ?: error("ANDROID_KEY_PASSWORD is missing.")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
