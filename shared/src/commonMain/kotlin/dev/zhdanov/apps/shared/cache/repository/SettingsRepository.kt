package dev.zhdanov.apps.shared.cache.repository

import dev.zhdanov.apps.shared.cache.AppDatabaseQueries
import dev.zhdanov.apps.shared.model.SettingKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(
    val db: AppDatabaseQueries,
    val json: Json = Json
) {
    inline fun <reified T> getSetting(key: SettingKey): T? {
        val data = db.selectSetting(key.id).executeAsOneOrNull()?.data_ ?: return null
        return json.decodeFromString(data)
    }

    inline fun <reified T> saveSetting(key: SettingKey, value: T) {
        val data = json.encodeToString(value)
        db.insertOrReplaceSetting(key.id, data)
    }

    fun deleteSetting(key: SettingKey) {
        db.deleteSetting(key.id)
    }
}
