package com.lfc.consumer.ui.legal

enum class LegalDocumentId(val routeKey: String) {
    USER_AGREEMENT("user_agreement"),
    PRIVACY_POLICY("privacy_policy"),
    COMMUNITY_GUIDELINES("community_guidelines"),
    C2C_AFTER_SALES("c2c_after_sales"),
    THIRD_PARTY_SDK("third_party_sdk"),
    PAYMENT_TERMS("payment_terms"),
    ;

    companion object {
        fun fromRouteKey(key: String): LegalDocumentId? =
            entries.firstOrNull { it.routeKey == key }
    }
}

data class LegalDocument(
    val id: LegalDocumentId,
    val title: String,
    val updatedAt: String,
    val sections: List<LegalDocumentSection>,
)

data class LegalDocumentSection(
    val heading: String,
    val paragraphs: List<String>,
)

fun legalDocumentOf(id: LegalDocumentId): LegalDocument = when (id) {
    LegalDocumentId.USER_AGREEMENT -> LegalDocument(
        id = id,
        title = "用户服务协议",
        updatedAt = "2026年6月5日",
        sections = listOf(
            LegalDocumentSection(
                heading = "一、总则",
                paragraphs = listOf(
                    "欢迎使用莲峰校园（以下简称「本平台」）。本协议由您与平台运营方共同订立，具有合同效力。",
                    "本平台面向已通过学生身份认证的用户，提供校园笔记分享、活动组织、闲置交易及相关增值服务。注册或使用即表示您已阅读并同意本协议。",
                ),
            ),
            LegalDocumentSection(
                heading = "二、账号与身份",
                paragraphs = listOf(
                    "您应使用真实、有效的校园邮箱注册，并按平台要求完成学生证等身份核验。",
                    "账号仅限本人使用，不得出租、出借、转让或售卖。您需妥善保管密码，对账号下的行为承担责任。",
                    "如发现账号被盗或异常使用，请立即联系平台客服处理。",
                ),
            ),
            LegalDocumentSection(
                heading = "三、用户行为规范",
                paragraphs = listOf(
                    "您发布的内容应遵守法律法规及《社区规范》，不得发布违法、侵权、虚假、骚扰、色情、暴力或其他不当信息。",
                    "不得利用本平台从事刷单、传销、赌博、诈骗等违法行为。",
                    "对于笔记、活动、商品交易及推广服务，您应保证信息真实、合法，并自行对发布内容负责。",
                ),
            ),
            LegalDocumentSection(
                heading = "四、交易与支付",
                paragraphs = listOf(
                    "闲置商品购买、活动报名、笔记擦亮/活动推广等付费功能，均通过支付宝完成。",
                    "C2C 交易（闲置商品、付费活动）适用平台托管与分账机制，具体见《支付与分账说明》及《C2C 退货售后规则》。",
                    "平台可能收取技术服务费或推广费用，费率以页面展示及订单详情为准。",
                ),
            ),
            LegalDocumentSection(
                heading = "五、知识产权",
                paragraphs = listOf(
                    "您保留对所发布内容的著作权，同时授予平台为提供服务所必需的存储、展示、传播许可。",
                    "未经权利人许可，不得上传、传播他人享有著作权或其他合法权益的内容。",
                ),
            ),
            LegalDocumentSection(
                heading = "六、服务变更与终止",
                paragraphs = listOf(
                    "平台有权根据业务需要调整功能，并将通过 App 内公告等方式告知。",
                    "对于违反本协议或相关规则的用户，平台有权采取警告、限制功能、下架内容、封禁账号等措施。",
                    "您可随时停止使用本平台；注销账号请联系客服，我们将按隐私政策处理您的个人信息。",
                ),
            ),
            LegalDocumentSection(
                heading = "七、免责声明",
                paragraphs = listOf(
                    "用户之间的线下交易、活动参与等行为由用户自行协商并承担风险，平台仅提供信息展示与支付托管等辅助服务。",
                    "因不可抗力、网络故障、第三方服务异常等导致的服务中断，平台将在合理范围内协助处理，但不承担超出法律规定之外的责任。",
                ),
            ),
            LegalDocumentSection(
                heading = "八、其他",
                paragraphs = listOf(
                    "本协议与《隐私政策》《社区规范》等文件共同构成完整协议。如有冲突，以专门规则为准。",
                    "本协议解释权归平台运营方所有。如有疑问，请通过 App 内反馈渠道联系我们。",
                ),
            ),
        ),
    )

    LegalDocumentId.PRIVACY_POLICY -> LegalDocument(
        id = id,
        title = "隐私政策",
        updatedAt = "2026年6月5日",
        sections = listOf(
            LegalDocumentSection(
                heading = "引言",
                paragraphs = listOf(
                    "莲峰校园重视您的个人信息保护。本政策说明我们如何收集、使用、存储、共享与保护您的信息，以及您享有的权利。",
                    "请在使用服务前仔细阅读。若您不同意本政策，请停止使用并勿注册账号。",
                ),
            ),
            LegalDocumentSection(
                heading = "一、我们收集的信息",
                paragraphs = listOf(
                    "账号信息：邮箱、密码（加密存储）、学号、真实姓名、学生证照片（用于身份核验）。",
                    "个人资料：昵称、头像、封面、简介等您主动填写的内容。",
                    "内容与互动：笔记、活动、评论、点赞、收藏、私信及您发布的商品信息。",
                    "交易信息：订单号、金额、支付状态、收款账号绑定状态（支付宝账号脱敏展示）。",
                    "设备与日志：设备型号、操作系统、App 版本、操作日志、崩溃信息，用于保障服务安全与稳定。",
                    "位置信息：仅在您发布活动或选择地点时，经授权后获取一次定位，用于填写活动地点。",
                ),
            ),
            LegalDocumentSection(
                heading = "二、信息的使用目的",
                paragraphs = listOf(
                    "提供注册、登录、内容发布、社交互动、交易支付、消息通知等核心功能。",
                    "完成学生身份审核，维护校园社区安全。",
                    "处理 C2C 订单、托管分账、退款售后及交易争议协助。",
                    "改进产品体验、进行数据统计分析（采用去标识化方式）。",
                    "履行法律法规要求，配合监管或司法机关依法提出的请求。",
                ),
            ),
            LegalDocumentSection(
                heading = "三、信息的共享与披露",
                paragraphs = listOf(
                    "未经您同意，我们不会向第三方出售您的个人信息。",
                    "以下情形可能共享必要信息：支付宝（支付与分账）、高德地图（定位与导航，详见第三方 SDK 说明）、云存储/CDN 服务商（图片与文件存储）。",
                    "根据法律法规、诉讼、仲裁或政府主管部门要求必须披露的情形。",
                    "在合并、收购或破产清算时，如涉及个人信息转移，我们将要求新的持有方继续受本政策约束。",
                ),
            ),
            LegalDocumentSection(
                heading = "四、信息存储与安全",
                paragraphs = listOf(
                    "您的信息存储于中华人民共和国境内服务器，保存期限为实现目的所必需的最短时间，法律法规另有规定的除外。",
                    "我们采取加密传输、访问控制、权限最小化等安全措施保护您的信息。",
                    "尽管已采取合理措施，互联网传输仍无法保证绝对安全，请您理解相关风险。",
                ),
            ),
            LegalDocumentSection(
                heading = "五、您的权利",
                paragraphs = listOf(
                    "查询与更正：可在「编辑资料」「设置」中查看和修改个人资料。",
                    "隐私控制：可在设置中控制评论、收藏、赞过是否对他人公开。",
                    "删除内容：您可自行编辑、删除或下架已发布的笔记与活动。",
                    "注销账号：联系客服申请注销，我们将停止提供服务并删除或匿名化您的个人信息，法律法规另有规定的除外。",
                ),
            ),
            LegalDocumentSection(
                heading = "六、未成年人保护",
                paragraphs = listOf(
                    "本平台主要面向已注册的高校学生用户。若您未满 18 周岁，请在监护人指导下阅读本政策并使用服务。",
                ),
            ),
            LegalDocumentSection(
                heading = "七、政策更新",
                paragraphs = listOf(
                    "我们可能适时修订本政策，重大变更将通过 App 内弹窗或公告通知。",
                    "若您继续使用服务，即视为接受更新后的政策。",
                ),
            ),
        ),
    )

    LegalDocumentId.COMMUNITY_GUIDELINES -> LegalDocument(
        id = id,
        title = "社区规范",
        updatedAt = "2026年6月5日",
        sections = listOf(
            LegalDocumentSection(
                heading = "一、基本原则",
                paragraphs = listOf(
                    "莲峰校园倡导真实、友善、互助的校园社区氛围。请尊重他人，文明交流。",
                    "发布内容应与校园生活相关，禁止 spam、广告轰炸及无关引流。",
                ),
            ),
            LegalDocumentSection(
                heading = "二、禁止内容",
                paragraphs = listOf(
                    "违反国家法律法规的内容。",
                    "侵犯他人肖像权、隐私权、知识产权或其他合法权益的内容。",
                    "谣言、虚假活动、虚假商品信息或欺诈行为。",
                    "色情低俗、暴力恐怖、仇恨歧视、人身攻击或骚扰信息。",
                    "未经授权的校外商业广告、传销、赌博、代购违禁品等。",
                ),
            ),
            LegalDocumentSection(
                heading = "三、交易与活动",
                paragraphs = listOf(
                    "闲置商品应如实描述成色与价格，不得销售违禁品。",
                    "活动发起人应保证活动信息真实，按时履约，合理收取费用。",
                    "禁止虚假交易、刷单、恶意竞价推广等扰乱秩序的行为。",
                    "C2C 交易发生争议时，买卖双方应诚信协商；平台将依据《C2C 退货售后规则》协助处理退款与履约。",
                ),
            ),
            LegalDocumentSection(
                heading = "四、违规处理",
                paragraphs = listOf(
                    "平台有权对违规内容进行删除、下架、限制展示。",
                    "情节严重者将被限制发布、禁言或永久封禁账号。",
                    "涉嫌违法犯罪的，平台将依法向有关部门报告。",
                ),
            ),
        ),
    )

    LegalDocumentId.C2C_AFTER_SALES -> LegalDocument(
        id = id,
        title = "C2C 退货售后规则",
        updatedAt = "2026年6月5日",
        sections = listOf(
            LegalDocumentSection(
                heading = "一、适用范围",
                paragraphs = listOf(
                    "本规则适用于本平台用户之间通过 App 完成的 C2C 交易，包括：",
                    "（1）闲置商品交易：买家向卖家购买笔记关联的二手/闲置商品；",
                    "（2）付费活动报名：参与者向活动发起人支付报名费用。",
                    "以下服务不适用本规则：笔记擦亮、活动推广等增值服务费（支付即生效，原则上不支持退款，详见《支付与分账说明》）。",
                    "平台作为信息展示与支付托管方，不介入用户线下交付细节，但有权依据本规则处理退款与订单状态。",
                ),
            ),
            LegalDocumentSection(
                heading = "二、基本交易流程",
                paragraphs = listOf(
                    "【闲置商品】买家下单支付 → 资金平台托管 → 买卖双方面交/邮寄完成交付 → 买家确认收货 → 平台分账给卖家。",
                    "【付费活动】参与者支付 → 锁定名额 → 活动开始/结束 → 平台按规则分账给发起人。",
                    "在分账完成前，资金由支付渠道（支付宝）及平台订单系统托管，卖家/发起人尚未收到款项。",
                ),
            ),
            LegalDocumentSection(
                heading = "三、闲置商品 — 退款与退货",
                paragraphs = listOf(
                    "1. 待付款订单：买家可在「我的订单」中直接取消，无需申请售后。",
                    "2. 已付款、未确认收货：买家可在订单详情或订单列表申请「售后/退款」。提交后平台将按订单金额原路退回，关联商品笔记将重新上架。",
                    "3. 确认收货前请当面或收货时验货。若商品与描述严重不符、存在质量问题或卖家无法交付，应优先与卖家协商，并可申请退款。",
                    "4. 确认收货后：款项进入分账流程。如对交易有异议，请先联系卖家协商；必要时可联系平台客服，平台将视情况协助，但不保证一定支持退款。",
                    "5. 自动确认收货：若买家长期未操作，系统将在订单页提示的天数（通常为 7 天）后自动确认收货并完成分账。",
                    "6. 退货方式：本平台以校园同城面交为主，原则上不强制物流退货；退款完成后，双方应自行约定未交付商品或已交付商品的返还方式。",
                ),
            ),
            LegalDocumentSection(
                heading = "四、付费活动 — 退款与取消",
                paragraphs = listOf(
                    "1. 待付款订单：可直接取消。",
                    "2. 已报名、活动未开始：参与者可在「我的订单」申请售后/退款，退款成功后自动取消报名资格。",
                    "3. 活动已开始或已结束：原则上不再支持退款，除非活动严重取消、信息与描述严重不符或存在欺诈等情形。",
                    "4. 活动发起人取消或变更活动：应提前通知参与者并协商处理；平台可协助但不对发起人线下行为承担担保责任。",
                    "5. 免费活动不适用本退款流程，报名后如需取消请按活动说明或联系发起人。",
                ),
            ),
            LegalDocumentSection(
                heading = "五、售后申请方式",
                paragraphs = listOf(
                    "路径：我的 → 设置/订单 → 我的订单 → 选择对应订单 →「申请售后」。",
                    "申请时需填写退款原因，请如实描述，便于平台及对方了解情况。",
                    "同一订单同时只能有一笔进行中的售后申请。",
                    "未支付订单请使用「取消订单」，勿重复申请售后。",
                ),
            ),
            LegalDocumentSection(
                heading = "六、退款处理",
                paragraphs = listOf(
                    "退款金额：C2C 订单原则上按实付金额全额原路退回（退回买家支付宝账户）。",
                    "到账时间：取决于支付宝及银行处理，通常 1–7 个工作日，请耐心等待。",
                    "退款成功后，订单状态将更新为「已退款」，相关商品/名额将释放或重新上架。",
                    "若退款失败（如支付渠道异常），订单将保持原状态，请稍后重试或联系客服。",
                ),
            ),
            LegalDocumentSection(
                heading = "七、买卖双方责任",
                paragraphs = listOf(
                    "【卖家/发起人】应保证发布信息真实、合法；按约定交付商品或举办活动；配合买家合理询问；不得诱导线下绕过平台支付。",
                    "【买家/参与者】应在支付前仔细核对商品/活动信息；按时完成面交或收货确认；恶意申请退款、虚假投诉将被限制功能或封禁。",
                    "因用户自身原因（如误拍、不喜欢、个人行程变更等）发起的退款，应在规则允许的时间窗口内申请，并自行承担已产生的沟通成本。",
                ),
            ),
            LegalDocumentSection(
                heading = "八、争议处理",
                paragraphs = listOf(
                    "鼓励买卖双方优先私信协商解决。",
                    "协商不成的，可保留聊天记录、商品照片、活动通知等证据，通过 App 内反馈或客服渠道申诉。",
                    "平台有权根据订单记录、售后原因、历史行为等作出处理，包括但不限于支持退款、驳回申请、限制账号。",
                    "涉嫌诈骗、售假、违禁品交易的，平台将冻结相关账号并依法配合调查。",
                ),
            ),
            LegalDocumentSection(
                heading = "九、免责与限制",
                paragraphs = listOf(
                    "平台不对用户线下交易过程中的人身、财产损害承担责任，但会尽力提供订单与支付记录作为协助依据。",
                    "因不可抗力（自然灾害、政策变化、支付系统故障等）导致无法履约或延迟退款的，平台将协助沟通但不承担超出法律规定之外的责任。",
                    "本规则与《用户服务协议》《支付与分账说明》不一致的，以本规则对 C2C 售后事项的专门约定为准。",
                ),
            ),
        ),
    )

    LegalDocumentId.THIRD_PARTY_SDK -> LegalDocument(
        id = id,
        title = "第三方 SDK 说明",
        updatedAt = "2026年6月5日",
        sections = listOf(
            LegalDocumentSection(
                heading = "说明",
                paragraphs = listOf(
                    "为实现特定功能，本 App 集成以下第三方 SDK。我们仅出于本政策所述目的调用相关能力。",
                ),
            ),
            LegalDocumentSection(
                heading = "支付宝 SDK",
                paragraphs = listOf(
                    "提供方：蚂蚁科技集团股份有限公司",
                    "使用目的：支付、收款授权、分账",
                    "收集信息：设备信息、网络信息、交易订单信息（由支付宝按其隐私政策处理）",
                    "隐私政策：https://opendocs.alipay.com/open/01g6qm",
                ),
            ),
            LegalDocumentSection(
                heading = "高德地图 SDK",
                paragraphs = listOf(
                    "提供方：高德软件有限公司",
                    "使用目的：活动定位、地图选点、导航",
                    "收集信息：位置信息、设备信息（经您授权后）",
                    "隐私政策：https://lbs.amap.com/pages/privacy/",
                ),
            ),
            LegalDocumentSection(
                heading = "Coil 图片加载",
                paragraphs = listOf(
                    "使用目的：加载与展示网络图片",
                    "收集信息：不直接向第三方发送个人身份信息，仅请求图片 URL",
                ),
            ),
        ),
    )

    LegalDocumentId.PAYMENT_TERMS -> LegalDocument(
        id = id,
        title = "支付与分账说明",
        updatedAt = "2026年6月5日",
        sections = listOf(
            LegalDocumentSection(
                heading = "一、支付方式",
                paragraphs = listOf(
                    "本平台交易均通过支付宝 App 完成支付，请确保已安装支付宝客户端。",
                    "支付前请核对订单金额、商品/活动信息及收款方。",
                ),
            ),
            LegalDocumentSection(
                heading = "二、闲置商品（C2C）",
                paragraphs = listOf(
                    "买家付款后，资金由平台托管，卖家此时无法提现。",
                    "买家确认收货后，平台按约定比例分账给卖家（扣除平台技术服务费）。",
                    "若买家在提示期限内未确认收货，系统将自动确认并完成分账。",
                    "未确认收货前，买家可申请全额退款，详见《C2C 退货售后规则》。",
                    "退款成功后，商品笔记将恢复「在售」状态，其他用户可再次购买。",
                ),
            ),
            LegalDocumentSection(
                heading = "三、活动报名（C2C）",
                paragraphs = listOf(
                    "付费活动报名成功后锁定名额，款项托管在平台。",
                    "活动开始前，参与者可申请退款并取消报名。",
                    "活动结束后，款项将分账给活动发起人。",
                    "活动开始后原则上不再受理退款，特殊情形见《C2C 退货售后规则》。",
                ),
            ),
            LegalDocumentSection(
                heading = "四、增值服务（非 C2C）",
                paragraphs = listOf(
                    "笔记擦亮、活动推广等增值服务，支付成功后立即生效，款项直接进入平台收款账户。",
                    "增值服务不涉及买卖双方面交或活动履约，原则上不支持退款。",
                    "推广展示位置与时长以购买页面说明为准。",
                ),
            ),
            LegalDocumentSection(
                heading = "五、收款绑定",
                paragraphs = listOf(
                    "卖家/活动发起人需绑定本人实名支付宝账号方可收款。",
                    "绑定与解绑均通过支付宝官方授权流程完成，平台不存储您的支付密码。",
                ),
            ),
            LegalDocumentSection(
                heading = "六、售后与退款",
                paragraphs = listOf(
                    "C2C 订单的退货、退款、争议处理，统一适用《C2C 退货售后规则》。",
                    "待付款订单请使用「取消订单」，已付款订单请使用「申请售后」。",
                    "退款原路返回至买家支付宝账户，具体到账时间以支付宝为准。",
                ),
            ),
        ),
    )
}

val ALL_LEGAL_DOCUMENTS: List<LegalDocumentId> = LegalDocumentId.entries.toList()
