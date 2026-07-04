import { Body, Controller, Post, UseGuards } from '@nestjs/common';
import { AnalyticsIngestService } from '@module/analytics/service/analytics-ingest.service';
import { IngestAnalyticsEventsBodySchema } from '@module/analytics/schema/analytics.schema';
import { OptionalJwtAuthGuard } from '@shared/guard/optional-jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/analytics')
export class ConsumerAnalyticsController {
  constructor(private readonly analyticsIngestService: AnalyticsIngestService) {}

  @Post('events')
  @UseGuards(OptionalJwtAuthGuard)
  ingest(
    @Body() body: IngestAnalyticsEventsBodySchema,
    @CurrentUser('userId') userId?: number,
  ) {
    return this.analyticsIngestService.ingestBatch(body, userId);
  }
}
