import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import {
  PaymentAfterSalesStatus,
  PaymentBizType,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { isPlatformDirectRevenueBizType } from '@module/payment/util/payment-order.util';

export interface FulfillmentStepDto {
  label: string;
  done: boolean;
  active: boolean;
}

export interface OrderFulfillmentGuaranteeDto {
  title: string;
  summary: string;
  steps: FulfillmentStepDto[];
}

function formatDateTime(value: Date | string | null | undefined): string | null {
  if (!value) {
    return null;
  }
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function buildSteps(
  labels: [string, string, string, string],
  stage: 0 | 1 | 2 | 3 | 4,
): FulfillmentStepDto[] {
  return labels.map((label, index) => ({
    label,
    done: stage > index,
    active: stage === index,
  }));
}

export function buildOrderFulfillmentGuarantee(input: {
  order: PaymentOrderEntity;
  post?: PostEntity;
  activity?: ActivityEntity;
  afterSales?: PaymentAfterSalesEntity;
  autoConfirmDays: number;
}): OrderFulfillmentGuaranteeDto {
  const { order, post, activity, afterSales, autoConfirmDays } = input;
  const isProduct = order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE;
  const isActivity = order.bizType === PaymentBizType.ACTIVITY_JOIN;
  const isPostBoost = order.bizType === PaymentBizType.POST_BOOST;
  const isActivityPromote = order.bizType === PaymentBizType.ACTIVITY_PROMOTE;

  if (isPostBoost || isActivityPromote) {
    const promotionSteps: [string, string, string, string] = isPostBoost
      ? ['支付下单', '立即擦亮', '优先展示', '订单完成']
      : ['支付下单', '立即推广', '优先展示', '订单完成'];
    const bizTitle = post?.title ?? activity?.title ?? order.subject;

    switch (order.status) {
      case PaymentOrderStatus.PENDING:
        return {
          title: '服务说明',
          summary: isPostBoost
            ? '支付成功后笔记立即进入擦亮状态，无需等待确认收货'
            : '支付成功后活动立即进入推广位，无需等待活动结束',
          steps: buildSteps(promotionSteps, 0),
        };
      case PaymentOrderStatus.PAID:
        return {
          title: '服务说明',
          summary: isPostBoost
            ? `正在生效：${bizTitle}`
            : `正在推广：${bizTitle}`,
          steps: buildSteps(promotionSteps, 2),
        };
      case PaymentOrderStatus.SETTLED:
        return {
          title: '服务说明',
          summary: isPostBoost
            ? '擦亮已生效，可在笔记详情查看推广状态'
            : '推广已生效，可在活动 Tab 查看推广标识',
          steps: buildSteps(promotionSteps, 4),
        };
      case PaymentOrderStatus.REFUNDED:
        return {
          title: '服务说明',
          summary: '订单已退款，擦亮/推广未生效或已撤销',
          steps: buildSteps(promotionSteps, 4),
        };
      case PaymentOrderStatus.CLOSED:
        return {
          title: '服务说明',
          summary: '订单已关闭，未发生实际扣款',
          steps: buildSteps(promotionSteps, 0),
        };
      default:
        return {
          title: '服务说明',
          summary: bizTitle,
          steps: buildSteps(promotionSteps, 0),
        };
    }
  }

  const productSteps: [string, string, string, string] = [
    '支付下单',
    '平台托管',
    '确认收货',
    '分账完成',
  ];
  const activitySteps: [string, string, string, string] = [
    '支付报名',
    '锁定名额',
    '活动履约',
    '分账完成',
  ];
  const stepLabels = isProduct ? productSteps : activitySteps;

  if (afterSales) {
    switch (afterSales.status) {
      case PaymentAfterSalesStatus.PENDING:
      case PaymentAfterSalesStatus.APPROVED:
      case PaymentAfterSalesStatus.REFUNDING:
        return {
          title: '履约保障',
          summary: '售后处理中，审核通过后将原路退款',
          steps: buildSteps(stepLabels, 4),
        };
      case PaymentAfterSalesStatus.REFUNDED:
        return {
          title: '履约保障',
          summary: '退款已完成，款项已原路退回',
          steps: buildSteps(stepLabels, 4),
        };
      default:
        break;
    }
  }

  switch (order.status) {
    case PaymentOrderStatus.PENDING:
      return {
        title: '履约保障',
        summary: isProduct
          ? '支付后款项由平台托管，确认收货后才会打给卖家；未收货可申请退款'
          : '支付后锁定活动名额，款项由平台托管，活动开始前可申请退款',
        steps: buildSteps(stepLabels, 0),
      };
    case PaymentOrderStatus.PAID:
      if (isProduct) {
        const deadline = formatDateTime(order.autoConfirmAt);
        return {
          title: '履约保障',
          summary: deadline
            ? `款项托管中，请在 ${deadline} 前确认收货；${autoConfirmDays} 天未确认将自动完成`
            : `款项托管中，确认收货后才会打给卖家；${autoConfirmDays} 天未确认将自动完成`,
          steps: buildSteps(stepLabels, 2),
        };
      }
      if (isActivity) {
        const endLabel = formatDateTime(activity?.endTime ?? order.autoConfirmAt);
        const startLabel = formatDateTime(activity?.startTime);
        const location = activity?.location?.trim();
        const summaryParts = [
          '报名成功，款项托管中',
          endLabel ? `活动结束（${endLabel}）后分账给发起人` : '活动结束后分账给发起人',
          startLabel ? `活动开始 ${startLabel}` : null,
          location ? `地点 ${location}` : null,
          '活动开始前可申请退款',
        ].filter(Boolean);
        return {
          title: '履约保障',
          summary: summaryParts.join(' · '),
          steps: buildSteps(stepLabels, 2),
        };
      }
      break;
    case PaymentOrderStatus.CONFIRMED:
      return {
        title: '履约保障',
        summary: isProduct
          ? '已确认收货，正在分账给卖家'
          : '活动履约完成，正在分账给发起人',
        steps: buildSteps(stepLabels, 3),
      };
    case PaymentOrderStatus.SETTLED:
      return {
        title: '履约保障',
        summary: isProduct
          ? '交易完成，款项已分账给卖家'
          : '活动已完成，款项已分账给发起人',
        steps: buildSteps(stepLabels, 4),
      };
    case PaymentOrderStatus.REFUNDED:
      return {
        title: '履约保障',
        summary: '订单已退款，款项已原路退回',
        steps: buildSteps(stepLabels, 4),
      };
    case PaymentOrderStatus.CLOSED:
      return {
        title: '履约保障',
        summary: '订单已关闭，未发生实际交易',
        steps: buildSteps(stepLabels, 0),
      };
    case PaymentOrderStatus.FAILED:
      return {
        title: '履约保障',
        summary: '支付失败，请重新下单',
        steps: buildSteps(stepLabels, 0),
      };
    default:
      break;
  }

  const bizTitle = post?.title ?? activity?.title ?? order.subject;
  return {
    title: '履约保障',
    summary: bizTitle,
    steps: buildSteps(stepLabels, 0),
  };
}

export function buildCounterpartyRoleLabel(bizType: PaymentBizType): string {
  if (isPlatformDirectRevenueBizType(bizType)) {
    return '收款方';
  }
  return bizType === PaymentBizType.ACTIVITY_JOIN ? '发起人' : '卖家';
}

export function canApplyActivityAfterSales(
  order: PaymentOrderEntity,
  activity?: ActivityEntity,
): boolean {
  if (order.bizType !== PaymentBizType.ACTIVITY_JOIN) {
    return false;
  }
  if (order.status !== PaymentOrderStatus.PAID) {
    return order.status === PaymentOrderStatus.SETTLED;
  }
  if (!activity?.startTime) {
    return true;
  }
  const startTime = new Date(activity.startTime);
  return !Number.isNaN(startTime.getTime()) && startTime.getTime() > Date.now();
}
