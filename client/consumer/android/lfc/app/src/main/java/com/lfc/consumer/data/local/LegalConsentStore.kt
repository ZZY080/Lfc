package com.lfc.consumer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.legalConsentStore: DataStore<Preferences> by preferencesDataStore(name = "legal_consent")

class LegalConsentStore(private val context: Context) {
    companion object {
        /** 协议版本号；更新协议正文后递增，将要求用户重新确认 */
        const val CURRENT_VERSION = 2
    }

    private val agreedVersionKey = intPreferencesKey("agreed_version")

    val agreedVersionFlow: Flow<Int> = context.legalConsentStore.data.map { prefs ->
        prefs[agreedVersionKey] ?: 0
    }

    val hasAgreedFlow: Flow<Boolean> = agreedVersionFlow.map { version ->
        version >= CURRENT_VERSION
    }

    suspend fun getAgreedVersion(): Int = agreedVersionFlow.first()

    suspend fun hasAgreed(): Boolean {
        return hasAgreedFlow.first()
    }

    suspend fun markAgreed() {
        context.legalConsentStore.edit { prefs ->
            prefs[agreedVersionKey] = CURRENT_VERSION
        }
    }
}
