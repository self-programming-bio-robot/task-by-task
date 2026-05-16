package dev.zhdanov.apps.composeApp.di

import dev.zhdanov.apps.composeApp.services.DesktopScheduler
import dev.zhdanov.apps.composeApp.services.JvmWorkspaceCryptoService
import dev.zhdanov.apps.composeApp.services.SchedulerService
import dev.zhdanov.apps.composeApp.services.ThemeChangeService
import dev.zhdanov.apps.composeApp.services.ThemeChangeServiceDesktop
import dev.zhdanov.apps.composeApp.services.WorkspaceCryptoService
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import dev.zhdanov.apps.shared.cache.JvmDatabaseDriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single<DatabaseDriverFactory> {
        JvmDatabaseDriverFactory(
            fileName = System.getProperty("user.home") + "/.taskByTask/cache.db"
        )
    }
    single<Database> {
        Database(get())
    }
    single<SchedulerService> {
        DesktopScheduler()
    }
    single<ThemeChangeService> {
        ThemeChangeServiceDesktop()
    }
    single<WorkspaceCryptoService> {
        JvmWorkspaceCryptoService()
    }
}
