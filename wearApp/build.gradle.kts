plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.salat.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.salat.mobile"
        minSdk = 30
        targetSdk = 36
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
    }

    signingConfigs {
        create("release") {
            // Only populated when the release secrets are present. Local builds and
            // pull-request CI have no keystore and must still be able to assemble.
            val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        getByName("release") {
            // R8 stays off until a shrunk build has been smoke-tested on a device.
            // Turning it on untested is how a release ships with a broken screen.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release").takeIf {
                !providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull.isNullOrBlank()
            }
        }
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.watchface.complications.data.source)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.google.play.services.wearable)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}
