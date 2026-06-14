export interface CreatePostCommentBodyDto {
  content: string;
  parentId?: number;
}

export interface PostSocialStateDto {
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  isLiked: boolean;
  isFavorited: boolean;
}

export interface TogglePostLikeResultDto extends PostSocialStateDto {}

export interface TogglePostFavoriteResultDto extends PostSocialStateDto {}

export interface CommentLikeStateDto {
  commentId: number;
  likeCount: number;
  isLiked: boolean;
}

export interface PostCommentThreadDto {
  id: number;
  postId: number;
  userId: number;
  content: string;
  parentId: number | null;
  rootId: number | null;
  likeCount: number;
  isLiked: boolean;
  createdAt: Date;
  author: unknown;
  replyCount: number;
  previewReplies: PostCommentThreadDto[];
}
