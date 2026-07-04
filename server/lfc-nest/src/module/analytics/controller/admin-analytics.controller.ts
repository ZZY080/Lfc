import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { AdminAnalyticsService } from '@module/analytics/service/admin-analytics.service';
import {
  AdminAnalyticsEventsQuerySchema,
  AdminDailyAnalyticsQuerySchema,
} from '@module/analytics/schema/analytics.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/analytics')
@UseGuards(JwtAuthGuard)
export class AdminAnalyticsController {
  constructor(private readonly adminAnalyticsService: AdminAnalyticsService) {}

  @Get('daily')
  getDaily(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminDailyAnalyticsQuerySchema,
  ) {
    return this.adminAnalyticsService.getDailyMetrics(userId, query.days);
  }

  @Get('events')
  findEvents(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminAnalyticsEventsQuerySchema,
  ) {
    return this.adminAnalyticsService.findEvents(userId, query);
  }
}
