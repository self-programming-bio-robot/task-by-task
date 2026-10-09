import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(projects.composeApp)
    implementation(compose.desktop.currentOs)

    implementation(libs.nucleus.notification.windows)
    implementation(libs.jbr.api)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.core)
    implementation(libs.logging)
}

// The merged title bar (WindowsTitleBar.kt) needs JetBrains Runtime, so run and
// package the app on JBR. Resolved only on Windows hosts (the only place this
// app is run or packaged) to keep other hosts from provisioning it; on any
// other runtime the app falls back to the standard system title bar.
val jbrLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
    vendor = JvmVendorSpec.JETBRAINS
}
val isWindowsHost = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

compose.desktop {
    application {
        mainClass = "dev.zhdanov.apps.windowsApp.MainKt"
        if (isWindowsHost) {
            javaHome = jbrLauncher.get().metadata.installationPath.asFile.absolutePath
        }

        nativeDistributions {
            val rawAppVersion = (project.findProperty("appVersion") as? String) ?: System.getenv("APP_VERSION")

            targetFormats(TargetFormat.Msi)
            packageName = "TaskByTask"
            packageVersion = sanitizeVersion(rawAppVersion)

            modules("java.sql", "java.naming")
        }
    }
}

fun sanitizeVersion(version: String?): String {
    if (version.isNullOrBlank()) {
        return "0.0.0"
    }
    val trimmed = version.trim()
    val regex = Regex("^v?\\d+\\.\\d+\\.\\d+$", RegexOption.IGNORE_CASE)
    if (!regex.matches(trimmed)) {
        error("Invalid app version '$version'. Version must be in format 'x.x.x' or 'vx.x.x' (e.g. '1.0.0' or 'v1.0.0').")
    }
    return trimmed.removePrefix("v").removePrefix("V")
}
