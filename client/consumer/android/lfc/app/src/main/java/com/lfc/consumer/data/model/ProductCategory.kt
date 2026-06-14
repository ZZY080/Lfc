package com.lfc.consumer.data.model

const val DEFAULT_POST_PRODUCT_CATEGORY = "GENERAL"

private val POST_PRODUCT_CATEGORY_LABELS = mapOf(
    "GENERAL" to "综合商品",
    "SECOND_HAND" to "二手闲置",
    "DIGITAL" to "数码电器",
    "CLOTHING" to "服饰鞋包",
    "BOOK" to "书籍文具",
    "DAILY" to "日用百货",
    "FOOD" to "食品饮料",
    "BEAUTY" to "美妆个护",
    "SPORTS" to "运动户外",
    "HANDMADE" to "手作文创",
    "TICKET" to "票券卡券",
    "SERVICE" to "技能服务",
    "OTHER" to "其他",
)

fun postProductCategoryLabel(category: String?): String =
    POST_PRODUCT_CATEGORY_LABELS[category?.uppercase()] ?: "商品"
