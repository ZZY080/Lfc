import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ConsumerUserService } from '@module/user/service/consumer-user.service';
import { OptionalJwtAuthGuard } from '@shared/guard/optional-jwt-auth.guard';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';
import { UpdateUserProfileBodySchema } from '@module/user/schema/update-user.schema';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';

@Controller('consumer/user')
export class ConsumerUserController {
  constructor(private readonly consumerUserService: ConsumerUserService) {}

  @Get('me')
  @UseGuards(JwtAuthGuard)
  getMe(@CurrentUser('userId') userId: number) {
    return this.consumerUserService.getMe(userId);
  }

  @Patch('me')
  @UseGuards(JwtAuthGuard)
  updateMe(
    @CurrentUser('userId') userId: number,
    @Body() body: UpdateUserProfileBodySchema,
  ) {
    return this.consumerUserService.updateMe(userId, body);
  }

  @Get('me/favorites')
  @UseGuards(JwtAuthGuard)
  getMyFavorites(
    @CurrentUser('userId') userId: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.consumerUserService.findMyFavorites(
      userId,
      query.page,
      query.limit,
    );
  }

  @Get('me/likes')
  @UseGuards(JwtAuthGuard)
  getMyLikes(
    @CurrentUser('userId') userId: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.consumerUserService.findMyLikes(userId, query.page, query.limit);
  }

  @Get('me/comments')
  @UseGuards(JwtAuthGuard)
  getMyComments(
    @CurrentUser('userId') userId: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.consumerUserService.findMyComments(
      userId,
      query.page,
      query.limit,
    );
  }

  @Get('lfc/:lfcNo/profile')
  @UseGuards(OptionalJwtAuthGuard)
  getProfileByLfcNo(
    @Param('lfcNo') lfcNo: string,
    @CurrentUser('userId') viewerId?: number,
  ) {
    return this.consumerUserService.getProfileByLfcNo(lfcNo, viewerId);
  }

  @Get(':id/posts')
  @UseGuards(OptionalJwtAuthGuard)
  getUserPosts(
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
    @CurrentUser('userId') viewerId?: number,
  ) {
    return this.consumerUserService.findUserPosts(
      id,
      viewerId,
      query.page,
      query.limit,
    );
  }

  @Get(':id/activities')
  @UseGuards(OptionalJwtAuthGuard)
  getUserActivities(
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
    @CurrentUser('userId') viewerId?: number,
  ) {
    return this.consumerUserService.findUserActivities(
      id,
      viewerId,
      query.page,
      query.limit,
    );
  }

  @Get(':id/favorites')
  @UseGuards(OptionalJwtAuthGuard)
  getUserFavorites(
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
    @CurrentUser('userId') viewerId?: number,
  ) {
    return this.consumerUserService.findUserFavorites(
      id,
      viewerId,
      query.page,
      query.limit,
    );
  }

  @Get(':id/likes')
  @UseGuards(OptionalJwtAuthGuard)
  getUserLikes(
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
    @CurrentUser('userId') viewerId?: number,
  ) {
    return this.consumerUserService.findUserLikes(
      id,
      viewerId,
      query.page,
      query.limit,
    );
  }

  @Get(':id/comments')
  @UseGuards(OptionalJwtAuthGuard)
  getUserComments(
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
    @CurrentUser('userId') viewerId?: number,
  ) {
    return this.consumerUserService.findUserComments(
      id,
      viewerId,
      query.page,
      query.limit,
    );
  }

  @Get(':id/profile')
  @UseGuards(OptionalJwtAuthGuard)
  getProfile(
    @Param('id', ParseIntPipe) id: number,
    @CurrentUser('userId') viewerId?: number,
  ) {
    return this.consumerUserService.getProfile(id, viewerId);
  }

  @Post(':id/follow')
  @UseGuards(JwtAuthGuard)
  follow(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerUserService.toggleFollow(userId, id);
  }

  @Delete(':id/follow')
  @UseGuards(JwtAuthGuard)
  unfollow(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerUserService.toggleFollow(userId, id);
  }
}
