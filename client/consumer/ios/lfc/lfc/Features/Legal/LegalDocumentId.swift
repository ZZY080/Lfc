import Foundation

enum LegalDocumentId: String, CaseIterable, Hashable, Identifiable {
    case userAgreement = "user_agreement"
    case privacyPolicy = "privacy_policy"
    case communityGuidelines = "community_guidelines"
    case c2cSecurity = "c2c_security"
    case c2cAfterSales = "c2c_after_sales"
    case promotionAds = "promotion_ads"
    case thirdPartySDK = "third_party_sdk"
    case paymentTerms = "payment_terms"

    var id: String { routeKey }

    var routeKey: String { rawValue }

    var title: String {
        switch self {
        case .userAgreement: return "用户服务协议"
        case .privacyPolicy: return "隐私政策"
        case .communityGuidelines: return "社区规范"
        case .c2cSecurity: return "C2C 交易安全保障说明"
        case .c2cAfterSales: return "C2C 退货售后规则"
        case .promotionAds: return "推广与广告服务说明"
        case .thirdPartySDK: return "第三方 SDK 说明"
        case .paymentTerms: return "支付与分账说明"
        }
    }

    var consentLinkTitle: String { "《\(title)》" }

    static func fromRouteKey(_ key: String) -> LegalDocumentId? {
        LegalDocumentId(rawValue: key)
    }
}

struct LegalDocumentSection: Hashable {
    let heading: String
    let paragraphs: [String]
}

struct LegalDocument {
    let id: LegalDocumentId
    let title: String
    let updatedAt: String
    let sections: [LegalDocumentSection]
}

func legalDocument(of id: LegalDocumentId) -> LegalDocument {
    switch id {
    case .userAgreement:
        return LegalDocument(
            id: id,
            title: "用户服务协议",
            updatedAt: "2026年6月5日",
            sections: [
                LegalDocumentSection(
                    heading: "一、总则",
                    paragraphs: [
                        "欢迎使用莲峰校园（以下简称「本平台」）。本协议由您与平台运营方共同订立，具有合同效力。",
                        "本平台面向已通过学生身份认证的用户，提供校园笔记分享、活动组织、闲置交易及相关增值服务。注册或使用即表示您已阅读并同意本协议。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "二、账号与身份",
                    paragraphs: [
                        "您应使用真实、有效的校园邮箱注册，并按平台要求完成学生证等身份核验。",
                        "账号仅限本人使用，不得出租、出借、转让或售卖。您需妥善保管密码，对账号下的行为承担责任。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "三、用户行为规范",
                    paragraphs: [
                        "您发布的内容应遵守法律法规及《社区规范》，不得发布违法、侵权、虚假、骚扰、色情、暴力或其他不当信息。",
                        "不得利用本平台从事刷单、传销、赌博、诈骗等违法行为。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "四、交易与支付",
                    paragraphs: [
                        "闲置商品购买、活动报名、笔记擦亮/活动推广等付费功能，均通过支付宝完成。",
                        "C2C 交易适用平台托管与分账机制，具体见《支付与分账说明》及《C2C 退货售后规则》。",
                    ]
                ),
            ]
        )

    case .privacyPolicy:
        return LegalDocument(
            id: id,
            title: "隐私政策",
            updatedAt: "2026年6月16日",
            sections: [
                LegalDocumentSection(
                    heading: "引言",
                    paragraphs: [
                        "莲峰校园（以下简称「本平台」或「我们」）由平台运营方提供并运营。我们深知个人信息对您的重要性，并将按照法律法规要求，采取相应安全保护措施。",
                        "请您在使用本 App 及注册账号前，仔细阅读并充分理解本政策。一旦您点击同意、注册账号或实际使用我们的服务，即表示您已充分理解并同意本政策的全部内容。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "一、我们如何收集和使用您的个人信息",
                    paragraphs: [
                        "账号注册与登录：校园邮箱、密码（加密存储）、学号、真实姓名、学生证照片（用于身份核验）。",
                        "个人资料：昵称、头像、封面、简介等您主动填写的内容。",
                        "交易信息：订单号、金额、支付状态、退款/售后记录、收款账号绑定状态。",
                        "设备与日志：设备型号、操作系统、App 版本、网络信息、操作日志、崩溃信息。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "二、我们如何共享您的个人信息",
                    paragraphs: [
                        "未经您单独同意，我们不会向第三方出售您的个人信息。",
                        "为实现支付功能，与支付宝共享必要交易信息；为实现定位功能，与高德地图共享位置信息（经您授权）。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "三、您的权利",
                    paragraphs: [
                        "查询与更正：可在「编辑资料」「设置」中查看和修改个人资料。",
                        "注销账号：通过 App 内反馈或客服申请注销，我们将停止提供服务并删除或匿名化您的个人信息。",
                    ]
                ),
            ]
        )

    case .c2cSecurity:
        return LegalDocument(
            id: id,
            title: "C2C 交易安全保障说明",
            updatedAt: "2026年6月16日",
            sections: [
                LegalDocumentSection(
                    heading: "资金托管",
                    paragraphs: [
                        "C2C 交易通过支付宝支付，确认收货或活动分账前，卖家/发起人通常无法提现，降低不履约风险。",
                        "平台提供学生身份核验、内容审核、售后审核与争议协助。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "安全建议",
                    paragraphs: [
                        "买家请当面验货后再确认收货；勿绕过平台线下付款。",
                        "卖家请如实描述商品与活动；勿诱导线下交易。",
                    ]
                ),
            ]
        )

    case .communityGuidelines:
        return LegalDocument(
            id: id,
            title: "社区规范",
            updatedAt: "2026年6月16日",
            sections: [
                LegalDocumentSection(
                    heading: "一、基本原则",
                    paragraphs: [
                        "莲峰校园倡导真实、友善、互助的校园社区氛围。请尊重他人，文明交流。",
                        "发布内容应与校园生活相关，禁止 spam、广告轰炸及无关引流。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "二、禁止内容",
                    paragraphs: [
                        "违反国家法律法规的内容。",
                        "侵犯他人肖像权、隐私权、知识产权或其他合法权益的内容。",
                        "谣言、虚假活动、虚假商品信息或欺诈行为。",
                    ]
                ),
            ]
        )

    case .c2cAfterSales:
        return LegalDocument(
            id: id,
            title: "C2C 退货售后规则",
            updatedAt: "2026年6月16日",
            sections: [
                LegalDocumentSection(
                    heading: "一、适用范围",
                    paragraphs: [
                        "本规则适用于本平台用户之间通过 App 完成的 C2C 交易，包括闲置商品交易与付费活动报名。",
                        "平台作为信息展示与支付托管方，有权依据本规则处理退款与订单状态。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "二、闲置商品 — 退款与退货",
                    paragraphs: [
                        "已付款、未确认收货：买家可在订单详情申请「售后/退款」，审核通过后原路退回。",
                        "确认收货后：款项进入分账流程，如有异议请先联系卖家协商。",
                    ]
                ),
            ]
        )

    case .promotionAds:
        return LegalDocument(
            id: id,
            title: "推广与广告服务说明",
            updatedAt: "2026年6月16日",
            sections: [
                LegalDocumentSection(
                    heading: "服务说明",
                    paragraphs: [
                        "笔记擦亮、活动推广用于在平台内提升展示优先级，属于平台内推广展示服务。",
                        "不向外部广告联盟出售用户个人信息，推广内容须遵守《社区规范》。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "付费规则",
                    paragraphs: [
                        "支付成功后推广即生效，原则上不支持退款；价格与时长以购买页面为准。",
                    ]
                ),
            ]
        )

    case .thirdPartySDK:
        return LegalDocument(
            id: id,
            title: "第三方 SDK 说明",
            updatedAt: "2026年6月5日",
            sections: [
                LegalDocumentSection(
                    heading: "说明",
                    paragraphs: [
                        "为实现特定功能，本 App 集成以下第三方 SDK。我们仅出于本政策所述目的调用相关能力。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "支付宝 SDK",
                    paragraphs: [
                        "提供方：蚂蚁科技集团股份有限公司",
                        "使用目的：支付、收款授权、分账",
                    ]
                ),
                LegalDocumentSection(
                    heading: "高德地图 SDK",
                    paragraphs: [
                        "提供方：高德软件有限公司",
                        "使用目的：活动定位、地图选点、导航",
                    ]
                ),
            ]
        )

    case .paymentTerms:
        return LegalDocument(
            id: id,
            title: "支付与分账说明",
            updatedAt: "2026年6月5日",
            sections: [
                LegalDocumentSection(
                    heading: "一、支付方式",
                    paragraphs: [
                        "本平台交易均通过支付宝 App 完成支付，请确保已安装支付宝客户端。",
                        "支付前请核对订单金额、商品/活动信息及收款方。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "二、闲置商品（C2C）",
                    paragraphs: [
                        "买家付款后，资金由平台托管，卖家此时无法提现。",
                        "买家确认收货后，平台按约定比例分账给卖家（扣除平台技术服务费）。",
                    ]
                ),
                LegalDocumentSection(
                    heading: "三、活动报名（C2C）",
                    paragraphs: [
                        "付费活动报名成功后锁定名额，款项托管在平台。",
                        "活动开始前，参与者可申请退款并取消报名。",
                    ]
                ),
            ]
        )
    }
}

let allLegalDocuments: [LegalDocumentId] = LegalDocumentId.allCases
