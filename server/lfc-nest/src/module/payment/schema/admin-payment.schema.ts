import { Type } from 'class-transformer';
import { IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';
import { AdminPaymentOrderListQueryDto } from '@module/admin/dto/admin-query.dto';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';
import {
  PaymentBizType,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';

export class AdminPaymentOrderListQuerySchema
  extends PaginationQuerySchema
  implements AdminPaymentOrderListQueryDto
{
  @IsOptional()
  @IsString()
  @MaxLength(64)
  keyword?: string;

  @IsOptional()
  @IsEnum(PaymentOrderStatus)
  status?: PaymentOrderStatus;

  @IsOptional()
  @IsEnum(PaymentBizType)
  bizType?: PaymentBizType;
}
