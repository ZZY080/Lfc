package com.lfc.consumer.ui.home

fun paymentBizTypeLabel(bizType: String): String = when (bizType) {
    "ACTIVITY_JOIN" -> "校园活动"
    "POST_PRODUCT_PURCHASE" -> "闲置商品"
    "POST_BOOST" -> "笔记擦亮"
    "ACTIVITY_PROMOTE" -> "活动推广"
    else -> "订单"
}

fun isPlatformRevenueBizType(bizType: String): Boolean =
    bizType == "POST_BOOST" || bizType == "ACTIVITY_PROMOTE"

fun requiresBuyerFulfillment(bizType: String): Boolean =
    bizType == "ACTIVITY_JOIN" || bizType == "POST_PRODUCT_PURCHASE"

fun fulfillmentBadgeLabel(bizType: String): String =
    if (isPlatformRevenueBizType(bizType)) "服务说明" else "履约保障"
