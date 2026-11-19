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
        versionCode = 3
        versionName = "0.2.1"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("ANDROID_KEYSTORE_PATH")
            val storePass = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("ANDROID_KEY_ALIAS")
            val keyPass = System.getenv("ANDROID_KEY_PASSWORD")

            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
            }
            if (!storePass.isNullOrBlank()) storePassword = storePass
            if (!keyAliasValue.isNullOrBlank()) keyAlias = keyAliasValue
            if (!keyPass.isNullOrBlank()) keyPassword = keyPass
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
