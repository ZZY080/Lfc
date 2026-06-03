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
