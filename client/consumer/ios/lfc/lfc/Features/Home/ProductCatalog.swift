import Foundation

let defaultPostProductCategory = "GENERAL"

private let postProductCategoryLabels: [String: String] = [
    "GENERAL": "综合商品",
    "SECOND_HAND": "二手闲置",
    "DIGITAL": "数码电器",
    "CLOTHING": "服饰鞋包",
    "BOOK": "书籍文具",
    "DAILY": "日用百货",
    "FOOD": "食品饮料",
    "BEAUTY": "美妆个护",
    "SPORTS": "运动户外",
    "HANDMADE": "手作文创",
    "TICKET": "票券卡券",
    "SERVICE": "技能服务",
    "OTHER": "其他",
]

func postProductCategoryLabel(_ category: String?) -> String {
    guard let category else { return "商品" }
    return postProductCategoryLabels[category.uppercased()] ?? "商品"
}

let postProductCategories: [(value: String, label: String)] = [
    "GENERAL", "SECOND_HAND", "DIGITAL", "CLOTHING", "BOOK", "DAILY",
    "FOOD", "BEAUTY", "SPORTS", "HANDMADE", "TICKET", "SERVICE", "OTHER",
].map { ($0, postProductCategoryLabel($0)) }

let postProductConditions: [(value: String, label: String)] = [
    ("BRAND_NEW", "全新"),
    ("LIKE_NEW", "几乎全新"),
    ("GOOD", "良好"),
    ("FAIR", "一般"),
]

let postProductDeliveryMethods: [(value: String, label: String)] = [
    ("PICKUP", "面交"),
    ("EXPRESS", "快递"),
    ("BOTH", "均可"),
]

let fallbackPublishCategories = [
    "校园生活", "学习", "美食", "运动", "旅行", "穿搭", "数码", "其他",
]
