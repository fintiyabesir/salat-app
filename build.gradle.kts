import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.screenshot) apply false
}

// Marketing version comes from one file so Android, Wear and the Apple build cannot drift.
val appVersionName: String = Properties().apply {
    rootProject.file("version.properties").inputStream().use(::load)
}.getProperty("versionName") ?: error("versionName missing from version.properties")

// The build number is the commit count. It is monotonic, stored nowhere, and unaffected
// by a workflow rename that would reset a CI run counter. It needs full history, so CI
// must check out with fetch-depth: 0 — a shallow clone would collapse this to 1.
val appVersionCode: Int = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput.asText.get().trim().toInt()
}.getOrElse { 1 }

extra["appVersionName"] = appVersionName
extra["appVersionCode"] = appVersionCode
