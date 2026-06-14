/** 与客户端 Feed 频道一致，不含「推荐」 */
export const POST_CATEGORIES = [
  '旅行',
  '职场',
  '情感',
  '读书',
  '文化',
  '社科',
  '学习',
  '科学科普',
  '心理',
  '体育',
  '穿搭',
  '汽车',
  '美食',
  '摄影',
  '影视',
  '户外',
  '校园生活',
  '护肤',
  '家居',
  '舞蹈',
  '机车',
  '手工',
  '游戏',
  '科技数码',
  '壁纸',
  '婚礼',
  '竞技体育',
  '动漫',
  '艺术',
  '健身塑型',
  '露营',
  '好物',
  '活动',
  '生活',
] as const;

export type PostCategory = (typeof POST_CATEGORIES)[number];

export const DEFAULT_POST_CATEGORY: PostCategory = '校园生活';

export const POST_CATEGORY_SET = new Set<string>(POST_CATEGORIES);
