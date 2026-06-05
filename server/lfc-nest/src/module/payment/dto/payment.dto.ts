import { PaymentChannel } from '@shared/enum/payment.enum';
import { PaymentTransactionType } from '@shared/enum/payment.enum';
import { AlipayAppPayResult } from '@integration/alipay/dto/alipay.dto';

export interface CreatePaymentResultDto {
  outTradeNo: string;
  channel: PaymentChannel;
  amount: string;
  platformFee: string;
  payeeAmount: string;
  platformFeeRateLabel: string;
  subject: string;
  status: string;
  /** C2C 收款方用户 ID */
  payeeId: number;
  alipay: AlipayAppPayResult;
}

export interface PaymentConfigDto {
  platformFeeRate: number;
  platformFeeRateLabel: string;
  platformFeeMin: number;
  autoConfirmDays: number;
}

/** @deprecated 使用 CreatePaymentResultDto */
export type CreateActivityPaymentResultDto = CreatePaymentResultDto;

export interface PaymentOrderDetailDto {
  outTradeNo: string;
  channel: PaymentChannel;
  amount: string;
  platformFee: string;
  payeeAmount: string;
  subject: string;
  status: string;
  bizType: string;
  bizId: number;
  payeeId: number;
  tradeNo: string | null;
  paidAt: Date | null;
  confirmedAt: Date | null;
  settledAt: Date | null;
  autoConfirmAt: Date | null;
  canConfirmReceipt: boolean;
  createdAt: Date;
}

export interface PaymentOrderListItemDto {
  outTradeNo: string;
  amount: string;
  subject: string;
  status: string;
  statusLabel: string;
  bizType: string;
  bizId: number;
  bizTitle: string;
  coverImage: string | null;
  payeeId: number;
  payeeName: string;
  payeeAvatarUrl: string | null;
  paidAt: Date | null;
  createdAt: Date;
  canPay: boolean;
  canConfirmReceipt: boolean;
  canReview: boolean;
  canApplyAfterSales: boolean;
  hasReview: boolean;
  afterSalesStatus: string | null;
}

export interface PaymentOrderTabCountsDto {
  all: number;
  pendingPayment: number;
  awaitingReceipt: number;
  review: number;
  afterSales: number;
}

export interface CreateOrderReviewDto {
  rating: number;
  content?: string;
}

export interface ApplyAfterSalesDto {
  reason: string;
}

export interface PaymentTransactionItemDto {
  txKey: string;
  type: PaymentTransactionType;
  typeLabel: string;
  direction: 'OUT' | 'IN';
  amount: string;
  outTradeNo: string;
  tradeNo: string | null;
  subject: string;
  bizType: string;
  bizId: number;
  bizTitle: string;
  coverImage: string | null;
  counterpartyName: string;
  occurredAt: Date;
}
