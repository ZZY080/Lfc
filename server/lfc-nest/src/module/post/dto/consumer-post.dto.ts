import { PostProductBodyDto } from '@module/post/dto/post-product.dto';

export interface CreatePostBodyDto {
  title?: string;
  category?: string;
  content?: string;
  images?: string[];
  product?: PostProductBodyDto;
  latitude?: number;
  longitude?: number;
  location?: string;
}

export interface UpdatePostBodyDto {
  title?: string;
  category?: string;
  content?: string;
  images?: string[];
  product?: PostProductBodyDto | null;
  latitude?: number;
  longitude?: number;
  location?: string;
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
