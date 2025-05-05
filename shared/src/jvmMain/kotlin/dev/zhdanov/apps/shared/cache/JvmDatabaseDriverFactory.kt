package dev.zhdanov.apps.shared.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.diamondedge.logging.logging
import java.io.File

class JvmDatabaseDriverFactory(
    private val fileName: String = ""
): DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        try {
            File(fileName).parentFile.mkdirs()
            logger.i { "Creating database driver in file: $fileName" }
            return JdbcSqliteDriver("jdbc:sqlite:$fileName")
        } catch (e: Exception) {
            logger.e(e) { "Database creation failed ${e.message}" }
            throw RuntimeException("Database creation failed", e)
        }
    }

    companion object {
        val logger = logging(JvmDatabaseDriverFactory::class.qualifiedName)
    }
}
