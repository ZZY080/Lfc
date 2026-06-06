import type { CreatePaymentResultDto } from '@module/payment/dto/payment.dto';
import { IsOptional, IsString, Matches } from 'class-validator';

export interface PromotionMetaDto {
  isActive: boolean;
  label: string | null;
  badge: string | null;
  until: string | null;
  canApply: boolean;
  nextAvailableAt: string | null;
  cooldownHours: number;
  durationHours: number;
  requiresPayment: boolean;
  price: string | null;
  minBidAmount: string | null;
  bidAmount: string | null;
}

export interface PromotionConfigDto {
  postBoostHours: number;
  postCooldownHours: number;
  postActiveLabel: string;
  postActionLabel: string;
  activityPromoteHours: number;
  activityCooldownHours: number;
  activityMaxFeedSlots: number;
  activityActiveLabel: string;
  activityActionLabel: string;
  postMaxFeedSlots: number;
  paidEnabled: boolean;
  postBoostPrice: string;
  activityPromotePrice: string;
  bidIncrement: string;
  lowestPostBoostBid: string | null;
  lowestActivityPromoteBid: string | null;
  postSlotsFull: boolean;
  activitySlotsFull: boolean;
}

export interface PromotionActionResultDto {
  message: string;
  promotion: PromotionMetaDto;
}

export class PromotionOrderBodyDto {
  @IsOptional()
  @IsString()
  @Matches(/^\d+(\.\d{1,2})?$/, { message: '出价格式不正确' })
  bidAmount?: string;
}

export interface PromotionPaymentResultDto {
  message: string;
  payment: CreatePaymentResultDto;
  promotion: PromotionMetaDto;
}
