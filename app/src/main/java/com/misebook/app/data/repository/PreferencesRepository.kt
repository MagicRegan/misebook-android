package com.misebook.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "misebook_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val CURRENT_WORKSPACE = stringPreferencesKey("current_workspace_id")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val currentWorkspaceId: Flow<String?> = context.dataStore.data.map { it[Keys.CURRENT_WORKSPACE] }
    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    suspend fun setCurrentWorkspace(id: String) {
        context.dataStore.edit { it[Keys.CURRENT_WORKSPACE] = id }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDED] = value }
    }
}
