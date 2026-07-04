export interface AnalyticsEventInputDto {
  event: string;
  properties?: Record<string, unknown>;
  platform?: string;
  sessionId?: string;
  occurredAt?: string;
}

export interface IngestAnalyticsEventsBodyDto {
  events: AnalyticsEventInputDto[];
}

export interface DailyAnalyticsRowDto {
  date: string;
  dau: number;
  eventCount: number;
  newUsers: number;
  postsCreated: number;
  activitiesCreated: number;
  ordersPaid: number;
  paidAmount: string;
  appOpens: number;
  screenViews: number;
  postViews: number;
  activityViews: number;
  feedRefreshes: number;
  activityFeedRefreshes: number;
  searches: number;
  postLikes: number;
  postComments: number;
  postFavorites: number;
  activityLikes: number;
  activityJoins: number;
  chatSends: number;
  userFollows: number;
  paymentSuccess: number;
  contentClicks: number;
}

export interface EventDailyCountDto {
  date: string;
  event: string;
  count: number;
}

export interface CategoryTotalDto {
  category: string;
  count: number;
}

export interface DimensionCountDto {
  key: string;
  label: string;
  count: number;
}

export interface HourlyActivityDto {
  hour: number;
  label: string;
  count: number;
}

export interface AnalyticsFunnelsDto {
  payment: {
    start: number;
    success: number;
    rate: number;
  };
  content: {
    clicks: number;
    views: number;
    engagements: number;
    viewRate: number;
    engageRate: number;
  };
  post: {
    views: number;
    likes: number;
    comments: number;
    favorites: number;
    likeRate: number;
    commentRate: number;
  };
}

export interface PaymentScenarioStatDto {
  key: string;
  label: string;
  start: number;
  success: number;
  rate: number;
}

export interface AnalyticsInsightsDto {
  topSearchKeywords: DimensionCountDto[];
  screenBreakdown: DimensionCountDto[];
  feedChannelBreakdown: DimensionCountDto[];
  feedTabBreakdown: DimensionCountDto[];
  contentClickBreakdown: DimensionCountDto[];
  searchTabBreakdown: DimensionCountDto[];
  chatMessageTypes: DimensionCountDto[];
  postCategoryCreateBreakdown: DimensionCountDto[];
  postCategoryViewBreakdown: DimensionCountDto[];
  paymentScenarioStats: PaymentScenarioStatDto[];
  hourlyActivity: HourlyActivityDto[];
  funnels: AnalyticsFunnelsDto;
}

export interface AdminDailyAnalyticsDto {
  days: number;
  rows: DailyAnalyticsRowDto[];
  eventBreakdown: EventDailyCountDto[];
  eventLabels: Record<string, string>;
  eventCategories: Record<string, string>;
  categoryTotals: CategoryTotalDto[];
  insights: AnalyticsInsightsDto;
  summary: {
    totalDau: number;
    avgDau: number;
    totalEvents: number;
    totalNewUsers: number;
    totalPostsCreated: number;
    totalActivitiesCreated: number;
    totalOrdersPaid: number;
    totalPaidAmount: string;
    totalPostViews: number;
    totalActivityViews: number;
    totalEngagements: number;
    totalPaymentSuccess: number;
  };
}

export interface AnalyticsEventRecordDto {
  id: number;
  userId: number | null;
  event: string;
  properties: Record<string, unknown> | null;
  platform: string | null;
  sessionId: string | null;
  createdAt: string;
}
