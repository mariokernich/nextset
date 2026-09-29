import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing with the upload key: android/keystore.properties locally
// (see README) or the NEXTSET_* environment variables in CI. Without them,
// release builds stay unsigned.
val keystore = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(key: String, environment: String): String? =
    keystore.getProperty(key) ?: providers.environmentVariable(environment).orNull

android {
    namespace = "com.mariokernich.nextset"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mariokernich.nextset"
        minSdk = 29
        targetSdk = 36
        versionCode = providers.gradleProperty("nextset.versionCode").get().toInt() * 10
        versionName = providers.gradleProperty("nextset.versionName").get()
    }

    val releaseSigning = signingValue("storeFile", "NEXTSET_KEYSTORE_FILE")?.let { path ->
        signingConfigs.create("release") {
            storeFile = rootProject.file(path)
            storePassword = signingValue("storePassword", "NEXTSET_KEYSTORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "NEXTSET_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "NEXTSET_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = releaseSigning
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // English and German, and the per-app language setting of Android 13+.
        localeFilters += listOf("en", "de")
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
