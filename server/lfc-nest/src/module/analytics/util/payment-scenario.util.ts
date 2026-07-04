import { PaymentBizType } from '@shared/enum/payment.enum';

export function paymentScenarioFromBizType(
  bizType: PaymentBizType | string,
): string {
  switch (bizType) {
    case PaymentBizType.POST_PRODUCT_PURCHASE:
      return 'product';
    case PaymentBizType.ACTIVITY_JOIN:
      return 'activity_join';
    case PaymentBizType.POST_BOOST:
      return 'post_boost';
    case PaymentBizType.ACTIVITY_PROMOTE:
      return 'activity_promote';
    default:
      return String(bizType).toLowerCase();
  }
}
