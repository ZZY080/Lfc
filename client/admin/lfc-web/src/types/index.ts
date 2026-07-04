export type UserRole = 'CONSUMER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'BANNED'

export type ActivityStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'OFF_SHELF'

export type PostStatus = ActivityStatus

export interface PaginatedResult<T> {
  items: T[]
  total: number
  page: number
  limit: number
  hasMore: boolean
}

export interface AuthUser {
  id: number
  email: string
  studentId: string
  role: UserRole
}

export interface AuthTokenResponse {
  accessToken: string
  refreshToken: string
  user: AuthUser
}

export interface ActivityAuthor {
  id: number
  email: string
  studentId: string
  realName: string
  nickname: string | null
}

export interface Activity {
  id: number
  title: string
  description: string
  images: string[] | null
  location: string
  latitude: number | null
  longitude: number | null
  startTime: string
  endTime: string
  maxParticipants: number
  fee: string
  status: ActivityStatus
  reviewComment: string | null
  authorId: number
  author?: ActivityAuthor
  participants?: ActivityParticipant[]
  createdAt: string
  updatedAt: string
}

export interface ActivityParticipant {
  id: number
  activityId: number
  userId: number
  joinedAt: string
  user?: ActivityAuthor
}

export interface User {
  id: number
  email: string
  studentId: string
  realName: string
  lfcNo: string | null
  nickname: string | null
  role: UserRole
  status: UserStatus
  banReason: string | null
  bannedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface PostAuthor {
  id: number
  email: string
  studentId: string
  realName: string
  nickname: string | null
}

export interface Post {
  id: number
  title: string
  category: string
  content: string
  images: string[] | null
  likeCount: number
  favoriteCount: number
  commentCount: number
  viewCount: number
  authorId: number
  isVisible: boolean
  status: PostStatus
  reviewComment: string | null
  location: string | null
  latitude: number | null
  longitude: number | null
  author?: PostAuthor
  createdAt: string
  updatedAt: string
}

export type PostProductCategory =
  | 'GENERAL'
  | 'SECOND_HAND'
  | 'DIGITAL'
  | 'BOOK'
  | 'DAILY'
  | 'CLOTHING'
  | 'FOOD'
  | 'BEAUTY'
  | 'SPORTS'
  | 'HANDMADE'
  | 'TICKET'
  | 'SERVICE'
  | 'OTHER'

export type ProductCondition = 'BRAND_NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR'

export type DeliveryMethod = 'PICKUP' | 'EXPRESS' | 'BOTH'

export type PostProductStatus = 'ON_SALE' | 'SOLD' | 'OFF_SHELF'

export interface PostProduct {
  id: number
  postId: number
  price: string
  originalPrice: string | null
  category: PostProductCategory
  condition: ProductCondition
  deliveryMethod: DeliveryMethod
  status: PostProductStatus
  buyerId: number | null
  soldAt: string | null
  createdAt: string
  updatedAt: string
}

export interface PostDetail extends Post {
  product: PostProduct | null
}

export interface PostRelationUser {
  id: number
  email: string
  studentId: string
  realName: string
  nickname: string | null
}

export interface PostLikeRecord {
  id: number
  postId: number
  userId: number
  user: PostRelationUser
  createdAt: string
}

export interface PostFavoriteRecord {
  id: number
  postId: number
  userId: number
  user: PostRelationUser
  createdAt: string
}

export interface CommentPostBrief {
  id: number
  title: string
}

export interface Comment {
  id: number
  postId: number
  userId: number
  content: string
  likeCount: number
  isVisible: boolean
  parentId: number | null
  createdAt: string
  author?: PostAuthor
  post?: CommentPostBrief
}

export type PaymentOrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'CONFIRMED'
  | 'SETTLED'
  | 'REFUNDED'
  | 'CLOSED'
  | 'FAILED'

export type PaymentBizType =
  | 'ACTIVITY_JOIN'
  | 'POST_PRODUCT_PURCHASE'
  | 'POST_BOOST'
  | 'ACTIVITY_PROMOTE'

export type PaymentChannel = 'ALIPAY' | 'WECHAT'

export interface PaymentUserBrief {
  id: number
  email: string
  realName: string
  nickname: string | null
}

export interface PaymentOrder {
  id: number
  outTradeNo: string
  amount: string
  platformFee: string
  payeeAmount: string
  subject: string
  status: PaymentOrderStatus
  bizType: PaymentBizType
  bizId: number
  channel: PaymentChannel
  userId: number
  payeeId: number
  tradeNo: string | null
  paidAt: string | null
  confirmedAt: string | null
  settledAt: string | null
  createdAt: string
  user: PaymentUserBrief | null
  payee: PaymentUserBrief | null
}

export type PaymentAfterSalesStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'REFUNDING'
  | 'REFUNDED'
  | 'CANCELLED'

export interface PaymentAfterSales {
  id: number
  paymentOrderId: number
  userId: number
  reason: string
  status: PaymentAfterSalesStatus
  refundAmount: string
  processedAt: string | null
  rejectReason: string | null
  createdAt: string
  user: PaymentUserBrief | null
  order: {
    id: number
    outTradeNo: string
    subject: string
    amount: string
    status: PaymentOrderStatus
    bizType: PaymentBizType
    payee: PaymentUserBrief | null
  } | null
}

export interface PaymentOrderDetail extends PaymentOrder {
  afterSales: Array<{
    id: number
    reason: string
    status: PaymentAfterSalesStatus
    refundAmount: string
    rejectReason: string | null
    processedAt: string | null
    createdAt: string
  }>
}

export interface StatsOverview {
  users: {
    total: number
    consumers: number
    admins: number
    recent7Days: number
  }
  posts: {
    total: number
    pending: number
    approved: number
    rejected: number
    offShelf: number
    visible: number
    hidden: number
  }
  activities: {
    total: number
    pending: number
    approved: number
    rejected: number
    offShelf: number
  }
  payments: {
    total: number
    paid: number
    totalPaidAmount: string
    pendingAfterSales: number
  }
}

export interface DailyAnalyticsRow {
  date: string
  dau: number
  eventCount: number
  newUsers: number
  postsCreated: number
  activitiesCreated: number
  ordersPaid: number
  paidAmount: string
  appOpens: number
  screenViews: number
  postViews: number
  activityViews: number
  feedRefreshes: number
  activityFeedRefreshes: number
  searches: number
  postLikes: number
  postComments: number
  postFavorites: number
  activityLikes: number
  activityJoins: number
  chatSends: number
  userFollows: number
  paymentSuccess: number
  contentClicks: number
}

export interface EventDailyCount {
  date: string
  event: string
  count: number
}

export interface CategoryTotal {
  category: string
  count: number
}

export interface DimensionCount {
  key: string
  label: string
  count: number
}

export interface HourlyActivity {
  hour: number
  label: string
  count: number
}

export interface AnalyticsFunnels {
  payment: { start: number; success: number; rate: number }
  content: {
    clicks: number
    views: number
    engagements: number
    viewRate: number
    engageRate: number
  }
  post: {
    views: number
    likes: number
    comments: number
    favorites: number
    likeRate: number
    commentRate: number
  }
}

export interface PaymentScenarioStat {
  key: string
  label: string
  start: number
  success: number
  rate: number
}

export interface AnalyticsInsights {
  topSearchKeywords: DimensionCount[]
  screenBreakdown: DimensionCount[]
  feedChannelBreakdown: DimensionCount[]
  feedTabBreakdown: DimensionCount[]
  contentClickBreakdown: DimensionCount[]
  searchTabBreakdown: DimensionCount[]
  chatMessageTypes: DimensionCount[]
  postCategoryCreateBreakdown?: DimensionCount[]
  postCategoryViewBreakdown?: DimensionCount[]
  paymentScenarioStats?: PaymentScenarioStat[]
  hourlyActivity: HourlyActivity[]
  funnels: AnalyticsFunnels
}

export interface DailyAnalytics {
  days: number
  rows: DailyAnalyticsRow[]
  eventBreakdown: EventDailyCount[]
  eventLabels: Record<string, string>
  eventCategories: Record<string, string>
  categoryTotals: CategoryTotal[]
  insights?: AnalyticsInsights
  summary: {
    totalDau: number
    avgDau: number
    totalEvents: number
    totalNewUsers: number
    totalPostsCreated: number
    totalActivitiesCreated: number
    totalOrdersPaid: number
    totalPaidAmount: string
    totalPostViews: number
    totalActivityViews: number
    totalEngagements: number
    totalPaymentSuccess: number
  }
}

export interface AnalyticsEventRecord {
  id: number
  userId: number | null
  event: string
  properties: Record<string, unknown> | null
  platform: string | null
  sessionId: string | null
  createdAt: string
}
