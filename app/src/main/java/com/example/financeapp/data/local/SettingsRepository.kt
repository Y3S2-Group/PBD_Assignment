package com.example.financeapp.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<SettingsState> = dataStore.data.map { prefs ->
        SettingsState(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            language = prefs[Keys.LANGUAGE] ?: "en",
            biometricsEnabled = prefs[Keys.BIOMETRICS_ENABLED] ?: false,
            localProfilePhotoPath = prefs[Keys.LOCAL_PROFILE_PHOTO_PATH] ?: "",
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRICS_ENABLED] = enabled }
    }

    suspend fun setLocalProfilePhotoPath(path: String) {
        dataStore.edit { it[Keys.LOCAL_PROFILE_PHOTO_PATH] = path }
    }

    suspend fun clearUserScopedSettings() {
        dataStore.edit {
            it.remove(Keys.LOCAL_PROFILE_PHOTO_PATH)
            it.remove(Keys.BIOMETRICS_ENABLED)
        }
    }

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")
        val LOCAL_PROFILE_PHOTO_PATH = stringPreferencesKey("local_profile_photo_path")
    }
}

data class SettingsState(
    val isDarkMode: Boolean,
    val language: String,
    val biometricsEnabled: Boolean,
    val localProfilePhotoPath: String,
)
