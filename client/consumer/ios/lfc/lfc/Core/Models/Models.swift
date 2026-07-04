import Foundation

// MARK: - Constants

let defaultPostCategory = "校园生活"

// MARK: - JSON helpers

enum JSONValue: Codable, Equatable, Sendable {
  case string(String)
  case int(Int)
  case double(Double)
  case bool(Bool)
  case null
  case array([JSONValue])
  case object([String: JSONValue])

  init(from decoder: Decoder) throws {
    let container = try decoder.singleValueContainer()
    if container.decodeNil() {
      self = .null
    } else if let value = try? container.decode(Bool.self) {
      self = .bool(value)
    } else if let value = try? container.decode(Int.self) {
      self = .int(value)
    } else if let value = try? container.decode(Double.self) {
      self = .double(value)
    } else if let value = try? container.decode(String.self) {
      self = .string(value)
    } else if let value = try? container.decode([JSONValue].self) {
      self = .array(value)
    } else if let value = try? container.decode([String: JSONValue].self) {
      self = .object(value)
    } else {
      throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported JSON value")
    }
  }

  func encode(to encoder: Encoder) throws {
    var container = encoder.singleValueContainer()
    switch self {
    case .string(let value):
      try container.encode(value)
    case .int(let value):
      try container.encode(value)
    case .double(let value):
      try container.encode(value)
    case .bool(let value):
      try container.encode(value)
    case .null:
      try container.encodeNil()
    case .array(let value):
      try container.encode(value)
    case .object(let value):
      try container.encode(value)
    }
  }

  static func from(_ value: Any) -> JSONValue {
    switch value {
    case let value as String:
      return .string(value)
    case let value as Int:
      return .int(value)
    case let value as Double:
      return .double(value)
    case let value as Bool:
      return .bool(value)
    case is NSNull:
      return .null
    case let value as [String: Any]:
      return .object(value.mapValues { from($0) })
    case let value as [Any]:
      return .array(value.map { from($0) })
    default:
      return .string(String(describing: value))
    }
  }
}

typealias JSONDictionary = [String: JSONValue]

// MARK: - Auth

struct UserDto: Codable, Identifiable, Equatable, Sendable {
  let id: Int
  let email: String
  let studentId: String
  let role: String
  let nickname: String?
  let avatarUrl: String?

  var displayName: String {
    if let nickname, !nickname.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
      return nickname
    }
    return studentId
  }
}

struct AuthResponse: Codable, Sendable {
  let accessToken: String
  let refreshToken: String
  let user: UserDto
}

struct LoginRequest: Codable, Sendable {
  let email: String
  let password: String
}

struct RefreshTokenRequest: Codable, Sendable {
  let refreshToken: String
}

// MARK: - Posts

struct PostDto: Codable, Identifiable, Equatable, Sendable {
  let id: Int
  let title: String
  let category: String
  let content: String
  let authorId: Int
  let createdAt: String
  let updatedAt: String
  let images: [String]?
  let likeCount: Int
  let favoriteCount: Int
  let commentCount: Int
  let viewCount: Int
  let isLiked: Bool
  let isFavorited: Bool
  let savedAt: String?
  let author: UserDto?
  let product: PostProductDto?
  let promotion: PromotionMetaDto?
  let isVisible: Bool
  let latitude: Double?
  let longitude: Double?
  let location: String?

  init(
    id: Int,
    title: String,
    category: String = defaultPostCategory,
    content: String,
    authorId: Int,
    createdAt: String,
    updatedAt: String,
    images: [String]? = nil,
    likeCount: Int = 0,
    favoriteCount: Int = 0,
    commentCount: Int = 0,
    viewCount: Int = 0,
    isLiked: Bool = false,
    isFavorited: Bool = false,
    savedAt: String? = nil,
    author: UserDto? = nil,
    product: PostProductDto? = nil,
    promotion: PromotionMetaDto? = nil,
    isVisible: Bool = true,
    latitude: Double? = nil,
    longitude: Double? = nil,
    location: String? = nil
  ) {
    self.id = id
    self.title = title
    self.category = category
    self.content = content
    self.authorId = authorId
    self.createdAt = createdAt
    self.updatedAt = updatedAt
    self.images = images
    self.likeCount = likeCount
    self.favoriteCount = favoriteCount
    self.commentCount = commentCount
    self.viewCount = viewCount
    self.isLiked = isLiked
    self.isFavorited = isFavorited
    self.savedAt = savedAt
    self.author = author
    self.product = product
    self.promotion = promotion
    self.isVisible = isVisible
    self.latitude = latitude
    self.longitude = longitude
    self.location = location
  }
}

struct PostProductDto: Codable, Equatable, Sendable {
  let id: Int
  let postId: Int
  let price: String
  let originalPrice: String?
  let category: String
  let condition: String
  let deliveryMethod: String
  let status: String
  let buyerId: Int?
  let soldAt: String?

  var isOnSale: Bool { status.caseInsensitiveCompare("ON_SALE") == .orderedSame }
  var isSold: Bool { status.caseInsensitiveCompare("SOLD") == .orderedSame }
}

struct PostProductRequest: Codable, Sendable {
  let price: Double
  let originalPrice: Double?
  let category: String?
  let condition: String?
  let deliveryMethod: String?
}

struct PostCommentDto: Codable, Identifiable, Equatable, Sendable {
  let id: Int
  let postId: Int
  let userId: Int
  let content: String
  let parentId: Int?
  let rootId: Int?
  let likeCount: Int
  let isLiked: Bool
  let createdAt: String
  let author: UserDto?
  let replyCount: Int?
  let previewReplies: [PostCommentDto]?
}

struct CommentLikeStateDto: Codable, Sendable {
  let commentId: Int
  let likeCount: Int
  let isLiked: Bool
}

struct CreatePostCommentRequest: Codable, Sendable {
  let content: String
  let parentId: Int?
}

struct PostSocialStateDto: Codable, Sendable {
  let likeCount: Int
  let favoriteCount: Int
  let commentCount: Int
  let isLiked: Bool
  let isFavorited: Bool
}

struct ActivitySocialStateDto: Codable, Sendable {
  let likeCount: Int
  let favoriteCount: Int
  let isLiked: Bool
  let isFavorited: Bool
}

struct PostFeedResponse: Codable, Sendable {
  let items: [PostDto]
  let total: Int
  let page: Int
  let limit: Int
  let hasMore: Bool
}

struct PaginatedResponse<T: Codable & Sendable>: Codable, Sendable where T: Sendable {
  let items: [T]
  let total: Int
  let page: Int
  let limit: Int
  let hasMore: Bool
}

struct CreatePostRequest: Codable, Sendable {
  let title: String?
  let category: String?
  let content: String?
  let images: [String]?
  let product: PostProductRequest?
  let latitude: Double?
  let longitude: Double?
  let location: String?
}

struct UpdatePostRequest: Codable, Sendable {
  let title: String?
  let category: String?
  let content: String?
  let images: [String]?
  let product: PostProductRequest?
  let latitude: Double?
  let longitude: Double?
  let location: String?
}

struct UploadImageResponse: Codable, Sendable {
  let url: String
}

// MARK: - Activities

struct ActivityDto: Codable, Identifiable, Equatable, Sendable {
  let id: Int
  let title: String
  let description: String?
  let images: [String]?
  let location: String
  let latitude: Double?
  let longitude: Double?
  let startTime: String
  let endTime: String
  let maxParticipants: Int
  let fee: String?
  let status: String
  let authorId: Int
  let createdAt: String
  let updatedAt: String
  let author: UserDto?
  let participants: [ActivityParticipantDto]?
  let isJoined: Bool
  let likeCount: Int
  let favoriteCount: Int
  let isLiked: Bool
  let isFavorited: Bool
  let savedAt: String?
  let promotion: PromotionMetaDto?

  init(
    id: Int,
    title: String,
    description: String? = nil,
    images: [String]? = nil,
    location: String,
    latitude: Double? = nil,
    longitude: Double? = nil,
    startTime: String,
    endTime: String,
    maxParticipants: Int,
    fee: String? = nil,
    status: String,
    authorId: Int,
    createdAt: String,
    updatedAt: String,
    author: UserDto? = nil,
    participants: [ActivityParticipantDto]? = nil,
    isJoined: Bool = false,
    likeCount: Int = 0,
    favoriteCount: Int = 0,
    isLiked: Bool = false,
    isFavorited: Bool = false,
    savedAt: String? = nil,
    promotion: PromotionMetaDto? = nil
  ) {
    self.id = id
    self.title = title
    self.description = description
    self.images = images
    self.location = location
    self.latitude = latitude
    self.longitude = longitude
    self.startTime = startTime
    self.endTime = endTime
    self.maxParticipants = maxParticipants
    self.fee = fee
    self.status = status
    self.authorId = authorId
    self.createdAt = createdAt
    self.updatedAt = updatedAt
    self.author = author
    self.participants = participants
    self.isJoined = isJoined
    self.likeCount = likeCount
    self.favoriteCount = favoriteCount
    self.isLiked = isLiked
    self.isFavorited = isFavorited
    self.savedAt = savedAt
    self.promotion = promotion
  }

  init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    id = try container.decode(Int.self, forKey: .id)
    title = try container.decode(String.self, forKey: .title)
    description = try container.decodeIfPresent(String.self, forKey: .description)
    images = try container.decodeIfPresent([String].self, forKey: .images)
    location = try container.decode(String.self, forKey: .location)
    latitude = try container.decodeIfPresent(Double.self, forKey: .latitude)
    longitude = try container.decodeIfPresent(Double.self, forKey: .longitude)
    startTime = try container.decode(String.self, forKey: .startTime)
    endTime = try container.decode(String.self, forKey: .endTime)
    maxParticipants = try container.decode(Int.self, forKey: .maxParticipants)
    fee = try container.decodeIfPresent(String.self, forKey: .fee)
    status = try container.decode(String.self, forKey: .status)
    authorId = try container.decode(Int.self, forKey: .authorId)
    createdAt = try container.decode(String.self, forKey: .createdAt)
    updatedAt = try container.decode(String.self, forKey: .updatedAt)
    author = try container.decodeIfPresent(UserDto.self, forKey: .author)
    participants = try container.decodeIfPresent([ActivityParticipantDto].self, forKey: .participants)
    isJoined = try container.decodeIfPresent(Bool.self, forKey: .isJoined) ?? false
    likeCount = try container.decodeIfPresent(Int.self, forKey: .likeCount) ?? 0
    favoriteCount = try container.decodeIfPresent(Int.self, forKey: .favoriteCount) ?? 0
    isLiked = try container.decodeIfPresent(Bool.self, forKey: .isLiked) ?? false
    isFavorited = try container.decodeIfPresent(Bool.self, forKey: .isFavorited) ?? false
    savedAt = try container.decodeIfPresent(String.self, forKey: .savedAt)
    promotion = try container.decodeIfPresent(PromotionMetaDto.self, forKey: .promotion)
  }

  var feeAmount: Double { Double(fee ?? "0") ?? 0 }
  var isPaidActivity: Bool { feeAmount > 0 }
  var promotionBadge: String? {
    guard promotion?.isActive == true else { return nil }
    return promotion?.badge
  }
}

struct ActivityParticipantDto: Codable, Identifiable, Equatable, Sendable {
  let id: Int
  let activityId: Int
  let userId: Int
  let joinedAt: String
  let user: UserDto?
  let activity: ActivityDto?
}

struct CreateActivityRequest: Codable, Sendable {
  let title: String?
  let description: String?
  let images: [String]?
  let location: String
  let latitude: Double?
  let longitude: Double?
  let startTime: String
  let endTime: String
  let maxParticipants: Int
  let fee: Double?
}

struct UpdateActivityRequest: Codable, Sendable {
  let title: String?
  let description: String?
  let images: [String]?
  let location: String?
  let latitude: Double?
  let longitude: Double?
  let startTime: String?
  let endTime: String?
  let maxParticipants: Int?
  let fee: Double?
}

// MARK: - Promotion

struct PromotionMetaDto: Codable, Equatable, Sendable {
  let isActive: Bool
  let label: String?
  let badge: String?
  let until: String?
  let canApply: Bool
  let nextAvailableAt: String?
  let cooldownHours: Int
  let durationHours: Int
  let requiresPayment: Bool
  let price: String?
  let minBidAmount: String?
  let bidAmount: String?
}

struct PromotionConfigDto: Codable, Sendable {
  let postBoostHours: Int
  let postCooldownHours: Int
  let postActiveLabel: String
  let postActionLabel: String
  let activityPromoteHours: Int
  let activityCooldownHours: Int
  let activityMaxFeedSlots: Int
  let activityActiveLabel: String
  let activityActionLabel: String
  let postMaxFeedSlots: Int
  let paidEnabled: Bool
  let postBoostPrice: String
  let activityPromotePrice: String
  let bidIncrement: String
  let lowestPostBoostBid: String?
  let lowestActivityPromoteBid: String?
  let postSlotsFull: Bool
  let activitySlotsFull: Bool
}

struct PromotionOrderRequest: Codable, Sendable {
  let bidAmount: String?

  init(bidAmount: String? = nil) {
    self.bidAmount = bidAmount
  }
}

struct PromotionActionResponseDto: Codable, Sendable {
  let message: String
  let promotion: PromotionMetaDto
}

struct PromotionPaymentResponseDto: Codable, Sendable {
  let message: String
  let payment: PaymentOrderResultDto
  let promotion: PromotionMetaDto
}

// MARK: - Payment

struct PaymentOrderResultDto: Codable, Sendable {
  let outTradeNo: String
  let channel: String
  let amount: String
  let platformFee: String
  let payeeAmount: String
  let platformFeeRateLabel: String
  let subject: String
  let status: String
  let payeeId: Int
  let alipay: AlipayPayPayloadDto?

  var alipayOrderStr: String { alipay?.orderStr ?? "" }
}

struct PaymentConfigDto: Codable, Sendable {
  let platformFeeRate: Double
  let platformFeeRateLabel: String
  let platformFeeMin: Double
  let autoConfirmDays: Int
  let alipaySandboxMode: Bool
}

struct AlipayPayPayloadDto: Codable, Sendable {
  let orderStr: String
}

struct AlipayAuthInfoDto: Codable, Sendable {
  let authInfo: String
}

struct BindAlipayOAuthRequest: Codable, Sendable {
  let authCode: String
}

struct PaymentOrderDetailDto: Codable, Sendable {
  let outTradeNo: String
  let channel: String
  let amount: String
  let platformFee: String?
  let payeeAmount: String?
  let subject: String
  let status: String
  let bizType: String
  let bizId: Int
  let payeeId: Int
  let tradeNo: String?
  let paidAt: String?
  let confirmedAt: String?
  let settledAt: String?
  let autoConfirmAt: String?
  let canConfirmReceipt: Bool
  let createdAt: String
}

struct FulfillmentStepDto: Codable, Equatable, Sendable {
  let label: String
  let done: Bool
  let active: Bool
}

struct OrderFulfillmentGuaranteeDto: Codable, Equatable, Sendable {
  let title: String
  let summary: String
  let steps: [FulfillmentStepDto]
}

struct PaymentOrderListItemDto: Codable, Identifiable, Sendable {
  var id: String { outTradeNo }
  let outTradeNo: String
  let amount: String
  let subject: String
  let status: String
  let statusLabel: String
  let bizType: String
  let bizId: Int
  let bizTitle: String
  let coverImage: String?
  let payeeId: Int
  let payeeName: String
  let payeeRoleLabel: String
  let payeeAvatarUrl: String?
  let platformFee: String?
  let payeeAmount: String?
  let paidAt: String?
  let confirmedAt: String?
  let settledAt: String?
  let autoConfirmAt: String?
  let bizStartTime: String?
  let bizEndTime: String?
  let bizLocation: String?
  let fulfillment: OrderFulfillmentGuaranteeDto?
  let createdAt: String
  let canPay: Bool
  let canConfirmReceipt: Bool
  let canReview: Bool
  let canApplyAfterSales: Bool
  let hasReview: Bool
  let afterSalesStatus: String?
}

struct PaymentOrderTabCountsDto: Codable, Sendable {
  let all: Int
  let pendingPayment: Int
  let awaitingReceipt: Int
  let review: Int
  let afterSales: Int
}

struct CreateOrderReviewRequest: Codable, Sendable {
  let rating: Int
  let content: String?
}

struct ApplyAfterSalesRequest: Codable, Sendable {
  let reason: String
}

struct PaymentTransactionItemDto: Codable, Identifiable, Sendable {
  var id: String { txKey }
  let txKey: String
  let type: String
  let typeLabel: String
  let direction: String
  let amount: String
  let outTradeNo: String
  let tradeNo: String?
  let subject: String
  let bizType: String
  let bizId: Int
  let bizTitle: String
  let coverImage: String?
  let counterpartyName: String
  let occurredAt: String
}

// MARK: - Profile

struct UserProfileDto: Codable, Identifiable, Sendable {
  let id: Int
  let lfcNo: String
  let studentId: String
  let nickname: String?
  let bio: String?
  let avatarUrl: String?
  let coverUrl: String?
  let postCount: Int
  let followingCount: Int
  let followerCount: Int
  let likeAndFavoriteCount: Int
  let isFollowing: Bool
  let isSelf: Bool
  let showCommentsPublic: Bool
  let showFavoritesPublic: Bool
  let showLikesPublic: Bool
  let alipayBound: Bool
  let alipayLoginIdMasked: String?
  let posts: [PostDto]
  let activities: [ActivityDto]
  let participationCount: Int

  init(
    id: Int,
    lfcNo: String,
    studentId: String,
    nickname: String?,
    bio: String?,
    avatarUrl: String?,
    coverUrl: String?,
    postCount: Int,
    followingCount: Int,
    followerCount: Int,
    likeAndFavoriteCount: Int,
    isFollowing: Bool,
    isSelf: Bool,
    showCommentsPublic: Bool,
    showFavoritesPublic: Bool,
    showLikesPublic: Bool,
    alipayBound: Bool = false,
    alipayLoginIdMasked: String? = nil,
    posts: [PostDto] = [],
    activities: [ActivityDto] = [],
    participationCount: Int = 0
  ) {
    self.id = id
    self.lfcNo = lfcNo
    self.studentId = studentId
    self.nickname = nickname
    self.bio = bio
    self.avatarUrl = avatarUrl
    self.coverUrl = coverUrl
    self.postCount = postCount
    self.followingCount = followingCount
    self.followerCount = followerCount
    self.likeAndFavoriteCount = likeAndFavoriteCount
    self.isFollowing = isFollowing
    self.isSelf = isSelf
    self.showCommentsPublic = showCommentsPublic
    self.showFavoritesPublic = showFavoritesPublic
    self.showLikesPublic = showLikesPublic
    self.alipayBound = alipayBound
    self.alipayLoginIdMasked = alipayLoginIdMasked
    self.posts = posts
    self.activities = activities
    self.participationCount = participationCount
  }

  init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    id = try container.decode(Int.self, forKey: .id)
    lfcNo = try container.decode(String.self, forKey: .lfcNo)
    studentId = try container.decode(String.self, forKey: .studentId)
    nickname = try container.decodeIfPresent(String.self, forKey: .nickname)
    bio = try container.decodeIfPresent(String.self, forKey: .bio)
    avatarUrl = try container.decodeIfPresent(String.self, forKey: .avatarUrl)
    coverUrl = try container.decodeIfPresent(String.self, forKey: .coverUrl)
    postCount = try container.decode(Int.self, forKey: .postCount)
    followingCount = try container.decodeIfPresent(Int.self, forKey: .followingCount) ?? 0
    followerCount = try container.decodeIfPresent(Int.self, forKey: .followerCount) ?? 0
    likeAndFavoriteCount = try container.decodeIfPresent(Int.self, forKey: .likeAndFavoriteCount) ?? 0
    isFollowing = try container.decodeIfPresent(Bool.self, forKey: .isFollowing) ?? false
    isSelf = try container.decodeIfPresent(Bool.self, forKey: .isSelf) ?? false
    showCommentsPublic = try container.decodeIfPresent(Bool.self, forKey: .showCommentsPublic) ?? false
    showFavoritesPublic = try container.decodeIfPresent(Bool.self, forKey: .showFavoritesPublic) ?? false
    showLikesPublic = try container.decodeIfPresent(Bool.self, forKey: .showLikesPublic) ?? false
    alipayBound = try container.decodeIfPresent(Bool.self, forKey: .alipayBound) ?? false
    alipayLoginIdMasked = try container.decodeIfPresent(String.self, forKey: .alipayLoginIdMasked)
    posts = try container.decodeIfPresent([PostDto].self, forKey: .posts) ?? []
    activities = try container.decodeIfPresent([ActivityDto].self, forKey: .activities) ?? []
    participationCount = try container.decodeIfPresent(Int.self, forKey: .participationCount) ?? 0
  }

  var displayName: String {
    if let nickname, !nickname.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
      return nickname
    }
    return studentId
  }
}

struct BindAlipayAccountRequest: Codable, Sendable {
  let alipayLoginId: String
  let alipayRealName: String?
}

struct AlipayAccountBindingDto: Codable, Sendable {
  let alipayBound: Bool
  let alipayLoginIdMasked: String?
  let alipayRealName: String?
  let alipayBoundAt: String?
}

struct UpdateProfileRequest: Codable, Sendable {
  let nickname: String?
  let bio: String?
  let avatarUrl: String?
  let coverUrl: String?
  let showCommentsPublic: Bool?
  let showFavoritesPublic: Bool?
  let showLikesPublic: Bool?
}

struct FollowStateDto: Codable, Sendable {
  let isFollowing: Bool
}

struct FeedChannelCatalogDto: Codable, Sendable {
  let allChannels: [String]
  let defaultMyChannels: [String]
  let publishCategories: [String]
}

struct UserFeedChannelsDto: Codable, Sendable {
  let myChannels: [String]
  let allChannels: [String]
  let recommendedChannels: [String]
  let publishCategories: [String]
}

struct UpdateFeedChannelsRequest: Codable, Sendable {
  let channels: [String]
}

struct ProfileCommentDto: Codable, Identifiable, Sendable {
  let id: Int
  let postId: Int
  let userId: Int
  let content: String
  let parentId: Int?
  let createdAt: String
  let post: PostDto?
}

// MARK: - Notifications & chat

struct NotificationDto: Codable, Identifiable, Sendable {
  let id: Int
  let userId: Int
  let title: String
  let content: String
  let type: String
  let relatedType: String?
  let relatedId: Int?
  let isRead: Bool
  let createdAt: String
}

struct ConversationDto: Codable, Identifiable, Sendable {
  let id: Int
  let peerUserId: Int
  let peerStudentId: String
  let peerNickname: String?
  let peerAvatarUrl: String?
  let lastMessageContent: String?
  let lastMessageAt: String?
  let unreadCount: Int
  let createdAt: String
  let updatedAt: String

  var peerDisplayName: String {
    if let peerNickname, !peerNickname.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
      return peerNickname
    }
    return peerStudentId
  }
}

struct ChatMessageDto: Codable, Identifiable, Sendable {
  let id: Int
  let conversationId: Int
  let senderId: Int
  let content: String
  let messageType: String
  let isRead: Bool
  let createdAt: String
  let sender: UserDto?
}

struct CreateConversationRequest: Codable, Sendable {
  let peerUserId: Int
}

struct SendChatMessageRequest: Codable, Sendable {
  let content: String
  let messageType: String?
}

struct UnreadCountDto: Codable, Sendable {
  let count: Int
}

// MARK: - Geocode

struct PlaceSuggestionDto: Codable, Identifiable, Equatable, Sendable {
  let id: String
  let name: String
  let address: String
  let latitude: Double
  let longitude: Double
  let district: String
}

struct PlaceSearchResponse: Codable, Sendable {
  let items: [PlaceSuggestionDto]
  let hasMore: Bool
}

struct ReverseGeocodeResult: Codable, Sendable {
  let address: String
  let latitude: Double
  let longitude: Double
}

// MARK: - Analytics

struct AnalyticsEventInput: Codable, Sendable {
  let event: String
  let properties: [String: JSONValue]?
  let platform: String?
  let sessionId: String?
  let occurredAt: String?
}

struct IngestAnalyticsEventsRequest: Codable, Sendable {
  let events: [AnalyticsEventInput]
}

struct IngestAnalyticsEventsResponse: Codable, Sendable {
  let accepted: Int
}

// MARK: - Chat product payload

struct ChatProductPayload: Codable, Sendable {
  let postId: Int
  let title: String
  let price: String
  let coverUrl: String?
  let status: String?
  let category: String?
}

extension PostDto {
  var promotionBadge: String? {
    guard promotion?.isActive == true else { return nil }
    return promotion?.badge
  }

  var hasOnSaleProduct: Bool { product?.isOnSale == true }

  func productDisplayTitle() -> String {
    let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
    let trimmedContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
    let titleLooksWeak =
      trimmedTitle.isEmpty
      || ["图片笔记", "校园笔记"].contains(trimmedTitle)
      || trimmedTitle == String(trimmedContent.prefix(30))
      // Only treat short ASCII slugs as weak (e.g. "test"), not short Chinese titles like "出去玩".
      || (trimmedTitle.count <= 4 && trimmedTitle.allSatisfy { $0.isASCII && ($0.isLetter || $0.isNumber) })

    if !titleLooksWeak {
      return trimmedTitle
    }
    if !trimmedContent.isEmpty {
      return trimmedContent.split(separator: "\n", maxSplits: 1).first.map(String.init) ?? trimmedContent
    }
    return "\(postProductCategoryLabel(product?.category))好物"
  }

  func toChatProductPayload() -> ChatProductPayload? {
    guard let product else { return nil }
    return ChatProductPayload(
      postId: id,
      title: productDisplayTitle(),
      price: product.price,
      coverUrl: images?.first,
      status: product.status,
      category: product.category
    )
  }
}

let orderCenterTabs: [(String, String)] = [
  ("all", "全部"),
  ("pending_payment", "待付款"),
  ("awaiting_receipt", "待履约"),
  ("review", "评价"),
  ("after_sales", "售后"),
]

// MARK: - UI compatibility typealiases

typealias PostDTO = PostDto
typealias ActivityDTO = ActivityDto
typealias UserDTO = UserDto
typealias UserProfileDTO = UserProfileDto
typealias NotificationDTO = NotificationDto
typealias ConversationDTO = ConversationDto
typealias ChatMessageDTO = ChatMessageDto
typealias UnreadCountDTO = UnreadCountDto

extension PostDto: Hashable {
  static func == (lhs: PostDto, rhs: PostDto) -> Bool { lhs.id == rhs.id }
  func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension ActivityDto: Hashable {
  static func == (lhs: ActivityDto, rhs: ActivityDto) -> Bool { lhs.id == rhs.id }
  func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension UserDto: Hashable {
  static func == (lhs: UserDto, rhs: UserDto) -> Bool { lhs.id == rhs.id }
  func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension UserProfileDto: Hashable {
  static func == (lhs: UserProfileDto, rhs: UserProfileDto) -> Bool { lhs.id == rhs.id }
  func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension NotificationDto: Hashable {
  static func == (lhs: NotificationDto, rhs: NotificationDto) -> Bool { lhs.id == rhs.id }
  func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension ConversationDto: Hashable {
  static func == (lhs: ConversationDto, rhs: ConversationDto) -> Bool { lhs.id == rhs.id }
  func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension ChatMessageDto: Hashable {
  static func == (lhs: ChatMessageDto, rhs: ChatMessageDto) -> Bool { lhs.id == rhs.id }
  func hash(into hasher: inout Hasher) { hasher.combine(id) }
}
