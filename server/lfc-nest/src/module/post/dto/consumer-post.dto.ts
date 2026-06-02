export interface CreatePostBodyDto {
  title: string;
  content: string;
}

export interface UpdatePostBodyDto {
  title?: string;
  content?: string;
}

export interface PostFeedQueryDto {
  page?: number;
  limit?: number;
  sort?: 'recommend' | 'latest';
  keyword?: string;
  tab?: string;
}

export interface PostFeedResultDto {
  items: unknown[];
  total: number;
  page: number;
  limit: number;
  hasMore: boolean;
}
