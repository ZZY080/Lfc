import { PostStatus } from '@shared/enum/post-status.enum';

export interface AdminPostListQueryDto {
  page?: number;
  limit?: number;
  keyword?: string;
  /** all | visible | hidden */
  visibility?: 'all' | 'visible' | 'hidden';
  status?: PostStatus;
}

export interface ReviewPostBodyDto {
  status: PostStatus.APPROVED | PostStatus.REJECTED | PostStatus.OFF_SHELF;
  reviewComment?: string;
}

export interface AdminUpdatePostBodyDto {
  title?: string;
  content?: string;
  category?: string;
  images?: string[];
  location?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  isVisible?: boolean;
  status?: PostStatus;
  reviewComment?: string;
}

export interface AdminUpdatePostVisibilityBodyDto {
  isVisible: boolean;
}
