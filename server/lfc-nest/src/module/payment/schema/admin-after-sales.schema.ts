import { Type } from 'class-transformer';
import {
  IsEnum,
  IsIn,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  ValidateIf,
} from 'class-validator';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';
import { PaymentAfterSalesStatus } from '@shared/enum/payment.enum';

export class AdminAfterSalesListQuerySchema extends PaginationQuerySchema {
  @IsOptional()
  @IsEnum(PaymentAfterSalesStatus)
  status?: PaymentAfterSalesStatus;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  keyword?: string;
}

export class AdminReviewAfterSalesBodySchema {
  @IsIn(['approve', 'reject'])
  action: 'approve' | 'reject';

  @ValidateIf((body) => body.action === 'reject')
  @IsString()
  @IsNotEmpty({ message: '拒绝时需填写审核意见' })
  @MaxLength(500)
  rejectReason?: string;
}
