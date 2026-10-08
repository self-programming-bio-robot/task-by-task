package dev.zhdanov.apps.linuxApp

import dev.zhdanov.apps.composeApp.platform.DesktopOsServices
import dev.zhdanov.apps.composeApp.platform.OsNotificationChannel
import dev.zhdanov.apps.composeApp.platform.PlatformUi
import org.koin.dsl.module

object LinuxDesktopServices : DesktopOsServices {

    override val koinModule = module {
        single<OsNotificationChannel> { LinuxNotificationChannel() }
    }

    override val platformUi: PlatformUi = LinuxPlatformUi
}
