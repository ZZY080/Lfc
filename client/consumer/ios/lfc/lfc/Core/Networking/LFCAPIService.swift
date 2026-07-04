import Foundation

/// Consumer API surface matching Android `LfcApiService.kt`.
final class LFCAPIService: Sendable {
  static let shared = LFCAPIService()

  private let client: APIClient

  init(client: APIClient = .shared) {
    self.client = client
  }

  // MARK: - Auth

  func login(_ request: LoginRequest) async throws -> AuthResponse {
    try await client.request(method: .post, path: "consumer/auth/login", body: request, requiresAuth: false)
  }

  func refreshToken(_ request: RefreshTokenRequest) async throws -> AuthResponse {
    try await client.request(
      method: .post,
      path: "consumer/auth/refresh",
      body: request,
      requiresAuth: false,
      allowRefreshRetry: false
    )
  }

  func register(
    email: String,
    password: String,
    studentId: String,
    realName: String,
    studentCardData: Data,
    studentCardFilename: String,
    studentCardMimeType: String = "image/jpeg"
  ) async throws -> AuthResponse {
    try await client.uploadMultipart(
      path: "consumer/auth/register",
      parts: [
        MultipartFormPart(name: "email", payload: .text(email)),
        MultipartFormPart(name: "password", payload: .text(password)),
        MultipartFormPart(name: "studentId", payload: .text(studentId)),
        MultipartFormPart(name: "realName", payload: .text(realName)),
        MultipartFormPart(
          name: "studentCard",
          payload: .data(studentCardData, filename: studentCardFilename, mimeType: studentCardMimeType)
        ),
      ],
      requiresAuth: false
    )
  }

  // MARK: - Posts

  func getPosts() async throws -> [PostDto] {
    try await client.request(method: .get, path: "consumer/post")
  }

  func getPostFeed(
    page: Int,
    limit: Int,
    sort: String? = nil,
    keyword: String? = nil,
    tab: String? = nil,
    city: String? = nil
  ) async throws -> PostFeedResponse {
    try await client.request(
      method: .get,
      path: "consumer/post/feed",
      queryItems: queryItems([
        ("page", "\(page)"),
        ("limit", "\(limit)"),
        ("sort", sort),
        ("keyword", keyword),
        ("tab", tab),
        ("city", city),
      ])
    )
  }

  func getMyPosts(keyword: String? = nil) async throws -> [PostDto] {
    try await client.request(
      method: .get,
      path: "consumer/post/mine",
      queryItems: queryItems([("keyword", keyword)])
    )
  }

  func getPost(id: Int) async throws -> PostDto {
    try await client.request(method: .get, path: "consumer/post/\(id)")
  }

  func togglePostLike(id: Int) async throws -> PostSocialStateDto {
    try await client.request(method: .post, path: "consumer/post/\(id)/like")
  }

  func togglePostFavorite(id: Int) async throws -> PostSocialStateDto {
    try await client.request(method: .post, path: "consumer/post/\(id)/favorite")
  }

  func getPostComments(
    id: Int,
    page: Int = 1,
    limit: Int = 20,
    sort: String = "default"
  ) async throws -> PaginatedResponse<PostCommentDto> {
    try await client.request(
      method: .get,
      path: "consumer/post/\(id)/comments",
      queryItems: queryItems([
        ("page", "\(page)"),
        ("limit", "\(limit)"),
        ("sort", sort),
      ])
    )
  }

  func getPostCommentReplies(
    postId: Int,
    commentId: Int,
    page: Int = 1,
    limit: Int = 20
  ) async throws -> PaginatedResponse<PostCommentDto> {
    try await client.request(
      method: .get,
      path: "consumer/post/\(postId)/comments/\(commentId)/replies",
      queryItems: queryItems([
        ("page", "\(page)"),
        ("limit", "\(limit)"),
      ])
    )
  }

  func createPostComment(id: Int, request: CreatePostCommentRequest) async throws -> PostCommentDto {
    try await client.request(method: .post, path: "consumer/post/\(id)/comments", body: request)
  }

  func deletePostComment(commentId: Int) async throws {
    try await client.requestVoid(method: .delete, path: "consumer/post/comments/\(commentId)")
  }

  func toggleCommentLike(commentId: Int) async throws -> CommentLikeStateDto {
    try await client.request(method: .post, path: "consumer/post/comments/\(commentId)/like")
  }

  func createPost(_ request: CreatePostRequest) async throws -> PostDto {
    try await client.request(method: .post, path: "consumer/post", body: request)
  }

  func updatePost(id: Int, request: UpdatePostRequest) async throws -> PostDto {
    try await client.request(method: .patch, path: "consumer/post/\(id)", body: request)
  }

  func deletePost(id: Int) async throws {
    try await client.requestVoid(method: .delete, path: "consumer/post/\(id)")
  }

  func offShelfPost(id: Int) async throws -> PostDto {
    try await client.request(method: .patch, path: "consumer/post/\(id)/off-shelf")
  }

  func onShelfPost(id: Int) async throws -> PostDto {
    try await client.request(method: .patch, path: "consumer/post/\(id)/on-shelf")
  }

  func offShelfPostProduct(id: Int) async throws {
    try await client.requestVoid(method: .patch, path: "consumer/post/\(id)/product/off-shelf")
  }

  func onShelfPostProduct(id: Int) async throws {
    try await client.requestVoid(method: .patch, path: "consumer/post/\(id)/product/on-shelf")
  }

  // MARK: - Upload

  func uploadImage(
    data: Data,
    filename: String,
    mimeType: String,
    scope: String? = nil
  ) async throws -> UploadImageResponse {
    try await client.uploadMultipart(
      path: "consumer/upload/image",
      queryItems: queryItems([("scope", scope)]),
      parts: [
        MultipartFormPart(name: "file", payload: .data(data, filename: filename, mimeType: mimeType)),
      ]
    )
  }

  func uploadVideo(
    data: Data,
    filename: String,
    mimeType: String,
    scope: String? = nil
  ) async throws -> UploadImageResponse {
    try await client.uploadMultipart(
      path: "consumer/upload/video",
      queryItems: queryItems([("scope", scope)]),
      parts: [
        MultipartFormPart(name: "file", payload: .data(data, filename: filename, mimeType: mimeType)),
      ]
    )
  }

  // MARK: - User / profile

  func getMyProfile() async throws -> UserProfileDto {
    try await client.request(method: .get, path: "consumer/user/me")
  }

  func updateMyProfile(_ request: UpdateProfileRequest) async throws -> UserProfileDto {
    try await client.request(method: .patch, path: "consumer/user/me", body: request)
  }

  func getFeedChannelCatalog() async throws -> FeedChannelCatalogDto {
    try await client.request(method: .get, path: "consumer/feed-channels")
  }

  func getMyFeedChannels() async throws -> UserFeedChannelsDto {
    try await client.request(method: .get, path: "consumer/user/me/feed-channels")
  }

  func updateMyFeedChannels(_ request: UpdateFeedChannelsRequest) async throws -> UserFeedChannelsDto {
    try await client.request(method: .patch, path: "consumer/user/me/feed-channels", body: request)
  }

  func getMyAlipayAccount() async throws -> AlipayAccountBindingDto {
    try await client.request(method: .get, path: "consumer/user/me/alipay")
  }

  func getAlipayOAuthAuthInfo() async throws -> AlipayAuthInfoDto {
    try await client.request(method: .get, path: "consumer/user/me/alipay/auth-info")
  }

  func bindMyAlipayByOAuth(_ request: BindAlipayOAuthRequest) async throws -> AlipayAccountBindingDto {
    try await client.request(method: .post, path: "consumer/user/me/alipay/oauth", body: request)
  }

  func bindMyAlipayAccount(_ request: BindAlipayAccountRequest) async throws -> AlipayAccountBindingDto {
    try await client.request(method: .put, path: "consumer/user/me/alipay", body: request)
  }

  func unbindMyAlipayAccount() async throws -> AlipayAccountBindingDto {
    try await client.request(method: .delete, path: "consumer/user/me/alipay")
  }

  func getMyFavoritePosts(page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<PostDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/me/favorites",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getMyLikedPosts(page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<PostDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/me/likes",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getMyFavoriteActivities(page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<ActivityDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/me/favorite-activities",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getMyLikedActivities(page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<ActivityDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/me/liked-activities",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getMyProfileComments(page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<ProfileCommentDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/me/comments",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getUserPosts(id: Int, page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<PostDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/\(id)/posts",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getUserActivities(id: Int, page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<ActivityDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/\(id)/activities",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getUserFavoritePosts(id: Int, page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<PostDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/\(id)/favorites",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getUserLikedPosts(id: Int, page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<PostDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/\(id)/likes",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getUserFavoriteActivities(id: Int, page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<ActivityDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/\(id)/favorite-activities",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getUserLikedActivities(id: Int, page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<ActivityDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/\(id)/liked-activities",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getUserProfileComments(id: Int, page: Int = 1, limit: Int = 10) async throws -> PaginatedResponse<ProfileCommentDto> {
    try await client.request(
      method: .get,
      path: "consumer/user/\(id)/comments",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func toggleFollow(id: Int) async throws -> FollowStateDto {
    try await client.request(method: .post, path: "consumer/user/\(id)/follow")
  }

  func getUserProfileByLfcNo(lfcNo: String) async throws -> UserProfileDto {
    try await client.request(method: .get, path: "consumer/user/lfc/\(lfcNo)/profile")
  }

  func getUserProfile(id: Int) async throws -> UserProfileDto {
    try await client.request(method: .get, path: "consumer/user/\(id)/profile")
  }

  // MARK: - Activities

  func getActivityFeed(
    page: Int,
    limit: Int,
    keyword: String? = nil,
    latitude: Double? = nil,
    longitude: Double? = nil,
    sort: String? = nil
  ) async throws -> PaginatedResponse<ActivityDto> {
    var items: [(String, String?)] = [
      ("page", "\(page)"),
      ("limit", "\(limit)"),
      ("keyword", keyword),
      ("sort", sort),
    ]
    if let latitude {
      items.append(("latitude", "\(latitude)"))
    }
    if let longitude {
      items.append(("longitude", "\(longitude)"))
    }
    return try await client.request(
      method: .get,
      path: "consumer/activity/feed",
      queryItems: queryItems(items)
    )
  }

  func getApprovedActivities() async throws -> [ActivityDto] {
    try await client.request(method: .get, path: "consumer/activity")
  }

  func getMyActivities() async throws -> [ActivityDto] {
    try await client.request(method: .get, path: "consumer/activity/mine")
  }

  func getActivity(id: Int) async throws -> ActivityDto {
    try await client.request(method: .get, path: "consumer/activity/\(id)")
  }

  func getMyParticipations() async throws -> [ActivityParticipantDto] {
    try await client.request(method: .get, path: "consumer/activity/participations/mine")
  }

  func createActivity(_ request: CreateActivityRequest) async throws -> ActivityDto {
    try await client.request(method: .post, path: "consumer/activity", body: request)
  }

  func updateActivity(id: Int, request: UpdateActivityRequest) async throws -> ActivityDto {
    try await client.request(method: .patch, path: "consumer/activity/\(id)", body: request)
  }

  func deleteActivity(id: Int) async throws {
    try await client.requestVoid(method: .delete, path: "consumer/activity/\(id)")
  }

  func offShelfActivity(id: Int) async throws -> ActivityDto {
    try await client.request(method: .patch, path: "consumer/activity/\(id)/off-shelf")
  }

  func onShelfActivity(id: Int) async throws -> ActivityDto {
    try await client.request(method: .patch, path: "consumer/activity/\(id)/on-shelf")
  }

  func joinActivity(id: Int) async throws -> ActivityParticipantDto {
    try await client.request(method: .post, path: "consumer/activity/\(id)/join")
  }

  func toggleActivityLike(id: Int) async throws -> ActivitySocialStateDto {
    try await client.request(method: .post, path: "consumer/activity/\(id)/like")
  }

  func toggleActivityFavorite(id: Int) async throws -> ActivitySocialStateDto {
    try await client.request(method: .post, path: "consumer/activity/\(id)/favorite")
  }

  func leaveActivity(id: Int) async throws {
    try await client.requestVoid(method: .delete, path: "consumer/activity/\(id)/join")
  }

  // MARK: - Promotion

  func getPromotionConfig() async throws -> PromotionConfigDto {
    try await client.request(method: .get, path: "consumer/promotion/config")
  }

  func boostPost(postId: Int) async throws -> PromotionActionResponseDto {
    try await client.request(method: .post, path: "consumer/promotion/post/\(postId)/boost")
  }

  func createPostBoostOrder(postId: Int, request: PromotionOrderRequest = PromotionOrderRequest()) async throws -> PromotionPaymentResponseDto {
    try await client.request(method: .post, path: "consumer/promotion/post/\(postId)/boost/order", body: request)
  }

  func promoteActivity(activityId: Int) async throws -> PromotionActionResponseDto {
    try await client.request(method: .post, path: "consumer/promotion/activity/\(activityId)/promote")
  }

  func createActivityPromoteOrder(
    activityId: Int,
    request: PromotionOrderRequest = PromotionOrderRequest()
  ) async throws -> PromotionPaymentResponseDto {
    try await client.request(
      method: .post,
      path: "consumer/promotion/activity/\(activityId)/promote/order",
      body: request
    )
  }

  // MARK: - Payment

  func getPaymentConfig() async throws -> PaymentConfigDto {
    try await client.request(method: .get, path: "consumer/payment/config")
  }

  func getPaymentOrders(tab: String, page: Int, limit: Int) async throws -> PaginatedResponse<PaymentOrderListItemDto> {
    try await client.request(
      method: .get,
      path: "consumer/payment/orders",
      queryItems: queryItems([
        ("tab", tab),
        ("page", "\(page)"),
        ("limit", "\(limit)"),
      ])
    )
  }

  func getPaymentOrderTabCounts() async throws -> PaymentOrderTabCountsDto {
    try await client.request(method: .get, path: "consumer/payment/orders/counts")
  }

  func getPaymentTransactions(page: Int, limit: Int) async throws -> PaginatedResponse<PaymentTransactionItemDto> {
    try await client.request(
      method: .get,
      path: "consumer/payment/transactions",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func cancelPaymentOrder(outTradeNo: String) async throws -> PaymentOrderDetailDto {
    try await client.request(method: .post, path: "consumer/payment/orders/\(outTradeNo)/cancel")
  }

  func repayPaymentOrder(outTradeNo: String) async throws -> PaymentOrderResultDto {
    try await client.request(method: .post, path: "consumer/payment/orders/\(outTradeNo)/repay")
  }

  func createPaymentOrderReview(outTradeNo: String, request: CreateOrderReviewRequest) async throws -> JSONDictionary {
    try await client.request(
      method: .post,
      path: "consumer/payment/orders/\(outTradeNo)/reviews",
      body: request
    )
  }

  func applyPaymentAfterSales(outTradeNo: String, request: ApplyAfterSalesRequest) async throws -> JSONDictionary {
    try await client.request(
      method: .post,
      path: "consumer/payment/orders/\(outTradeNo)/after-sales",
      body: request
    )
  }

  func cancelPaymentAfterSales(outTradeNo: String) async throws -> JSONDictionary {
    try await client.request(method: .delete, path: "consumer/payment/orders/\(outTradeNo)/after-sales")
  }

  func createActivityPaymentOrder(activityId: Int) async throws -> PaymentOrderResultDto {
    try await client.request(method: .post, path: "consumer/payment/activity/\(activityId)/order")
  }

  func createPostProductOrder(postId: Int) async throws -> PaymentOrderResultDto {
    try await client.request(method: .post, path: "consumer/payment/post/\(postId)/order")
  }

  func createPostBoostPaymentOrder(postId: Int, request: PromotionOrderRequest) async throws -> PaymentOrderResultDto {
    try await client.request(method: .post, path: "consumer/payment/post/\(postId)/boost/order", body: request)
  }

  func createActivityPromotePaymentOrder(activityId: Int, request: PromotionOrderRequest) async throws -> PaymentOrderResultDto {
    try await client.request(
      method: .post,
      path: "consumer/payment/activity/\(activityId)/promote/order",
      body: request
    )
  }

  func getPostProductOrder(postId: Int) async throws -> PaymentOrderDetailDto? {
    try await client.requestOptional(method: .get, path: "consumer/payment/post/\(postId)/order")
  }

  func getPaymentOrder(outTradeNo: String) async throws -> PaymentOrderDetailDto {
    try await client.request(method: .get, path: "consumer/payment/orders/\(outTradeNo)")
  }

  func syncPaymentOrder(outTradeNo: String) async throws -> PaymentOrderDetailDto {
    try await client.request(method: .post, path: "consumer/payment/orders/\(outTradeNo)/sync")
  }

  func confirmPaymentReceipt(outTradeNo: String) async throws -> PaymentOrderDetailDto {
    try await client.request(method: .post, path: "consumer/payment/orders/\(outTradeNo)/confirm-receipt")
  }

  // MARK: - Notifications

  func getNotifications(page: Int, limit: Int) async throws -> PaginatedResponse<NotificationDto> {
    try await client.request(
      method: .get,
      path: "consumer/notification",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getNotificationUnreadCount() async throws -> UnreadCountDto {
    try await client.request(method: .get, path: "consumer/notification/unread-count")
  }

  func getNotification(id: Int) async throws -> NotificationDto {
    try await client.request(method: .get, path: "consumer/notification/\(id)")
  }

  func markAllNotificationsRead() async throws {
    try await client.requestVoid(method: .patch, path: "consumer/notification/read-all")
  }

  func deleteNotification(id: Int) async throws {
    try await client.requestVoid(method: .delete, path: "consumer/notification/\(id)")
  }

  // MARK: - Conversations / chat

  func getConversations(page: Int, limit: Int) async throws -> PaginatedResponse<ConversationDto> {
    try await client.request(
      method: .get,
      path: "consumer/conversation",
      queryItems: queryItems([("page", "\(page)"), ("limit", "\(limit)")])
    )
  }

  func getConversationUnreadCount() async throws -> UnreadCountDto {
    try await client.request(method: .get, path: "consumer/conversation/unread-count")
  }

  func createConversation(_ request: CreateConversationRequest) async throws -> ConversationDto {
    try await client.request(method: .post, path: "consumer/conversation", body: request)
  }

  func deleteConversation(id: Int) async throws {
    try await client.requestVoid(method: .delete, path: "consumer/conversation/\(id)")
  }

  func getChatMessages(id: Int) async throws -> [ChatMessageDto] {
    try await client.request(method: .get, path: "consumer/conversation/\(id)/messages")
  }

  func sendChatMessage(id: Int, request: SendChatMessageRequest) async throws -> ChatMessageDto {
    try await client.request(method: .post, path: "consumer/conversation/\(id)/messages", body: request)
  }

  // MARK: - Analytics

  func ingestAnalyticsEvents(_ request: IngestAnalyticsEventsRequest) async throws -> IngestAnalyticsEventsResponse {
    try await client.request(method: .post, path: "consumer/analytics/events", body: request)
  }

  // MARK: - Geocode

  func reverseGeocode(latitude: Double, longitude: Double) async throws -> ReverseGeocodeResult? {
    let result: ReverseGeocodeResult? = try await client.requestOptional(
      method: .get,
      path: "consumer/geocode/reverse",
      queryItems: queryItems([
        ("latitude", "\(latitude)"),
        ("longitude", "\(longitude)"),
      ]),
      requiresAuth: false
    )
    return result
  }

  func searchPlaces(
    keyword: String,
    page: Int = 1,
    limit: Int = 20,
    latitude: Double? = nil,
    longitude: Double? = nil,
    city: String? = nil
  ) async throws -> PlaceSearchResponse {
    let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
      return PlaceSearchResponse(items: [], hasMore: false)
    }

    var items: [(String, String?)] = [
      ("keyword", trimmed),
      ("page", "\(page)"),
      ("limit", "\(limit)"),
    ]
    if let latitude, let longitude {
      items.append(("latitude", "\(latitude)"))
      items.append(("longitude", "\(longitude)"))
    }
    if let city {
      let cityLabel = city.trimmingCharacters(in: .whitespacesAndNewlines)
      if !cityLabel.isEmpty, cityLabel != "同城" {
        items.append(("city", cityLabel))
      }
    }

    return try await client.request(
      method: .get,
      path: "consumer/geocode/search",
      queryItems: queryItems(items),
      requiresAuth: false
    )
  }

  // MARK: - Helpers

  func searchPosts(keyword: String, page: Int, limit: Int = 10) async throws -> PaginatedResponse<PostDto> {
    let feed = try await getPostFeed(page: page, limit: limit, keyword: keyword)
    return PaginatedResponse(
      items: feed.items,
      total: feed.total,
      page: feed.page,
      limit: feed.limit,
      hasMore: feed.hasMore
    )
  }

  func searchActivities(keyword: String, page: Int, limit: Int = 10) async throws -> PaginatedResponse<ActivityDto> {
    try await getActivityFeed(page: page, limit: limit, keyword: keyword)
  }

  func getUserProfile(userId: Int) async throws -> UserProfileDto {
    try await getUserProfile(id: userId)
  }

  func getConversation(id: Int) async throws -> ConversationDto {
    var page = 1
    repeat {
      let response = try await getConversations(page: page, limit: 50)
      if let found = response.items.first(where: { $0.id == id }) {
        return found
      }
      if !response.hasMore {
        break
      }
      page += 1
    } while page <= 20

    throw ApiError(statusCode: 404, message: "会话不存在", body: nil)
  }

  func getChatMessages(conversationId: Int, page: Int = 1) async throws -> PaginatedResponse<ChatMessageDto> {
    let messages = try await getChatMessages(id: conversationId)
    return PaginatedResponse(
      items: messages,
      total: messages.count,
      page: page,
      limit: max(messages.count, 1),
      hasMore: false
    )
  }

  private func queryItems(_ pairs: [(String, String?)]) -> [URLQueryItem]? {
    let items = pairs.compactMap { name, value -> URLQueryItem? in
      guard let value, !value.isEmpty else { return nil }
      return URLQueryItem(name: name, value: value)
    }
    return items.isEmpty ? nil : items
  }
}
