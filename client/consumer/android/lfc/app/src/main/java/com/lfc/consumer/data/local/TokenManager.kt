package com.lfc.consumer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class UserSession(
    val userId: Int,
    val email: String,
    val studentId: String,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lfc_prefs")

class TokenManager(private val context: Context) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val emailKey = stringPreferencesKey("user_email")
    private val studentIdKey = stringPreferencesKey("student_id")

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { !it[tokenKey].isNullOrBlank() }

    val userSessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        if (prefs[tokenKey].isNullOrBlank()) {
            null
        } else {
            UserSession(
                userId = prefs[userIdKey]?.toIntOrNull() ?: 0,
                email = prefs[emailKey].orEmpty(),
                studentId = prefs[studentIdKey].orEmpty(),
            )
        }
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Int,
        email: String,
        studentId: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
            prefs[userIdKey] = userId.toString()
            prefs[emailKey] = email
            prefs[studentIdKey] = studentId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { it[refreshTokenKey] }.first()
    }
}
