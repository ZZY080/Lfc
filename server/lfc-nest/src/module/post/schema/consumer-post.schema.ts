import {
  ArrayMaxSize,
  IsArray,
  IsEnum,
  IsNumber,
  IsOptional,
  IsString,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';
import {
  CreatePostBodyDto,
  UpdatePostBodyDto,
} from '@module/post/dto/consumer-post.dto';
import {
  DeliveryMethod,
  PostProductCategory,
  ProductCondition,
} from '@shared/enum/product.enum';
import { PostProductBodyDto } from '@module/post/dto/post-product.dto';

export class PostProductBodySchema implements PostProductBodyDto {
  @IsNumber({}, { message: '商品价格格式不正确' })
  @Min(0.01, { message: '商品价格必须大于0' })
  price: number;

  @IsOptional()
  @IsNumber({}, { message: '原价格式不正确' })
  @Min(0.01, { message: '原价必须大于0' })
  originalPrice?: number;

  @IsOptional()
  @IsEnum(PostProductCategory, { message: '商品分类不正确' })
  category?: PostProductCategory;

  @IsOptional()
  @IsEnum(ProductCondition, { message: '成色选项不正确' })
  condition?: ProductCondition;

  @IsOptional()
  @IsEnum(DeliveryMethod, { message: '交易方式不正确' })
  deliveryMethod?: DeliveryMethod;
}

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

  @IsOptional()
  @ValidateNested()
  @Type(() => PostProductBodySchema)
  product?: PostProductBodySchema;
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

  @IsOptional()
  @ValidateNested()
  @Type(() => PostProductBodySchema)
  product?: PostProductBodySchema | null;
}
