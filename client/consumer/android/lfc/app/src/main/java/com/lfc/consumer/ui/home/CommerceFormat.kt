package com.lfc.consumer.ui.home

import com.lfc.consumer.data.model.PostProductDto

fun formatPriceYuan(amount: String?): String {
    val value = amount?.toDoubleOrNull() ?: return "¥0"
    return if (value % 1.0 == 0.0) "¥${value.toInt()}" else "¥${"%.2f".format(value)}"
}

fun formatActivityFeeLabel(fee: String?): String {
    val value = fee?.toDoubleOrNull() ?: 0.0
    return if (value <= 0) "免费" else formatPriceYuan(fee)
}

fun productCategoryLabel(category: String): String =
    com.lfc.consumer.data.model.postProductCategoryLabel(category)

fun productConditionLabel(condition: String): String = when (condition.uppercase()) {
    "BRAND_NEW" -> "全新"
    "LIKE_NEW" -> "几乎全新"
    "GOOD" -> "良好"
    "FAIR" -> "一般"
    else -> condition
}

fun productDeliveryLabel(method: String): String = when (method.uppercase()) {
    "PICKUP" -> "面交"
    "EXPRESS" -> "快递"
    "BOTH" -> "面交/快递"
    else -> method
}

fun productStatusLabel(product: PostProductDto): String = when (product.status.uppercase()) {
    "ON_SALE" -> "在售"
    "SOLD" -> "已售出"
    "OFF_SHELF" -> "已下架"
    else -> product.status
}

data class PlatformSettlement(
    val platformFee: Double,
    val payeeAmount: Double,
)

fun calculatePlatformSettlement(
    totalAmount: String?,
    feeRate: Double,
    minFee: Double = 0.0,
): PlatformSettlement {
    val total = totalAmount?.toDoubleOrNull() ?: 0.0
    if (total <= 0) return PlatformSettlement(0.0, 0.0)

    val rate = if (feeRate > 0) feeRate else 0.0
    var fee = total * rate
    if (minFee > 0) fee = maxOf(fee, minFee)
    fee = (fee * 100).roundToInt() / 100.0
    if (fee >= total) fee = maxOf(0.0, ((total - 0.01) * 100).roundToInt() / 100.0)
    val payee = ((total - fee) * 100).roundToInt() / 100.0
    return PlatformSettlement(fee, payee)
}

fun formatPayeeReceiveHint(price: String?, feeRateLabel: String?): String? {
    val rateLabel = feeRateLabel?.takeIf { it.isNotBlank() } ?: return null
    val settlement = calculatePlatformSettlement(price, parseFeeRateLabel(rateLabel))
    if (settlement.payeeAmount <= 0) return null
    return "卖家预计到账 ${formatPriceYuan(settlement.payeeAmount.toString())}（平台服务费 $rateLabel）"
}

private fun parseFeeRateLabel(label: String): Double {
    val numeric = label.removeSuffix("%").trim().toDoubleOrNull() ?: return 0.05
    return numeric / 100.0
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
