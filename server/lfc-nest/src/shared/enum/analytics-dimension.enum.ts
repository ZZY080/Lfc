export const ANALYTICS_SCREEN_LABELS: Record<string, string> = {
  home: '首页',
  activity: '活动 Tab',
  messages: '消息 Tab',
  profile: '我的 Tab',
  main: '主页',
  search: '搜索',
  search_result: '搜索结果',
  notifications: '通知中心',
  post_detail: '笔记详情',
  activity_detail: '活动详情',
  product_detail: '商品详情',
  chat: '私信聊天',
  user_profile: '用户主页',
  publish_post: '发布笔记',
  publish_activity: '发布活动',
  edit_post: '编辑笔记',
  edit_activity: '编辑活动',
  settings: '设置',
  orders: '我的订单',
  payment_transactions: '交易记录',
  profile_search: '主页内搜索',
  location_search: '选点搜索',
  edit_profile: '编辑资料',
  my_qrcode: '我的二维码',
  profile_qr_scan: '扫码',
};

export function resolveAnalyticsScreenLabel(key: string): string {
  if (!key) return '未知';
  const base = key.split('/')[0] ?? key;
  return ANALYTICS_SCREEN_LABELS[base] ?? ANALYTICS_SCREEN_LABELS[key] ?? key;
}

export const PAYMENT_SCENARIO_LABELS: Record<string, string> = {
  product: '闲置商品',
  activity_join: '活动报名',
  post_boost: '笔记擦亮',
  activity_promote: '活动推广',
  POST_PRODUCT_PURCHASE: '闲置商品',
  ACTIVITY_JOIN: '活动报名',
  POST_BOOST: '笔记擦亮',
  ACTIVITY_PROMOTE: '活动推广',
};

export function resolvePaymentScenarioLabel(key: string): string {
  if (!key) return '未知';
  return PAYMENT_SCENARIO_LABELS[key] ?? key;
}
