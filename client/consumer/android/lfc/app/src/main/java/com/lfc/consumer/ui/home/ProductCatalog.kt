package com.lfc.consumer.ui.home

import com.lfc.consumer.data.model.DEFAULT_POST_PRODUCT_CATEGORY
import com.lfc.consumer.data.model.postProductCategoryLabel

data class ProductCategoryOption(val value: String, val label: String)

data class ProductConditionOption(val value: String, val label: String)

val POST_PRODUCT_CATEGORIES = listOf(
    "GENERAL",
    "SECOND_HAND",
    "DIGITAL",
    "CLOTHING",
    "BOOK",
    "DAILY",
    "FOOD",
    "BEAUTY",
    "SPORTS",
    "HANDMADE",
    "TICKET",
    "SERVICE",
    "OTHER",
).map { value -> ProductCategoryOption(value, postProductCategoryLabel(value)) }

val POST_PRODUCT_CONDITIONS = listOf(
    ProductConditionOption("BRAND_NEW", "全新"),
    ProductConditionOption("LIKE_NEW", "几乎全新"),
    ProductConditionOption("GOOD", "良好"),
    ProductConditionOption("FAIR", "一般"),
)

val POST_PRODUCT_DELIVERY_METHODS = listOf(
    ProductCategoryOption("PICKUP", "面交"),
    ProductCategoryOption("EXPRESS", "快递"),
    ProductCategoryOption("BOTH", "均可"),
)
