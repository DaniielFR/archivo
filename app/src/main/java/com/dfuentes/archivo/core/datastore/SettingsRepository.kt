package com.dfuentes.archivo.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dfuentes.archivo.core.designsystem.theme.ThemeMode
import com.dfuentes.archivo.core.model.LibraryLayout
import com.dfuentes.archivo.core.model.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/** Preferencias de usuario. Todo lo que aquí falte se pierde al cerrar la app. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val sort: SortOrder = SortOrder.RECENT,
    val autoBackupEnabled: Boolean = false,
    /** URI de árbol SAF con permiso persistido. Null = sin carpeta elegida. */
    val backupFolderUri: String? = null,
    val lastBackupAt: Long? = null,
)

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val LAYOUT = stringPreferencesKey("library_layout")
        val SORT = stringPreferencesKey("library_sort")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val BACKUP_FOLDER = stringPreferencesKey("backup_folder")
        val LAST_BACKUP = longPreferencesKey("last_backup_at")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            // Lecturas defensivas: si una preferencia guardada ya no existe en el
            // enum (porque se renombró), volvemos al valor por defecto en vez de
            // hacer crashear la app al arrancar.
            themeMode = prefs[Keys.THEME].toEnum(ThemeMode.SYSTEM),
            dynamicColor = prefs[Keys.DYNAMIC] ?: true,
            layout = prefs[Keys.LAYOUT].toEnum(LibraryLayout.GRID),
            sort = prefs[Keys.SORT].toEnum(SortOrder.RECENT),
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: false,
            backupFolderUri = prefs[Keys.BACKUP_FOLDER],
            lastBackupAt = prefs[Keys.LAST_BACKUP],
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME, mode.name)

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC] = enabled }
    }

    suspend fun setLayout(layout: LibraryLayout) = put(Keys.LAYOUT, layout.name)

    suspend fun setSort(sort: SortOrder) = put(Keys.SORT, sort.name)

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
    }

    suspend fun setBackupFolder(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.BACKUP_FOLDER) else prefs[Keys.BACKUP_FOLDER] = uri
        }
    }

    suspend fun setLastBackupAt(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP] = millis }
    }

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

