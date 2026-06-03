export interface UserProfileDto {
  id: number;
  lfcNo: string;
  studentId: string;
  nickname: string | null;
  bio: string | null;
  avatarUrl: string | null;
  coverUrl: string | null;
  postCount: number;
  followingCount: number;
  followerCount: number;
  likeAndFavoriteCount: number;
  isFollowing: boolean;
  isSelf: boolean;
  showCommentsPublic: boolean;
  showFavoritesPublic: boolean;
  showLikesPublic: boolean;
}

export interface UserProfileDetailDto extends UserProfileDto {
  posts: unknown[];
  activities: unknown[];
  participationCount: number;
}
