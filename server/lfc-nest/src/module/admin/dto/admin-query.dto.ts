import {
  PaymentBizType,
  PaymentChannel,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';

export interface AdminPaymentOrderListQueryDto {
  page?: number;
  limit?: number;
  keyword?: string;
  status?: PaymentOrderStatus;
  bizType?: PaymentBizType;
}

export interface AdminActivityListQueryDto {
  page?: number;
  limit?: number;
  keyword?: string;
  status?: string;
}
