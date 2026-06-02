import { IsOptional, IsString, MinLength } from 'class-validator';
import {
  CreatePostBodyDto,
  UpdatePostBodyDto,
} from '@module/post/dto/consumer-post.dto';

export class CreatePostBodySchema implements CreatePostBodyDto {
  @IsString()
  @MinLength(1, { message: '标题不能为空' })
  title: string;

  @IsString()
  @MinLength(1, { message: '内容不能为空' })
  content: string;
}

export class UpdatePostBodySchema implements UpdatePostBodyDto {
  @IsOptional()
  @IsString()
  @MinLength(1, { message: '标题不能为空' })
  title?: string;

  @IsOptional()
  @IsString()
  @MinLength(1, { message: '内容不能为空' })
  content?: string;
}
