package com.lfc.consumer.payment

import android.app.Activity
import com.alipay.sdk.app.AuthTask
import com.alipay.sdk.app.PayTask
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
        "9000" -> if (result.authCode.isNullOrBlank()) "授权失败，未获取授权码" else ""
        "6001" -> "已取消授权"
        "6002" -> "网络异常，请稍后重试"
        "4000" -> result.memo.ifBlank { "授权失败" }
        "ERROR" -> result.memo.ifBlank { "支付宝调起失败" }
        else -> result.memo.ifBlank { "授权未完成" }
    }

    private fun parseAuthCode(result: String): String? {
        if (result.isBlank()) return null
        val match = Regex("""auth_code=([^&]+)""").find(result) ?: return null
        return match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }
}
