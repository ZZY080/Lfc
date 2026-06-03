export interface UpdateUserProfileBodyDto {
  nickname?: string;
  bio?: string;
  avatarUrl?: string;
  coverUrl?: string;
  showCommentsPublic?: boolean;
  showFavoritesPublic?: boolean;
  showLikesPublic?: boolean;
}
