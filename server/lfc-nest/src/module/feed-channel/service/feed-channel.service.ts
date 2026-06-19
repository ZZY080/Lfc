import { Injectable, OnModuleInit } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { FeedChannelEntity } from '@module/feed-channel/entity/feed-channel.entity';
import {
  DEFAULT_MY_FEED_CHANNELS,
  FEED_RECOMMEND_CHANNEL,
} from '@shared/constant/feed-channel.constant';
import { POST_CATEGORIES } from '@shared/enum/post-category.enum';
import {
  FeedChannelCatalogDto,
  UserFeedChannelsDto,
} from '@module/feed-channel/dto/feed-channel.dto';

@Injectable()
export class FeedChannelService implements OnModuleInit {
  private channelNameSet: Set<string> = new Set();

  constructor(
    @InjectRepository(FeedChannelEntity)
    private readonly feedChannelRepository: Repository<FeedChannelEntity>,
  ) {}

  async onModuleInit() {
    await this.seedIfEmpty();
    await this.syncMissingFromConstants();
    await this.refreshChannelNameCache();
  }

  async getCatalog(): Promise<FeedChannelCatalogDto> {
    const channels = await this.listActiveChannels();
    const allChannels = channels.map((item) => item.name);
    const defaultMyChannels = this.pickDefaultMyChannelNames(channels);
    return {
      allChannels,
      defaultMyChannels,
      publishCategories: this.pickPublishCategories(channels),
    };
  }

  async buildUserFeedChannels(myChannels: string[] | null): Promise<UserFeedChannelsDto> {
    const channels = await this.listActiveChannels();
    const allChannels = channels.map((item) => item.name);
    const defaultMyChannels = this.pickDefaultMyChannelNames(channels);
    const normalizedMy =
      myChannels?.length
        ? this.normalizeMyChannelsSync(myChannels, allChannels)
        : [...defaultMyChannels];
    const recommendedChannels = allChannels.filter(
      (name) => !normalizedMy.includes(name),
    );
    return {
      myChannels: normalizedMy,
      allChannels,
      recommendedChannels,
      publishCategories: this.pickPublishCategories(channels),
    };
  }

  async normalizeMyChannels(channels: string[], allowedNames?: string[]): Promise<string[]> {
    const allowed =
      allowedNames ?? (await this.listActiveChannels()).map((item) => item.name);
    return this.normalizeMyChannelsSync(channels, allowed);
  }

  private normalizeMyChannelsSync(channels: string[], allowedNames: string[]): string[] {
    const allowedSet = new Set(allowedNames);
    const seen = new Set<string>();
    const result: string[] = [];

    if (allowedSet.has(FEED_RECOMMEND_CHANNEL)) {
      result.push(FEED_RECOMMEND_CHANNEL);
      seen.add(FEED_RECOMMEND_CHANNEL);
    }

    for (const raw of channels) {
      const channel = raw.trim();
      if (
        !channel ||
        channel === FEED_RECOMMEND_CHANNEL ||
        !allowedSet.has(channel) ||
        seen.has(channel)
      ) {
        continue;
      }
      seen.add(channel);
      result.push(channel);
    }

    return result;
  }

  isValidPublishCategory(category: string): boolean {
    return this.channelNameSet.has(category);
  }

  getRecommendChannelName(): string {
    return FEED_RECOMMEND_CHANNEL;
  }

  private async listActiveChannels(): Promise<FeedChannelEntity[]> {
    return this.feedChannelRepository.find({
      where: { isActive: true },
      order: { sortOrder: 'ASC', id: 'ASC' },
    });
  }

  private pickDefaultMyChannelNames(channels: FeedChannelEntity[]): string[] {
    const recommend = channels.find((item) => item.isRecommend);
    const defaults = channels
      .filter((item) => item.defaultInMy && !item.isRecommend)
      .map((item) => item.name);
    const names = recommend ? [recommend.name, ...defaults] : defaults;
    return this.normalizeMyChannelsSync(names, channels.map((item) => item.name));
  }

  private pickPublishCategories(channels: FeedChannelEntity[]): string[] {
    return channels
      .filter((item) => !item.isRecommend)
      .map((item) => item.name);
  }

  private async refreshChannelNameCache() {
    const channels = await this.listActiveChannels();
    this.channelNameSet = new Set(
      channels.filter((item) => !item.isRecommend).map((item) => item.name),
    );
  }

  private async seedIfEmpty() {
    const count = await this.feedChannelRepository.count();
    if (count > 0) {
      return;
    }

    const defaultMySet = new Set(
      DEFAULT_MY_FEED_CHANNELS.filter((name) => name !== FEED_RECOMMEND_CHANNEL),
    );
    const allNames = [FEED_RECOMMEND_CHANNEL, ...POST_CATEGORIES];
    const rows: Partial<FeedChannelEntity>[] = allNames.map((name, index) => ({
      name,
      sortOrder: index,
      isRecommend: name === FEED_RECOMMEND_CHANNEL,
      defaultInMy: defaultMySet.has(name),
      isActive: true,
    }));

    await this.feedChannelRepository.save(rows);
  }

  /** 增量补齐：枚举里有但库里没有的频道（升级后自动入库） */
  async syncMissingFromConstants() {
    const existing = await this.feedChannelRepository.find({
      select: ['name', 'sortOrder'],
    });
    const existingNames = new Set(existing.map((item) => item.name));
    const defaultMySet = new Set(
      DEFAULT_MY_FEED_CHANNELS.filter((name) => name !== FEED_RECOMMEND_CHANNEL),
    );
    let nextSort =
      existing.reduce((max, item) => Math.max(max, item.sortOrder), -1) + 1;
    const missing: string[] = POST_CATEGORIES.filter((name) => !existingNames.has(name));
    if (!existingNames.has(FEED_RECOMMEND_CHANNEL)) {
      missing.unshift(FEED_RECOMMEND_CHANNEL);
    }

    if (missing.length === 0) {
      return;
    }

    const rows = missing.map((name) => ({
      name,
      sortOrder: nextSort++,
      isRecommend: name === FEED_RECOMMEND_CHANNEL,
      defaultInMy: defaultMySet.has(name),
      isActive: true,
    }));
    await this.feedChannelRepository.save(rows);
    await this.refreshChannelNameCache();
  }
}
