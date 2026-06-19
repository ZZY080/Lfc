import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  Query,
  UseGuards,
} from '@nestjs/common';
import { AdminPostService } from '@module/post/service/admin-post.service';
import {
  AdminPostListQuerySchema,
  AdminUpdatePostBodySchema,
  AdminUpdatePostVisibilityBodySchema,
  ReviewPostBodySchema,
} from '@module/post/schema/admin-post.schema';
import { AdminUpdatePostProductBodySchema } from '@module/post/schema/admin-post-relation.schema';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/post')
@UseGuards(JwtAuthGuard)
export class AdminPostController {
  constructor(private readonly adminPostService: AdminPostService) {}

  @Get()
  findAll(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminPostListQuerySchema,
  ) {
    return this.adminPostService.findAll(userId, query);
  }

  @Get('pending')
  findPending(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminPostListQuerySchema,
  ) {
    return this.adminPostService.findPending(userId, query);
  }

  @Get(':id/detail')
  findDetail(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminPostService.findDetail(userId, id);
  }

  @Get(':id/comments')
  findComments(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.adminPostService.findComments(
      userId,
      id,
      query.page,
      query.limit,
    );
  }

  @Get(':id/likes')
  findLikes(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.adminPostService.findLikes(userId, id, query.page, query.limit);
  }

  @Get(':id/favorites')
  findFavorites(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.adminPostService.findFavorites(
      userId,
      id,
      query.page,
      query.limit,
    );
  }

  @Patch(':id/product')
  updateProduct(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdatePostProductBodySchema,
  ) {
    return this.adminPostService.updateProduct(userId, id, body);
  }

  @Delete(':id/product')
  removeProduct(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminPostService.removeProduct(userId, id);
  }

  @Delete(':id/likes/:likeId')
  removeLike(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Param('likeId', ParseIntPipe) likeId: number,
  ) {
    return this.adminPostService.removeLike(userId, id, likeId);
  }

  @Delete(':id/favorites/:favoriteId')
  removeFavorite(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Param('favoriteId', ParseIntPipe) favoriteId: number,
  ) {
    return this.adminPostService.removeFavorite(userId, id, favoriteId);
  }

  @Get(':id')
  findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminPostService.findOne(userId, id);
  }

  @Patch(':id')
  update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdatePostBodySchema,
  ) {
    return this.adminPostService.update(userId, id, body);
  }

  @Patch(':id/review')
  review(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: ReviewPostBodySchema,
  ) {
    return this.adminPostService.review(userId, id, body);
  }

  @Patch(':id/visibility')
  updateVisibility(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdatePostVisibilityBodySchema,
  ) {
    return this.adminPostService.updateVisibility(userId, id, body);
  }

  @Delete(':id')
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminPostService.remove(userId, id);
  }
}
