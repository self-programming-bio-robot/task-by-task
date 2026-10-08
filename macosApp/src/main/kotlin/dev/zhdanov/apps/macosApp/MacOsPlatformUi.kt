package dev.zhdanov.apps.macosApp

import dev.zhdanov.apps.composeApp.platform.PlatformUi

/**
 * macOS-specific window UI. Scaffold for now — override WindowContent and
 * the undecorated/transparent flags when a custom macOS title bar lands.
 */
object MacOsPlatformUi : PlatformUi
