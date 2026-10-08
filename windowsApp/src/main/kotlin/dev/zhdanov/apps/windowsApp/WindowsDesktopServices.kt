package dev.zhdanov.apps.windowsApp

import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.notification.TrayNotificationService
import dev.zhdanov.apps.composeApp.platform.DesktopOsServices
import dev.zhdanov.apps.composeApp.platform.PlatformUi
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

object WindowsDesktopServices : DesktopOsServices {

    override val koinModule = module {
        single {
            WindowsNotificationService(fallback = TrayNotificationService(get()))
        } onClose { it?.close() } bind NotificationService::class
    }

    override val platformUi: PlatformUi = WindowsPlatformUi
}
