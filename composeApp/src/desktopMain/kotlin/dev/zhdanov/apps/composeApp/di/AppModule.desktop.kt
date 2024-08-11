package dev.zhdanov.apps.composeApp.di

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
}