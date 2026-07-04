import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AnalyticsEventEntity } from '@module/analytics/entity/analytics-event.entity';
import {
  AnalyticsEventInputDto,
  IngestAnalyticsEventsBodyDto,
} from '@module/analytics/dto/analytics.dto';

@Injectable()
export class AnalyticsIngestService {
  constructor(
    @InjectRepository(AnalyticsEventEntity)
    private readonly analyticsEventRepository: Repository<AnalyticsEventEntity>,
  ) {}

  async ingestBatch(
    body: IngestAnalyticsEventsBodyDto,
    userId?: number,
  ): Promise<{ accepted: number }> {
    const rows = body.events.map((event) =>
      this.analyticsEventRepository.create(this.toEntity(event, userId)),
    );
    await this.analyticsEventRepository.save(rows);
    return { accepted: rows.length };
  }

  async track(
    event: string,
    userId?: number,
    properties?: Record<string, unknown>,
    platform?: string,
    sessionId?: string,
  ): Promise<void> {
    await this.analyticsEventRepository.save(
      this.analyticsEventRepository.create(
        this.toEntity({ event, properties, platform, sessionId }, userId),
      ),
    );
  }

  private toEntity(
    input: AnalyticsEventInputDto,
    userId?: number,
  ): AnalyticsEventEntity {
    const createdAt = input.occurredAt ? new Date(input.occurredAt) : undefined;
    const entity = this.analyticsEventRepository.create({
      userId: userId ?? null,
      event: input.event.slice(0, 64),
      properties: input.properties ?? null,
      platform: input.platform?.slice(0, 16) ?? null,
      sessionId: input.sessionId?.slice(0, 64) ?? null,
    });
    if (createdAt && !Number.isNaN(createdAt.getTime())) {
      entity.createdAt = createdAt;
    }
    return entity;
  }
}
