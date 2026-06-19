import {
  IsBoolean,
  IsIn,
  IsInt,
  IsOptional,
  IsString,
  MaxLength,
  MinLength,
  IsArray,
  ArrayMaxSize,
  IsEnum,
  IsNumber,
  ValidateIf,
} from 'class-validator';
import {
  AdminPostListQueryDto,
  AdminUpdatePostBodyDto,
  AdminUpdatePostVisibilityBodyDto,
  ReviewPostBodyDto,
} from '@module/post/dto/admin-post.dto';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';
import { POST_CATEGORIES } from '@shared/enum/post-category.enum';
import { PostStatus } from '@shared/enum/post-status.enum';
import { IsNotEmpty } from 'class-validator';

export class AdminPostListQuerySchema
  extends PaginationQuerySchema
  implements AdminPostListQueryDto
{
  @IsOptional()
  @IsString()
  @MaxLength(64)
  keyword?: string;

  @IsOptional()
  @IsIn(['all', 'visible', 'hidden'])
  visibility?: 'all' | 'visible' | 'hidden';

  @IsOptional()
  @IsEnum(PostStatus)
  status?: PostStatus;
}

export class AdminUpdatePostBodySchema implements AdminUpdatePostBodyDto {
  @IsOptional()
  @IsString()
  @MinLength(1)
  @MaxLength(128)
  title?: string;

  @IsOptional()
  @IsString()
  @MinLength(1)
  content?: string;

  @IsOptional()
  @IsString()
  @MinLength(1)
  @MaxLength(32)
  @IsEnum(POST_CATEGORIES, { message: '笔记类型不正确' })
  category?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ArrayMaxSize(20, { message: '最多上传20张图片' })
  images?: string[];

  @IsOptional()
  @ValidateIf((_, value) => value !== null)
  @IsString()
  @MaxLength(255)
  location?: string | null;

  @IsOptional()
  @ValidateIf((_, value) => value !== null)
  @IsNumber()
  latitude?: number | null;

  @IsOptional()
  @ValidateIf((_, value) => value !== null)
  @IsNumber()
  longitude?: number | null;

  @IsOptional()
  @IsBoolean()
  isVisible?: boolean;

  @IsOptional()
  @IsEnum(PostStatus)
  status?: PostStatus;

  @ValidateIf((body) => body.status === PostStatus.REJECTED)
  @IsString()
  @IsNotEmpty({ message: '拒绝时需填写审核意见' })
  @MaxLength(500)
  reviewComment?: string;
}

export class ReviewPostBodySchema implements ReviewPostBodyDto {
  @IsEnum([PostStatus.APPROVED, PostStatus.REJECTED, PostStatus.OFF_SHELF])
  status: PostStatus.APPROVED | PostStatus.REJECTED | PostStatus.OFF_SHELF;

  @ValidateIf((body) => body.status === PostStatus.REJECTED)
  @IsString()
  @IsNotEmpty({ message: '拒绝时需填写审核意见' })
  @MaxLength(500)
  reviewComment?: string;
}

export class AdminUpdatePostVisibilityBodySchema
  implements AdminUpdatePostVisibilityBodyDto
{
  @IsBoolean()
  isVisible: boolean;
}
