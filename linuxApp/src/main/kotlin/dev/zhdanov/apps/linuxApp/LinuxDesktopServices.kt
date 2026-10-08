package dev.zhdanov.apps.linuxApp

import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.notification.TrayNotificationService
import dev.zhdanov.apps.composeApp.platform.DesktopOsServices
import dev.zhdanov.apps.composeApp.platform.PlatformUi
import org.koin.dsl.module

object LinuxDesktopServices : DesktopOsServices {

    override val koinModule = module {
        single<NotificationService> { TrayNotificationService(get()) }
    }

    override val platformUi: PlatformUi = LinuxPlatformUi
}
