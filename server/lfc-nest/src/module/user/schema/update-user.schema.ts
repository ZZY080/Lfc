import { IsBoolean, IsOptional, IsString, MaxLength } from 'class-validator';
import { UpdateUserProfileBodyDto } from '@module/user/dto/update-user.dto';

export class UpdateUserProfileBodySchema implements UpdateUserProfileBodyDto {
  @IsOptional()
  @IsString()
  @MaxLength(50, { message: '昵称不能超过50字' })
  nickname?: string;

  @IsOptional()
  @IsString()
  @MaxLength(200, { message: '简介不能超过200字' })
  bio?: string;

  @IsOptional()
  @IsString()
  avatarUrl?: string;

  @IsOptional()
  @IsString()
  coverUrl?: string;

  @IsOptional()
  @IsBoolean()
  showCommentsPublic?: boolean;

  @IsOptional()
  @IsBoolean()
  showFavoritesPublic?: boolean;

  @IsOptional()
  @IsBoolean()
  showLikesPublic?: boolean;
}
