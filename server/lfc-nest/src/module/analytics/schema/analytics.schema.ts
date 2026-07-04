import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsISO8601,
  IsInt,
  IsObject,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';
import {
  AnalyticsEventInputDto,
  IngestAnalyticsEventsBodyDto,
} from '@module/analytics/dto/analytics.dto';

export class AnalyticsEventInputSchema implements AnalyticsEventInputDto {
  @IsString()
  @MaxLength(64)
  event: string;

  @IsOptional()
  @IsObject()
  properties?: Record<string, unknown>;

  @IsOptional()
  @IsString()
  @MaxLength(16)
  platform?: string;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  sessionId?: string;

  @IsOptional()
  @IsISO8601()
  occurredAt?: string;
}

export class IngestAnalyticsEventsBodySchema
  implements IngestAnalyticsEventsBodyDto
{
  @IsArray()
  @ArrayMinSize(1, { message: '至少上报一条事件' })
  @ArrayMaxSize(100, { message: '单次最多上报100条事件' })
  @ValidateNested({ each: true })
  @Type(() => AnalyticsEventInputSchema)
  events: AnalyticsEventInputSchema[];
}

export class AdminDailyAnalyticsQuerySchema {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(90)
  days?: number = 30;
}

export class AdminAnalyticsEventsQuerySchema {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(90)
  days?: number = 7;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  event?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number = 20;
}
