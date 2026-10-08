package dev.zhdanov.apps.composeApp.platform

import org.koin.core.module.Module

/**
 * Everything an OS application module contributes to the shared desktop app.
 * Each module (windowsApp, macosApp, linuxApp) instantiates its implementation
 * directly in its own main() — no runtime OS detection.
 */
interface DesktopOsServices {

    /** Koin bindings for OS-specific services (NotificationService, ...). */
    val koinModule: Module

    val platformUi: PlatformUi
}
