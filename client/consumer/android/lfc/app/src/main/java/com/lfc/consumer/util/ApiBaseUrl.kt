package com.lfc.consumer.util

import android.net.Uri
import android.os.Build
import android.util.Log
import com.lfc.consumer.BuildConfig

object ApiBaseUrl {
    private const val TAG = "ApiBaseUrl"
    /** 模拟器访问宿主机（Mac/PC）的固定地址，与 adb reverse / 局域网 IP 无关 */
    private const val EMULATOR_HOST = "10.0.2.2"

    /**
     * 真机：直接用 BuildConfig（如 192.168.x.x 局域网 IP）。
     * 模拟器：虚拟网卡访问不到 192.168.x.x / 127.0.0.1，统一改走 10.0.2.2（保留端口与路径）。
     */
    fun resolve(): String {
        val configured = normalizeTrailingSlash(BuildConfig.API_BASE_URL)
        if (!BuildConfig.DEBUG || !isEmulator()) {
            return configured
        }
        return rewriteForEmulator(configured)
    }

    private fun rewriteForEmulator(configured: String): String {
        val uri = Uri.parse(configured.trimEnd('/'))
        val host = uri.host
        if (host == null || host == EMULATOR_HOST) {
            return configured
        }
        val port = when {
            uri.port != -1 -> uri.port
            uri.scheme.equals("https", ignoreCase = true) -> 443
            else -> 8000
        }
        val path = uri.path?.takeIf { it.isNotBlank() } ?: "/api/"
        val normalizedPath = normalizeTrailingSlash(path)
        val scheme = uri.scheme ?: "http"
        val resolved = "$scheme://$EMULATOR_HOST:$port$normalizedPath"
        Log.i(TAG, "Emulator: $configured -> $resolved")
        return resolved
    }

    private fun normalizeTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk", ignoreCase = true)
            || Build.MODEL.contains("Emulator", ignoreCase = true)
            || Build.MODEL.contains("Android SDK built for x86", ignoreCase = true)
            || Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)
            || Build.HARDWARE.contains("goldfish", ignoreCase = true)
            || Build.HARDWARE.contains("ranchu", ignoreCase = true)
            || Build.PRODUCT.contains("sdk_gphone", ignoreCase = true)
            || Build.PRODUCT.contains("sdk_google", ignoreCase = true)
    }
}
