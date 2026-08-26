plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "app.salat.shared"
        compileSdk = 37
        minSdk = 26
    }

    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()
    listOf(iosArm64Target, iosSimulatorArm64Target).forEach { target ->
        target.binaries.framework {
            baseName = "SalatShared"
            isStatic = true
        }
    }

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
