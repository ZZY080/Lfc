package com.lfc.consumer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "lfc_search")

class SearchHistoryStore(private val context: Context) {
    private val historyKey = stringPreferencesKey("search_history")

    val historyFlow: Flow<List<String>> = context.searchDataStore.data.map { prefs ->
        prefs[historyKey]
            ?.split(HISTORY_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun add(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        context.searchDataStore.edit { prefs ->
            val current = prefs[historyKey]
                ?.split(HISTORY_DELIMITER)
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: mutableListOf()
            current.remove(trimmed)
            current.add(0, trimmed)
            prefs[historyKey] = current.take(MAX_HISTORY).joinToString(HISTORY_DELIMITER)
        }
    }

    suspend fun clear() {
        context.searchDataStore.edit { prefs ->
            prefs.remove(historyKey)
        }
    }

    companion object {
        private const val HISTORY_DELIMITER = "\u0001"
        private const val MAX_HISTORY = 10
    }
}
