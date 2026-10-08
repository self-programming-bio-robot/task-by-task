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

    implementation(libs.koin.core)
    implementation(libs.logging)
}

compose.desktop {
    application {
        mainClass = "dev.zhdanov.apps.macosApp.MainKt"

        nativeDistributions {
            val rawAppVersion = (project.findProperty("appVersion") as? String) ?: System.getenv("APP_VERSION")

            targetFormats(TargetFormat.Dmg)
            packageName = "TaskByTask"
            packageVersion = sanitizeVersion(rawAppVersion)

            jvmArgs += "-Dapple.awt.application.appearance=system"

            modules("java.sql", "java.naming")

            macOS {
                dockName = "Task By Task"
            }
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
