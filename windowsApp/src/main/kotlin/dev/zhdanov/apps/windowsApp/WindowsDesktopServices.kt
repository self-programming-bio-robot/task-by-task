package dev.zhdanov.apps.windowsApp

import dev.zhdanov.apps.composeApp.platform.DesktopOsServices
import dev.zhdanov.apps.composeApp.platform.OsNotificationChannel
import dev.zhdanov.apps.composeApp.platform.PlatformUi
import org.koin.dsl.module

object WindowsDesktopServices : DesktopOsServices {

    override val koinModule = module {
        single<OsNotificationChannel> { WindowsNotificationChannel() }
    }

    override val platformUi: PlatformUi = WindowsPlatformUi
}
