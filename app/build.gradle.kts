plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseStorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = !releaseStorePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

val persistentDebugKeystore = file(System.getProperty("user.home") + "/.android/debug.keystore")

android {
    namespace = "com.ryoobach.easynavistarter"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ryoobach.easynavistarter"
        minSdk = 26
        targetSdk = 35
        versionCode = 36
        versionName = "0.5.4"
    }
    signingConfigs {
        create("persistentDebug") {
            storeFile = persistentDebugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("persistentDebug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("persistentDebug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
