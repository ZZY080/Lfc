import { Type } from 'class-transformer';
import {
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';
import { PaymentOrderTab } from '@shared/enum/payment.enum';

export class PaymentOrderListQuerySchema {
  @IsOptional()
  @IsEnum(PaymentOrderTab)
  tab?: PaymentOrderTab = PaymentOrderTab.ALL;

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
}

export class CreateOrderReviewBodySchema {
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(5)
  rating: number;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  content?: string;
}

export class ApplyAfterSalesBodySchema {
  @IsString()
  @MaxLength(255)
  reason: string;
}

export type CreateOrderReviewBodyDto = CreateOrderReviewBodySchema;
export type ApplyAfterSalesBodyDto = ApplyAfterSalesBodySchema;
