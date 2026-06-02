import {
  IsDateString,
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Min,
  MinLength,
} from 'class-validator';
import {
  CreateActivityBodyDto,
  ReviewActivityBodyDto,
  UpdateActivityBodyDto,
} from '@module/activity/dto/activity.dto';
import { ActivityStatus } from '@shared/enum/user-role.enum';

export class CreateActivityBodySchema implements CreateActivityBodyDto {
  @IsString()
  @MinLength(1)
  title: string;

  @IsString()
  @MinLength(1)
  description: string;

  @IsString()
  @MinLength(1)
  location: string;

  @IsDateString()
  startTime: string;

  @IsDateString()
  endTime: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  maxParticipants?: number;
}

export class UpdateActivityBodySchema implements UpdateActivityBodyDto {
  @IsOptional()
  @IsString()
  @MinLength(1)
  title?: string;

  @IsOptional()
  @IsString()
  @MinLength(1)
  description?: string;

  @IsOptional()
  @IsString()
  @MinLength(1)
  location?: string;

  @IsOptional()
  @IsDateString()
  startTime?: string;

  @IsOptional()
  @IsDateString()
  endTime?: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  maxParticipants?: number;
}

export class ReviewActivityBodySchema implements ReviewActivityBodyDto {
  @IsEnum([ActivityStatus.APPROVED, ActivityStatus.REJECTED])
  status: ActivityStatus.APPROVED | ActivityStatus.REJECTED;
}
