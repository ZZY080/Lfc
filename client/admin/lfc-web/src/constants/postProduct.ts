import type {
  DeliveryMethod,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
} from '../types'

export const POST_PRODUCT_CATEGORY_LABELS: Record<PostProductCategory, string> = {
  GENERAL: '综合',
  SECOND_HAND: '二手',
  DIGITAL: '数码',
  BOOK: '书籍',
  DAILY: '日用',
  CLOTHING: '服饰',
  FOOD: '食品',
  BEAUTY: '美妆',
  SPORTS: '运动',
  HANDMADE: '手工',
  TICKET: '票券',
  SERVICE: '服务',
  OTHER: '其他',
}

export const PRODUCT_CONDITION_LABELS: Record<ProductCondition, string> = {
  BRAND_NEW: '全新',
  LIKE_NEW: '几乎全新',
  GOOD: '良好',
  FAIR: '一般',
}

export const DELIVERY_METHOD_LABELS: Record<DeliveryMethod, string> = {
  PICKUP: '自提',
  EXPRESS: '快递',
  BOTH: '均可',
}

export const POST_PRODUCT_STATUS_LABELS: Record<PostProductStatus, string> = {
  ON_SALE: '在售',
  SOLD: '已售出',
  OFF_SHELF: '已下架',
}
