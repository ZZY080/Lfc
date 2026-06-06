package com.lfc.consumer.payment

import android.app.Activity
import android.os.Build
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
    suspend fun pay(orderStr: String): AlipayPayResult {
        val activity = AlipayPaymentHost.getActivity()
            ?: return AlipayPayResult(
                success = false,
                resultStatus = "ERROR",
                memo = "无法调起支付宝，请重试",
            )
        return pay(activity, orderStr)
    }

    /**
     * 支付宝 SDK 要求 PayTask 在**非 UI 子线程**调用；在主线程调用会阻塞 Compose 导致卡死。
     */
    suspend fun pay(activity: Activity, orderStr: String): AlipayPayResult {
        if (orderStr.isBlank()) {
            return AlipayPayResult(
                success = false,
                resultStatus = "ERROR",
                memo = "支付参数为空",
            )
        }
        if (isActivityUnavailable(activity)) {
            return AlipayPayResult(
                success = false,
                resultStatus = "ERROR",
                memo = "页面已关闭，无法调起支付宝",
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val raw = PayTask(activity).payV2(orderStr, false)
                val status = raw["resultStatus"].orEmpty()
                AlipayPayResult(
                    success = status == "9000",
                    resultStatus = status,
                    memo = raw["memo"].orEmpty(),
                )
            } catch (e: Exception) {
                AlipayPayResult(
                    success = false,
                    resultStatus = "ERROR",
                    memo = e.message.orEmpty(),
                )
            }
        }
    }

    suspend fun auth(authInfo: String): AlipayAuthResult {
        val activity = AlipayPaymentHost.getActivity()
            ?: return AlipayAuthResult(
                success = false,
                resultStatus = "ERROR",
                authCode = null,
                memo = "无法调起支付宝，请重试",
            )
        return auth(activity, authInfo)
    }

    suspend fun auth(activity: Activity, authInfo: String): AlipayAuthResult {
        if (isActivityUnavailable(activity)) {
            return AlipayAuthResult(
                success = false,
                resultStatus = "ERROR",
                authCode = null,
                memo = "页面已关闭，无法调起支付宝",
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val raw = AuthTask(activity).authV2(authInfo, false)
                val status = raw["resultStatus"].orEmpty()
                val result = raw["result"].orEmpty()
                val authCode = parseAuthCode(result)
                AlipayAuthResult(
                    success = status == "9000" && !authCode.isNullOrBlank(),
                    resultStatus = status,
                    authCode = authCode,
                    memo = raw["memo"].orEmpty(),
                )
            } catch (e: Exception) {
                AlipayAuthResult(
                    success = false,
                    resultStatus = "ERROR",
                    authCode = null,
                    memo = e.message.orEmpty(),
                )
            }
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

    private fun isActivityUnavailable(activity: Activity): Boolean {
        if (activity.isFinishing) {
            return true
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed
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
