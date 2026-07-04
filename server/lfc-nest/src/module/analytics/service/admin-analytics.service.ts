import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AnalyticsEventEntity } from '@module/analytics/entity/analytics-event.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  ANALYTICS_EVENT_CATEGORIES,
  ANALYTICS_EVENT_LABELS,
  ANALYTICS_EVENTS,
} from '@shared/enum/analytics-event.enum';
import {
  AdminDailyAnalyticsDto,
  AnalyticsEventRecordDto,
  AnalyticsInsightsDto,
  CategoryTotalDto,
  DailyAnalyticsRowDto,
  DimensionCountDto,
  EventDailyCountDto,
  PaymentScenarioStatDto,
} from '@module/analytics/dto/analytics.dto';
import {
  resolveAnalyticsScreenLabel,
  resolvePaymentScenarioLabel,
} from '@shared/enum/analytics-dimension.enum';
import { paymentScenarioFromBizType } from '@module/analytics/util/payment-scenario.util';
import { PaymentOrderStatus } from '@shared/enum/payment.enum';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

type DateCountRow = { date: string; count: string };
type DimensionRow = { dimension: string | null; count: string };
type HourlyRow = { hour: string; count: string };
type DauRow = { date: string; dau: string };
type EventCountRow = { date: string; event: string; count: string };
type PaidAmountRow = { date: string; amount: string };

@Injectable()
export class AdminAnalyticsService {
  constructor(
    @InjectRepository(AnalyticsEventEntity)
    private readonly analyticsEventRepository: Repository<AnalyticsEventEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async getDailyMetrics(
    adminUserId: number,
    daysInput?: number,
  ): Promise<AdminDailyAnalyticsDto> {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);

    const days = this.normalizeDays(daysInput);
    const dateKeys = this.buildDateKeys(days);
    const startAt = this.startOfDay(dateKeys[0]);

    const [
      dauRows,
      eventCountRows,
      eventBreakdownRows,
      newUserRows,
      postRows,
      activityRows,
      paidOrderRows,
      paidAmountRows,
    ] = await Promise.all([
      this.queryDau(startAt),
      this.queryEventCountsByDate(startAt),
      this.queryEventBreakdown(startAt),
      this.queryCreatedPerDay(this.userRepository, 'user', startAt),
      this.queryCreatedPerDay(this.postRepository, 'post', startAt),
      this.queryCreatedPerDay(this.activityRepository, 'activity', startAt),
      this.queryPaidOrdersPerDay(startAt),
      this.queryPaidAmountPerDay(startAt),
    ]);

    const dauMap = this.toCountMap(dauRows, 'dau');
    const eventCountMap = this.toCountMap(eventCountRows, 'count');
    const newUserMap = this.toCountMap(newUserRows, 'count');
    const postMap = this.toCountMap(postRows, 'count');
    const activityMap = this.toCountMap(activityRows, 'count');
    const paidOrderMap = this.toCountMap(paidOrderRows, 'count');
    const paidAmountMap = this.toAmountMap(paidAmountRows);

    const eventDailyMaps = await this.queryEventDailyMaps(startAt);

    const rows: DailyAnalyticsRowDto[] = dateKeys.map((date) => ({
      date,
      dau: dauMap.get(date) ?? 0,
      eventCount: eventCountMap.get(date) ?? 0,
      newUsers: newUserMap.get(date) ?? 0,
      postsCreated: postMap.get(date) ?? 0,
      activitiesCreated: activityMap.get(date) ?? 0,
      ordersPaid: paidOrderMap.get(date) ?? 0,
      paidAmount: paidAmountMap.get(date) ?? '0.00',
      appOpens: this.pickEvent(eventDailyMaps, ANALYTICS_EVENTS.APP_OPEN, date),
      screenViews: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.SCREEN_VIEW,
        date,
      ),
      postViews: this.pickEvent(eventDailyMaps, ANALYTICS_EVENTS.POST_VIEW, date),
      activityViews: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.ACTIVITY_VIEW,
        date,
      ),
      feedRefreshes: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.FEED_REFRESH,
        date,
      ),
      activityFeedRefreshes: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.ACTIVITY_FEED_REFRESH,
        date,
      ),
      searches: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.SEARCH_SUBMIT,
        date,
      ),
      postLikes: this.pickEvent(eventDailyMaps, ANALYTICS_EVENTS.POST_LIKE, date),
      postComments: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.POST_COMMENT,
        date,
      ),
      postFavorites: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.POST_FAVORITE,
        date,
      ),
      activityLikes: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.ACTIVITY_LIKE,
        date,
      ),
      activityJoins: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.ACTIVITY_JOIN,
        date,
      ),
      chatSends: this.pickEvent(eventDailyMaps, ANALYTICS_EVENTS.CHAT_SEND, date),
      userFollows: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.USER_FOLLOW,
        date,
      ),
      paymentSuccess: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.PAYMENT_SUCCESS,
        date,
      ),
      contentClicks: this.pickEvent(
        eventDailyMaps,
        ANALYTICS_EVENTS.CONTENT_CLICK,
        date,
      ),
    }));

    const eventBreakdown = this.filterBreakdownToRange(
      eventBreakdownRows,
      dateKeys,
    );

    const categoryTotals = this.buildCategoryTotals(eventBreakdown);

    const summary = rows.reduce(
      (acc, row) => ({
        totalDau: acc.totalDau + row.dau,
        avgDau: 0,
        totalEvents: acc.totalEvents + row.eventCount,
        totalNewUsers: acc.totalNewUsers + row.newUsers,
        totalPostsCreated: acc.totalPostsCreated + row.postsCreated,
        totalActivitiesCreated:
          acc.totalActivitiesCreated + row.activitiesCreated,
        totalOrdersPaid: acc.totalOrdersPaid + row.ordersPaid,
        totalPaidAmount: (
          Number(acc.totalPaidAmount) + Number(row.paidAmount)
        ).toFixed(2),
        totalPostViews: acc.totalPostViews + row.postViews,
        totalActivityViews: acc.totalActivityViews + row.activityViews,
        totalEngagements:
          acc.totalEngagements +
          row.postLikes +
          row.postComments +
          row.postFavorites +
          row.activityLikes +
          row.activityJoins +
          row.chatSends +
          row.userFollows,
        totalPaymentSuccess: acc.totalPaymentSuccess + row.paymentSuccess,
      }),
      {
        totalDau: 0,
        avgDau: 0,
        totalEvents: 0,
        totalNewUsers: 0,
        totalPostsCreated: 0,
        totalActivitiesCreated: 0,
        totalOrdersPaid: 0,
        totalPaidAmount: '0',
        totalPostViews: 0,
        totalActivityViews: 0,
        totalEngagements: 0,
        totalPaymentSuccess: 0,
      },
    );
    summary.avgDau = Math.round(summary.totalDau / days);

    const insights = await this.buildInsights(startAt, summary);

    return {
      days,
      rows,
      eventBreakdown,
      eventLabels: ANALYTICS_EVENT_LABELS,
      eventCategories: ANALYTICS_EVENT_CATEGORIES,
      categoryTotals,
      insights,
      summary,
    };
  }

  async findEvents(
    adminUserId: number,
    query: {
      days?: number;
      event?: string;
      page?: number;
      limit?: number;
    },
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);

    const days = this.normalizeDays(query.days ?? 7);
    const dateKeys = this.buildDateKeys(days);
    const startAt = this.startOfDay(dateKeys[0]);
    const { page, limit, skip } = normalizePagination(query.page, query.limit);

    const qb = this.analyticsEventRepository
      .createQueryBuilder('event')
      .where('event.created_at >= :startAt', { startAt });

    if (query.event?.trim()) {
      qb.andWhere('event.event = :event', { event: query.event.trim() });
    }

    qb.orderBy('event.created_at', 'DESC').skip(skip).take(limit);
    const [items, total] = await qb.getManyAndCount();

    return createPaginatedResult(
      items.map(
        (item): AnalyticsEventRecordDto => ({
          id: item.id,
          userId: item.userId,
          event: item.event,
          properties: item.properties,
          platform: item.platform,
          sessionId: item.sessionId,
          createdAt: item.createdAt.toISOString(),
        }),
      ),
      total,
      page,
      limit,
    );
  }

  private async buildInsights(
    startAt: Date,
    summary: AdminDailyAnalyticsDto['summary'],
  ): Promise<AnalyticsInsightsDto> {
    const [
      topSearchKeywords,
      screenBreakdown,
      feedChannelBreakdown,
      feedTabBreakdown,
      contentClickBreakdown,
      searchTabBreakdown,
      chatMessageTypes,
      postCategoryCreateBreakdown,
      postCategoryViewBreakdown,
      paymentScenarioStats,
      hourlyActivity,
      paymentStart,
      postCategoryCreateFromDb,
      paymentScenarioFromDb,
      postCategoryViewFromJoin,
      postCategoryViewSnapshotFromDb,
    ] = await Promise.all([
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.SEARCH_SUBMIT,
        'keyword',
        20,
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.SCREEN_VIEW,
        'screen',
        20,
        resolveAnalyticsScreenLabel,
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.FEED_CHANNEL_SWITCH,
        'channel',
        15,
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.FEED_TAB_SWITCH,
        'tab',
        15,
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.CONTENT_CLICK,
        'type',
        10,
        (key) => (key === 'post' ? '笔记' : key === 'activity' ? '活动' : key),
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.SEARCH_TAB_SWITCH,
        'tab',
        10,
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.CHAT_SEND,
        'messageType',
        10,
        (key) => {
          const labels: Record<string, string> = {
            TEXT: '文字',
            IMAGE: '图片',
            VIDEO: '视频',
          };
          return labels[key] ?? key;
        },
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.POST_CREATE,
        'category',
        15,
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.POST_VIEW,
        'category',
        15,
      ),
      this.queryPaymentScenarioStats(startAt),
      this.queryHourlyActivity(startAt),
      this.queryEventTotal(startAt, ANALYTICS_EVENTS.PAYMENT_START),
      this.queryPostCategoryCreateFromDb(startAt),
      this.queryPaymentScenarioStatsFromDb(startAt),
      this.queryPostCategoryViewFromEventsJoin(startAt),
      this.queryPostCategoryViewSnapshotFromDb(),
    ]);

    const postViews = summary.totalPostViews;
    const postLikes = await this.queryEventTotal(
      startAt,
      ANALYTICS_EVENTS.POST_LIKE,
    );
    const postComments = await this.queryEventTotal(
      startAt,
      ANALYTICS_EVENTS.POST_COMMENT,
    );
    const postFavorites = await this.queryEventTotal(
      startAt,
      ANALYTICS_EVENTS.POST_FAVORITE,
    );
    const contentClicks = await this.queryEventTotal(
      startAt,
      ANALYTICS_EVENTS.CONTENT_CLICK,
    );
    const contentViews = postViews + summary.totalActivityViews;

    const mergedPostCategoryCreate = this.pickDimensionSource(
      postCategoryCreateFromDb,
      postCategoryCreateBreakdown,
    );
    const mergedPostCategoryView = this.pickFirstNonEmptyDimensionSource(
      postCategoryViewBreakdown,
      postCategoryViewFromJoin,
      postCategoryViewSnapshotFromDb,
    );
    const mergedPaymentScenarioStats = this.pickPaymentScenarioSource(
      paymentScenarioFromDb,
      paymentScenarioStats,
    );

    return {
      topSearchKeywords,
      screenBreakdown,
      feedChannelBreakdown,
      feedTabBreakdown,
      contentClickBreakdown,
      searchTabBreakdown,
      chatMessageTypes,
      postCategoryCreateBreakdown: mergedPostCategoryCreate,
      postCategoryViewBreakdown: mergedPostCategoryView,
      paymentScenarioStats: mergedPaymentScenarioStats,
      hourlyActivity,
      funnels: {
        payment: {
          start: paymentStart,
          success: summary.totalPaymentSuccess,
          rate: this.calcRate(summary.totalPaymentSuccess, paymentStart),
        },
        content: {
          clicks: contentClicks,
          views: contentViews,
          engagements: summary.totalEngagements,
          viewRate: this.calcRate(contentViews, contentClicks),
          engageRate: this.calcRate(summary.totalEngagements, contentViews),
        },
        post: {
          views: postViews,
          likes: postLikes,
          comments: postComments,
          favorites: postFavorites,
          likeRate: this.calcRate(postLikes, postViews),
          commentRate: this.calcRate(postComments, postViews),
        },
      },
    };
  }

  private calcRate(numerator: number, denominator: number): number {
    if (denominator <= 0) return 0;
    return Math.round((numerator / denominator) * 1000) / 10;
  }

  private async queryEventTotal(
    startAt: Date,
    event: string,
  ): Promise<number> {
    const row = await this.analyticsEventRepository
      .createQueryBuilder('event')
      .select('COUNT(*)', 'count')
      .where('event.created_at >= :startAt', { startAt })
      .andWhere('event.event = :event', { event })
      .getRawOne<{ count: string }>();
    return Number(row?.count) || 0;
  }

  private async queryPropertyBreakdown(
    startAt: Date,
    event: string,
    propertyKey: string,
    limit: number,
    labelResolver: (key: string) => string = (key) => key,
  ): Promise<DimensionCountDto[]> {
    const jsonPath = `$.${propertyKey}`;
    const rows = await this.analyticsEventRepository
      .createQueryBuilder('event')
      .select(
        `JSON_UNQUOTE(JSON_EXTRACT(CAST(event.properties AS JSON), :jsonPath))`,
        'dimension',
      )
      .addSelect('COUNT(*)', 'count')
      .where('event.created_at >= :startAt', { startAt })
      .andWhere('event.event = :event', { event })
      .andWhere('event.properties IS NOT NULL')
      .andWhere(`JSON_EXTRACT(CAST(event.properties AS JSON), :jsonPath) IS NOT NULL`)
      .setParameter('jsonPath', jsonPath)
      .groupBy('dimension')
      .orderBy('count', 'DESC')
      .limit(limit)
      .getRawMany<DimensionRow>();

    return rows
      .map((row) => {
        const key = String(row.dimension ?? '').trim();
        return {
          key,
          label: labelResolver(key),
          count: Number(row.count) || 0,
        };
      })
      .filter((row) => row.key.length > 0);
  }

  private async queryHourlyActivity(
    startAt: Date,
  ): Promise<AnalyticsInsightsDto['hourlyActivity']> {
    const rows = await this.analyticsEventRepository
      .createQueryBuilder('event')
      .select('HOUR(event.created_at)', 'hour')
      .addSelect('COUNT(*)', 'count')
      .where('event.created_at >= :startAt', { startAt })
      .groupBy('HOUR(event.created_at)')
      .orderBy('hour', 'ASC')
      .getRawMany<HourlyRow>();

    const map = new Map<number, number>();
    for (const row of rows) {
      map.set(Number(row.hour), Number(row.count) || 0);
    }

    return Array.from({ length: 24 }, (_, hour) => ({
      hour,
      label: `${String(hour).padStart(2, '0')}:00`,
      count: map.get(hour) ?? 0,
    }));
  }

  private async queryPaymentScenarioStats(
    startAt: Date,
  ): Promise<PaymentScenarioStatDto[]> {
    const [startRows, successRows] = await Promise.all([
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.PAYMENT_START,
        'scenario',
        20,
      ),
      this.queryPropertyBreakdown(
        startAt,
        ANALYTICS_EVENTS.PAYMENT_SUCCESS,
        'scenario',
        20,
      ),
    ]);

    const successMap = new Map(successRows.map((row) => [row.key, row.count]));
    const keys = new Set([
      ...startRows.map((row) => row.key),
      ...successRows.map((row) => row.key),
    ]);

    return [...keys]
      .map((key) => {
        const start =
          startRows.find((row) => row.key === key)?.count ??
          successMap.get(key) ??
          0;
        const success = successMap.get(key) ?? 0;
        return {
          key,
          label: resolvePaymentScenarioLabel(key),
          start,
          success,
          rate: this.calcRate(success, start),
        };
      })
      .filter((row) => row.start > 0 || row.success > 0)
      .sort((a, b) => b.start - a.start);
  }

  private async queryPostCategoryCreateFromDb(
    startAt: Date,
  ): Promise<DimensionCountDto[]> {
    const rows = await this.postRepository
      .createQueryBuilder('post')
      .select('post.category', 'dimension')
      .addSelect('COUNT(*)', 'count')
      .where('post.created_at >= :startAt', { startAt })
      .groupBy('post.category')
      .orderBy('count', 'DESC')
      .getRawMany<DimensionRow>();

    return rows
      .map((row) => {
        const key = String(row.dimension ?? '').trim();
        return {
          key,
          label: key,
          count: Number(row.count) || 0,
        };
      })
      .filter((row) => row.key.length > 0);
  }

  private async queryPostCategoryViewFromEventsJoin(
    startAt: Date,
  ): Promise<DimensionCountDto[]> {
    const rows = await this.analyticsEventRepository
      .createQueryBuilder('event')
      .innerJoin(
        PostEntity,
        'post',
        `post.id = CAST(JSON_UNQUOTE(JSON_EXTRACT(CAST(event.properties AS JSON), '$.postId')) AS UNSIGNED)`,
      )
      .select('post.category', 'dimension')
      .addSelect('COUNT(*)', 'count')
      .where('event.created_at >= :startAt', { startAt })
      .andWhere('event.event = :event', { event: ANALYTICS_EVENTS.POST_VIEW })
      .andWhere('post.category IS NOT NULL')
      .andWhere("post.category != ''")
      .groupBy('post.category')
      .orderBy('count', 'DESC')
      .limit(15)
      .getRawMany<DimensionRow>();

    return rows
      .map((row) => {
        const key = String(row.dimension ?? '').trim();
        return {
          key,
          label: key,
          count: Number(row.count) || 0,
        };
      })
      .filter((row) => row.key.length > 0);
  }

  private async queryPostCategoryViewSnapshotFromDb(): Promise<DimensionCountDto[]> {
    const rows = await this.postRepository
      .createQueryBuilder('post')
      .select('post.category', 'dimension')
      .addSelect('SUM(post.view_count)', 'count')
      .where('post.view_count > 0')
      .andWhere('post.category IS NOT NULL')
      .andWhere("post.category != ''")
      .groupBy('post.category')
      .orderBy('count', 'DESC')
      .limit(15)
      .getRawMany<DimensionRow>();

    return rows
      .map((row) => {
        const key = String(row.dimension ?? '').trim();
        return {
          key,
          label: key,
          count: Number(row.count) || 0,
        };
      })
      .filter((row) => row.key.length > 0);
  }

  private async queryPaymentScenarioStatsFromDb(
    startAt: Date,
  ): Promise<PaymentScenarioStatDto[]> {
    const paidStatuses = [
      PaymentOrderStatus.PAID,
      PaymentOrderStatus.CONFIRMED,
      PaymentOrderStatus.SETTLED,
    ];

    const [startRows, successRows] = await Promise.all([
      this.paymentOrderRepository
        .createQueryBuilder('order')
        .select('order.biz_type', 'dimension')
        .addSelect('COUNT(*)', 'count')
        .where('order.created_at >= :startAt', { startAt })
        .groupBy('order.biz_type')
        .getRawMany<DimensionRow>(),
      this.paymentOrderRepository
        .createQueryBuilder('order')
        .select('order.biz_type', 'dimension')
        .addSelect('COUNT(*)', 'count')
        .where('order.paid_at IS NOT NULL')
        .andWhere('order.paid_at >= :startAt', { startAt })
        .andWhere('order.status IN (:...statuses)', { statuses: paidStatuses })
        .groupBy('order.biz_type')
        .getRawMany<DimensionRow>(),
    ]);

    const successMap = new Map(
      successRows.map((row) => [
        paymentScenarioFromBizType(String(row.dimension ?? '')),
        Number(row.count) || 0,
      ]),
    );
    const keys = new Set<string>([
      ...startRows.map((row) =>
        paymentScenarioFromBizType(String(row.dimension ?? '')),
      ),
      ...successMap.keys(),
    ]);

    return [...keys]
      .map((key) => {
        const start = startRows
          .filter(
            (row) =>
              paymentScenarioFromBizType(String(row.dimension ?? '')) === key,
          )
          .reduce((sum, row) => sum + (Number(row.count) || 0), 0);
        const success = successMap.get(key) ?? 0;
        return {
          key,
          label: resolvePaymentScenarioLabel(key),
          start,
          success,
          rate: this.calcRate(success, start),
        };
      })
      .filter((row) => row.start > 0 || row.success > 0)
      .sort((a, b) => b.start - a.start);
  }

  private pickFirstNonEmptyDimensionSource(
    ...sources: DimensionCountDto[][]
  ): DimensionCountDto[] {
    for (const source of sources) {
      if (source.reduce((sum, item) => sum + item.count, 0) > 0) {
        return source;
      }
    }
    return [];
  }

  private pickDimensionSource(
    fromDb: DimensionCountDto[],
    fromEvents: DimensionCountDto[],
  ): DimensionCountDto[] {
    const dbTotal = fromDb.reduce((sum, item) => sum + item.count, 0);
    return dbTotal > 0 ? fromDb : fromEvents;
  }

  private pickPaymentScenarioSource(
    fromDb: PaymentScenarioStatDto[],
    fromEvents: PaymentScenarioStatDto[],
  ): PaymentScenarioStatDto[] {
    const dbTotal = fromDb.reduce(
      (sum, item) => sum + item.start + item.success,
      0,
    );
    return dbTotal > 0 ? fromDb : fromEvents;
  }

  private normalizeDays(daysInput?: number): number {
    const parsed = Number(daysInput ?? 30);
    if (!Number.isFinite(parsed)) return 30;
    return Math.min(90, Math.max(1, Math.floor(parsed)));
  }

  private buildDateKeys(days: number): string[] {
    const keys: string[] = [];
    const cursor = new Date();
    cursor.setHours(0, 0, 0, 0);
    for (let i = days - 1; i >= 0; i -= 1) {
      const date = new Date(cursor);
      date.setDate(cursor.getDate() - i);
      keys.push(this.formatDate(date));
    }
    return keys;
  }

  private startOfDay(dateKey: string): Date {
    const [year, month, day] = dateKey.split('-').map(Number);
    return new Date(year, month - 1, day, 0, 0, 0, 0);
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private async queryDau(startAt: Date): Promise<DauRow[]> {
    return this.analyticsEventRepository
      .createQueryBuilder('event')
      .select('DATE(event.created_at)', 'date')
      .addSelect('COUNT(DISTINCT event.user_id)', 'dau')
      .where('event.created_at >= :startAt', { startAt })
      .andWhere('event.user_id IS NOT NULL')
      .groupBy('DATE(event.created_at)')
      .orderBy('date', 'ASC')
      .getRawMany<DauRow>();
  }

  private async queryEventCountsByDate(startAt: Date): Promise<DateCountRow[]> {
    return this.analyticsEventRepository
      .createQueryBuilder('event')
      .select('DATE(event.created_at)', 'date')
      .addSelect('COUNT(*)', 'count')
      .where('event.created_at >= :startAt', { startAt })
      .groupBy('DATE(event.created_at)')
      .orderBy('date', 'ASC')
      .getRawMany<DateCountRow>();
  }

  private async queryEventBreakdown(startAt: Date): Promise<EventCountRow[]> {
    return this.analyticsEventRepository
      .createQueryBuilder('event')
      .select('DATE(event.created_at)', 'date')
      .addSelect('event.event', 'event')
      .addSelect('COUNT(*)', 'count')
      .where('event.created_at >= :startAt', { startAt })
      .groupBy('DATE(event.created_at)')
      .addGroupBy('event.event')
      .orderBy('date', 'ASC')
      .addOrderBy('count', 'DESC')
      .getRawMany<EventCountRow>();
  }

  private async queryCreatedPerDay(
    repository: Repository<{ createdAt: Date }>,
    alias: string,
    startAt: Date,
  ): Promise<DateCountRow[]> {
    return repository
      .createQueryBuilder(alias)
      .select(`DATE(${alias}.created_at)`, 'date')
      .addSelect('COUNT(*)', 'count')
      .where(`${alias}.created_at >= :startAt`, { startAt })
      .groupBy(`DATE(${alias}.created_at)`)
      .orderBy('date', 'ASC')
      .getRawMany<DateCountRow>();
  }

  private async queryPaidOrdersPerDay(startAt: Date): Promise<DateCountRow[]> {
    const paidStatuses = [
      PaymentOrderStatus.PAID,
      PaymentOrderStatus.CONFIRMED,
      PaymentOrderStatus.SETTLED,
    ];
    return this.paymentOrderRepository
      .createQueryBuilder('order')
      .select('DATE(order.paid_at)', 'date')
      .addSelect('COUNT(*)', 'count')
      .where('order.paid_at IS NOT NULL')
      .andWhere('order.paid_at >= :startAt', { startAt })
      .andWhere('order.status IN (:...statuses)', { statuses: paidStatuses })
      .groupBy('DATE(order.paid_at)')
      .orderBy('date', 'ASC')
      .getRawMany<DateCountRow>();
  }

  private async queryPaidAmountPerDay(startAt: Date): Promise<PaidAmountRow[]> {
    const paidStatuses = [
      PaymentOrderStatus.PAID,
      PaymentOrderStatus.CONFIRMED,
      PaymentOrderStatus.SETTLED,
    ];
    return this.paymentOrderRepository
      .createQueryBuilder('order')
      .select('DATE(order.paid_at)', 'date')
      .addSelect('COALESCE(SUM(order.amount), 0)', 'amount')
      .where('order.paid_at IS NOT NULL')
      .andWhere('order.paid_at >= :startAt', { startAt })
      .andWhere('order.status IN (:...statuses)', { statuses: paidStatuses })
      .groupBy('DATE(order.paid_at)')
      .orderBy('date', 'ASC')
      .getRawMany<PaidAmountRow>();
  }

  private async queryEventDailyMaps(
    startAt: Date,
  ): Promise<Map<string, Map<string, number>>> {
    const rows = await this.analyticsEventRepository
      .createQueryBuilder('event')
      .select('DATE(event.created_at)', 'date')
      .addSelect('event.event', 'event')
      .addSelect('COUNT(*)', 'count')
      .where('event.created_at >= :startAt', { startAt })
      .groupBy('DATE(event.created_at)')
      .addGroupBy('event.event')
      .getRawMany<EventCountRow>();

    const maps = new Map<string, Map<string, number>>();
    for (const row of rows) {
      const date = this.normalizeRawDate(row.date);
      const count = Number(row.count) || 0;
      if (!maps.has(row.event)) {
        maps.set(row.event, new Map());
      }
      maps.get(row.event)!.set(date, count);
    }
    return maps;
  }

  private pickEvent(
    maps: Map<string, Map<string, number>>,
    event: string,
    date: string,
  ): number {
    return maps.get(event)?.get(date) ?? 0;
  }

  private buildCategoryTotals(
    eventBreakdown: EventDailyCountDto[],
  ): CategoryTotalDto[] {
    const totals = new Map<string, number>();
    for (const item of eventBreakdown) {
      const category = ANALYTICS_EVENT_CATEGORIES[item.event] ?? '其他';
      totals.set(category, (totals.get(category) ?? 0) + item.count);
    }
    return [...totals.entries()]
      .map(([category, count]) => ({ category, count }))
      .sort((a, b) => b.count - a.count);
  }

  private toCountMap(
    rows: Array<{ date: string | Date; count?: string; dau?: string }>,
    field: 'count' | 'dau',
  ): Map<string, number> {
    const map = new Map<string, number>();
    for (const row of rows) {
      const date = this.normalizeRawDate(row.date);
      map.set(date, Number(row[field]) || 0);
    }
    return map;
  }

  private toAmountMap(rows: PaidAmountRow[]): Map<string, string> {
    const map = new Map<string, string>();
    for (const row of rows) {
      const date = this.normalizeRawDate(row.date);
      map.set(date, Number(row.amount || 0).toFixed(2));
    }
    return map;
  }

  private normalizeRawDate(value: string | Date): string {
    if (value instanceof Date) {
      return this.formatDate(value);
    }
    const text = String(value);
    if (text.length >= 10) {
      return text.slice(0, 10);
    }
    return text;
  }

  private filterBreakdownToRange(
    rows: EventCountRow[],
    dateKeys: string[],
  ): EventDailyCountDto[] {
    const allowed = new Set(dateKeys);
    return rows
      .map((row) => ({
        date: this.normalizeRawDate(row.date),
        event: row.event,
        count: Number(row.count) || 0,
      }))
      .filter((row) => allowed.has(row.date));
  }
}
