import {
  ArrayMaxSize,
  IsArray,
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
  @IsOptional()
  @IsString()
  @MinLength(1)
  title?: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ArrayMaxSize(20, { message: '最多上传20张图片' })
  images?: string[];

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
  description?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ArrayMaxSize(20, { message: '最多上传20张图片' })
  images?: string[];

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
