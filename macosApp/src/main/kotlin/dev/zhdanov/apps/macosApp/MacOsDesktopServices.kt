package dev.zhdanov.apps.macosApp

import dev.zhdanov.apps.composeApp.platform.DesktopOsServices
import dev.zhdanov.apps.composeApp.platform.OsNotificationChannel
import dev.zhdanov.apps.composeApp.platform.PlatformUi
import org.koin.dsl.module

object MacOsDesktopServices : DesktopOsServices {

    override val koinModule = module {
        single<OsNotificationChannel> { MacOsNotificationChannel() }
    }

    override val platformUi: PlatformUi = MacOsPlatformUi
}
