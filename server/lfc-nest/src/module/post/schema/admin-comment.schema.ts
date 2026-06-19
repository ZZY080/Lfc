import { Type } from 'class-transformer';
import { IsBoolean, IsIn, IsInt, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';
import {
  AdminCommentListQueryDto,
  AdminUpdateCommentBodyDto,
} from '@module/post/dto/admin-comment.dto';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';

export class AdminCommentListQuerySchema
  extends PaginationQuerySchema
  implements AdminCommentListQueryDto
{
  @IsOptional()
  @IsString()
  @MaxLength(64)
  keyword?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  postId?: number;

  @IsOptional()
  @IsIn(['all', 'visible', 'hidden'])
  visibility?: 'all' | 'visible' | 'hidden';
}

export class AdminUpdateCommentBodySchema implements AdminUpdateCommentBodyDto {
  @IsOptional()
  @IsString()
  @MinLength(1)
  content?: string;

  @IsOptional()
  @IsBoolean()
  isVisible?: boolean;
}
