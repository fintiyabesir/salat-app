plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    android {
        namespace = "app.salat.shared"
        compileSdk = 37
        minSdk = 26
    }

    iosArm64()
    iosSimulatorArm64()
    jvm("jvm")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.adhan)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
