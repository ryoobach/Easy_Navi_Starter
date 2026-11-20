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

// 저장소 루트의 확정 원본 이미지를 Android drawable 리소스로 복사합니다.
val generatedBrandResDir = layout.buildDirectory.dir("generated/res/brandAssets")
val copyBrandAssets by tasks.registering(Copy::class) {
    from(rootProject.file("app_icon.png")) {
        rename { "easy_navi_launcher.png" }
    }
    from(rootProject.file("js_logo.png")) {
        rename { "js_logo.png" }
    }
    into(generatedBrandResDir.map { it.dir("drawable-nodpi") })
}

android {
    namespace = "com.ryoobach.easynavistarter"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ryoobach.easynavistarter"
        minSdk = 26
        targetSdk = 35
        versionCode = 23
        versionName = "0.4.1"
    }
    sourceSets.getByName("main").res.srcDir(generatedBrandResDir)

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
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

tasks.named("preBuild").configure {
    dependsOn(copyBrandAssets)
}
