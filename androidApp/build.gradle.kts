plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "app.salat.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.salat.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.file("resources"))
        }
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
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.datetime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.google.play.services.wearable)
    debugImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
