import { IsInt, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';
import { CreatePostCommentBodyDto } from '@module/post/dto/post-social.dto';

export class CreatePostCommentBodySchema implements CreatePostCommentBodyDto {
  @IsString()
  @MinLength(1, { message: '评论内容不能为空' })
  @MaxLength(500, { message: '评论不能超过500字' })
  content: string;

  @IsOptional()
  @IsInt()
  parentId?: number;
}
