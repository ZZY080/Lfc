import { PostProductBodyDto } from '@module/post/dto/post-product.dto';

export interface CreatePostBodyDto {
  title?: string;
  content?: string;
  images?: string[];
  product?: PostProductBodyDto;
}

export interface UpdatePostBodyDto {
  title?: string;
  content?: string;
  images?: string[];
  product?: PostProductBodyDto | null;
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

export interface UploadImageResultDto {
  url: string;
}
