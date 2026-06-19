import { POST_CATEGORIES } from '@shared/enum/post-category.enum';

export const FEED_RECOMMEND_CHANNEL = '推荐';

export const DEFAULT_MY_FEED_CHANNELS: string[] = [
  FEED_RECOMMEND_CHANNEL,
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
  '美食',
  '摄影',
  '户外',
  '校园生活',
  '护肤',
  '家居',
  '舞蹈',
  '手工',
];

export const ALL_FEED_CHANNELS: string[] = [
  FEED_RECOMMEND_CHANNEL,
  ...POST_CATEGORIES,
];

const ALL_FEED_CHANNEL_SET = new Set<string>(ALL_FEED_CHANNELS);

export function normalizeFeedChannels(channels: string[]): string[] {
  const seen = new Set<string>();
  const result: string[] = [FEED_RECOMMEND_CHANNEL];
  for (const raw of channels) {
    const channel = raw.trim();
    if (
      !channel ||
      channel === FEED_RECOMMEND_CHANNEL ||
      !ALL_FEED_CHANNEL_SET.has(channel) ||
      seen.has(channel)
    ) {
      continue;
    }
    seen.add(channel);
    result.push(channel);
  }
  return result;
}
