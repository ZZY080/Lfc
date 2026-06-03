import { ArrayMaxSize, IsArray, IsOptional, IsString, MaxLength } from 'class-validator';
import {
  CreatePostBodyDto,
  UpdatePostBodyDto,
} from '@module/post/dto/consumer-post.dto';

export class CreatePostBodySchema implements CreatePostBodyDto {
  @IsOptional()
  @IsString()
  @MaxLength(100, { message: '标题不能超过100字' })
  title?: string;

  @IsOptional()
  @IsString()
  @MaxLength(5000, { message: '正文不能超过5000字' })
  content?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ArrayMaxSize(20, { message: '最多上传20张图片' })
  images?: string[];
}

export class UpdatePostBodySchema implements UpdatePostBodyDto {
  @IsOptional()
  @IsString()
  @MaxLength(100, { message: '标题不能超过100字' })
  title?: string;

  @IsOptional()
  @IsString()
  @MaxLength(5000, { message: '正文不能超过5000字' })
  content?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ArrayMaxSize(20, { message: '最多上传20张图片' })
  images?: string[];
}
