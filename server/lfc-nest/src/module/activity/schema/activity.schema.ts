import {
  ArrayMaxSize,
  IsArray,
  IsDateString,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  MaxLength,
  Min,
  MinLength,
  ValidateIf,
} from 'class-validator';
import {
  CreateActivityBodyDto,
  ReviewActivityBodyDto,
  UpdateActivityBodyDto,
} from '@module/activity/dto/activity.dto';
import { AdminUpdateActivityBodyDto } from '@module/activity/dto/admin-activity.dto';
import { AdminActivityListQueryDto } from '@module/admin/dto/admin-query.dto';
import { ActivityStatus } from '@shared/enum/user-role.enum';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';

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

  @IsOptional()
  @IsNumber()
  latitude?: number;

  @IsOptional()
  @IsNumber()
  longitude?: number;

  @IsDateString()
  startTime: string;

  @IsDateString()
  endTime: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  maxParticipants?: number;

  @IsOptional()
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  fee?: number;
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
  @IsNumber()
  latitude?: number;

  @IsOptional()
  @IsNumber()
  longitude?: number;

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

  @IsOptional()
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  fee?: number;
}

export class ReviewActivityBodySchema implements ReviewActivityBodyDto {
  @IsEnum([ActivityStatus.APPROVED, ActivityStatus.REJECTED])
  status: ActivityStatus.APPROVED | ActivityStatus.REJECTED;

  @ValidateIf((body) => body.status === ActivityStatus.REJECTED)
  @IsString()
  @IsNotEmpty({ message: '拒绝时需填写审核意见' })
  @MaxLength(500)
  reviewComment?: string;
}

export class AdminActivityListQuerySchema
  extends PaginationQuerySchema
  implements AdminActivityListQueryDto
{
  @IsOptional()
  @IsString()
  @MaxLength(64)
  keyword?: string;

  @IsOptional()
  @IsEnum(ActivityStatus)
  status?: ActivityStatus;
}

export class AdminUpdateActivityBodySchema implements AdminUpdateActivityBodyDto {
  @IsOptional()
  @IsString()
  @MinLength(1)
  title?: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsOptional()
  @IsString()
  @MinLength(1)
  location?: string;

  @IsOptional()
  @IsEnum(ActivityStatus)
  status?: ActivityStatus;

  @ValidateIf((body) => body.status === ActivityStatus.REJECTED)
  @IsString()
  @IsNotEmpty({ message: '拒绝时需填写审核意见' })
  @MaxLength(500)
  reviewComment?: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  maxParticipants?: number;

  @IsOptional()
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  fee?: number;
}
