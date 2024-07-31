package com.blecast.app.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class PreferencesDataStore(context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferences")
    private val dataStore = context.dataStore

    suspend fun setString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getString(key: Preferences.Key<String>): Flow<String> {
        return dataStore.data
            .map { preferences ->
                preferences[key].orEmpty()
            }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }

    companion object {
        @Volatile
        private var INSTANCE: PreferencesDataStore? = null

        fun getInstance(context: Context): PreferencesDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
