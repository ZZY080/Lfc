import {
  IsEnum,
  IsNumber,
  IsOptional,
  Min,
  ValidateIf,
} from 'class-validator';
import { AdminUpdatePostProductBodyDto } from '@module/post/dto/admin-post-detail.dto';
import {
  DeliveryMethod,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
} from '@shared/enum/product.enum';

export class AdminUpdatePostProductBodySchema
  implements AdminUpdatePostProductBodyDto
{
  @IsOptional()
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0.01)
  price?: number;

  @IsOptional()
  @ValidateIf((_, value) => value !== null)
  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0.01)
  originalPrice?: number | null;

  @IsOptional()
  @IsEnum(PostProductCategory)
  category?: PostProductCategory;

  @IsOptional()
  @IsEnum(ProductCondition)
  condition?: ProductCondition;

  @IsOptional()
  @IsEnum(DeliveryMethod)
  deliveryMethod?: DeliveryMethod;

  @IsOptional()
  @IsEnum(PostProductStatus)
  status?: PostProductStatus;
}
