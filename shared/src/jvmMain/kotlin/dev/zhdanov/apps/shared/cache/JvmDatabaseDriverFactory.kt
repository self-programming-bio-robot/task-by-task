package dev.zhdanov.apps.shared.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class JvmDatabaseDriverFactory(
    private val fileName: String = ""
): DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        File(fileName).parentFile.mkdirs()
        return JdbcSqliteDriver("jdbc:sqlite:$fileName")
    }
}