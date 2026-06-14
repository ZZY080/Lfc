package com.lfc.consumer.data

import com.lfc.consumer.data.api.LfcApiService
import com.lfc.consumer.data.local.TokenManager
import com.lfc.consumer.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * accessToken 过期（401）时自动用 refreshToken 换新，失败则清会话并通知跳转登录。
 */
class BearerTokenAuthenticator(
    private val tokenManager: TokenManager,
    private val authApi: LfcApiService,
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null
        if (response.retryCount() >= 2) {
            notifySessionExpired()
            return null
        }

        val path = response.request.url.encodedPath
        if (path.contains("/consumer/auth/login") || path.contains("/consumer/auth/register")) {
            return null
        }
        if (path.contains("/consumer/auth/refresh")) {
            notifySessionExpired()
            return null
        }

        synchronized(refreshLock) {
            val latestToken = runBlocking { tokenManager.getToken() }
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (!latestToken.isNullOrBlank() && requestToken != latestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
            if (refreshToken.isNullOrBlank()) {
                notifySessionExpired()
                return null
            }

            val tokens = try {
                runBlocking {
                    authApi.refreshToken(RefreshTokenRequest(refreshToken))
                }
            } catch (_: Exception) {
                notifySessionExpired()
                return null
            }

            runBlocking {
                tokenManager.updateTokens(tokens.accessToken, tokens.refreshToken)
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${tokens.accessToken}")
                .build()
        }
    }

    private fun notifySessionExpired() {
        runBlocking {
            tokenManager.clearSessionAndNotify()
        }
    }

    private fun Response.retryCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
