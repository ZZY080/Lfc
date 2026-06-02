import { Type } from 'class-transformer';
import { IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';
import { PostFeedQueryDto } from '@module/post/dto/consumer-post.dto';

export class PostFeedQuerySchema implements PostFeedQueryDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(30)
  limit?: number;

  @IsOptional()
  @IsIn(['recommend', 'latest'])
  sort?: 'recommend' | 'latest';

  @IsOptional()
  @IsString()
  keyword?: string;

  @IsOptional()
  @IsString()
  tab?: string;
}
