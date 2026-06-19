export interface AdminCommentListQueryDto {
  page?: number;
  limit?: number;
  keyword?: string;
  postId?: number;
  visibility?: 'all' | 'visible' | 'hidden';
}

export interface AdminUpdateCommentBodyDto {
  content?: string;
  isVisible?: boolean;
}
