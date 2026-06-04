package com.lfc.consumer.payment

import android.app.Activity
import com.alipay.sdk.app.AuthTask
import com.alipay.sdk.app.PayTask
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AlipayPayResult(
    val success: Boolean,
    val resultStatus: String,
    val memo: String,
)

data class AlipayAuthResult(
    val success: Boolean,
    val resultStatus: String,
    val authCode: String?,
    val memo: String,
)

object AlipayHelper {
    suspend fun pay(activity: Activity, orderStr: String): AlipayPayResult {
        return try {
            withContext(Dispatchers.IO) {
                val raw = PayTask(activity).payV2(orderStr, true)
                val status = raw["resultStatus"].orEmpty()
                AlipayPayResult(
                    success = status == "9000",
                    resultStatus = status,
                    memo = raw["memo"].orEmpty(),
                )
            }
        } catch (e: Exception) {
            AlipayPayResult(
                success = false,
                resultStatus = "ERROR",
                memo = e.message.orEmpty(),
            )
        }
    }

    suspend fun auth(activity: Activity, authInfo: String): AlipayAuthResult {
        return try {
            withContext(Dispatchers.IO) {
                val raw = AuthTask(activity).authV2(authInfo, true)
                val status = raw["resultStatus"].orEmpty()
                val result = raw["result"].orEmpty()
                val authCode = parseAuthCode(result)
                AlipayAuthResult(
                    success = status == "9000" && !authCode.isNullOrBlank(),
                    resultStatus = status,
                    authCode = authCode,
                    memo = raw["memo"].orEmpty(),
                )
            }
        } catch (e: Exception) {
            AlipayAuthResult(
                success = false,
                resultStatus = "ERROR",
                authCode = null,
                memo = e.message.orEmpty(),
            )
        }
    }

    fun resultMessage(result: AlipayPayResult): String = when (result.resultStatus) {
        "9000" -> ""
        "8000" -> "支付处理中，请稍后查看订单状态"
        "6001" -> "已取消支付"
        "6002" -> "网络异常，请稍后重试"
        "4000" -> result.memo.ifBlank { "支付失败" }
        "ERROR" -> result.memo.ifBlank { "支付宝调起失败" }
        else -> result.memo.ifBlank { "支付未完成" }
    }

    fun authMessage(result: AlipayAuthResult): String = when (result.resultStatus) {
        "9000" -> if (result.authCode.isNullOrBlank()) "授权失败，未获取授权码（status=9000）" else ""
        "6001" -> "已取消授权"
        "6002" -> "网络异常，请稍后重试"
        "4000" -> result.memo.ifBlank { "授权失败" }
        "ERROR" -> result.memo.ifBlank { "支付宝调起失败" }
        else -> result.memo.ifBlank { "授权未完成（status=${result.resultStatus}）" }
    }

    private fun parseAuthCode(result: String): String? {
        if (result.isBlank()) return null
        val decoded = runCatching { URLDecoder.decode(result, "UTF-8") }.getOrDefault(result)
        val candidates = listOf(result, decoded)
        val patterns = listOf(
            Regex("""(?:^|&)auth_code=([^&]+)"""),
            Regex("""(?:^|&)authCode=([^&]+)"""),
        )
        for (candidate in candidates) {
            for (pattern in patterns) {
                val match = pattern.find(candidate) ?: continue
                val value = match.groupValues.getOrNull(1)?.trim()
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
        }
        return null
    }
}
