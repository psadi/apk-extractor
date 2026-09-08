package com.psadi.apkextractor.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.psadi.apkextractor.data.model.AppCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "apk_extractor_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_CUSTOM_FOLDER_URI = stringPreferencesKey("custom_folder_uri")
        private val KEY_SELECTED_CATEGORY = stringPreferencesKey("selected_category")
    }

    val customFolderUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_FOLDER_URI]
    }

    val selectedCategory: Flow<AppCategory> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_SELECTED_CATEGORY]) {
            AppCategory.USER.name -> AppCategory.USER
            AppCategory.SYSTEM.name -> AppCategory.SYSTEM
            AppCategory.EXTRACTED.name -> AppCategory.EXTRACTED
            else -> AppCategory.ALL
        }
    }

    suspend fun setCustomFolderUri(uriString: String?) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[KEY_CUSTOM_FOLDER_URI] = uriString
            } else {
                preferences.remove(KEY_CUSTOM_FOLDER_URI)
            }
        }
    }

    suspend fun setSelectedCategory(category: AppCategory) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_CATEGORY] = category.name
        }
    }
}
