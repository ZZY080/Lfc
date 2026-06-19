export interface FeedChannelCatalogDto {
  allChannels: string[];
  defaultMyChannels: string[];
  publishCategories: string[];
}

export interface UserFeedChannelsDto {
  myChannels: string[];
  allChannels: string[];
  recommendedChannels: string[];
  publishCategories: string[];
}

export interface UpdateFeedChannelsBodyDto {
  channels: string[];
}
