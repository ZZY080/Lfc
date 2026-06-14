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
import { ConsumerPostService } from '@module/post/service/consumer-post.service';
import { ConsumerPostSocialService } from '@module/post/service/consumer-post-social.service';
import {
  CreatePostBodySchema,
  UpdatePostBodySchema,
} from '@module/post/schema/consumer-post.schema';
import { CreatePostCommentBodySchema } from '@module/post/schema/post-social.schema';
import {
  PostCommentQuerySchema,
  PostCommentReplyQuerySchema,
} from '@module/post/schema/post-comment.schema';
import { PostFeedQuerySchema } from '@module/post/schema/post-feed.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { OptionalJwtAuthGuard } from '@shared/guard/optional-jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/post')
export class ConsumerPostController {
  constructor(
    private readonly consumerPostService: ConsumerPostService,
    private readonly consumerPostSocialService: ConsumerPostSocialService,
  ) {}

  @Post()
  @UseGuards(JwtAuthGuard)
  create(
    @CurrentUser('userId') userId: number,
    @Body() body: CreatePostBodySchema,
  ) {
    return this.consumerPostService.create(userId, body);
  }

  @Get()
  findAll() {
    return this.consumerPostService.findAll();
  }

  @Get('feed')
  @UseGuards(OptionalJwtAuthGuard)
  findFeed(
    @Query() query: PostFeedQuerySchema,
    @CurrentUser('userId') userId?: number,
  ) {
    return this.consumerPostService.findFeed(query, userId);
  }

  @Get('mine')
  @UseGuards(JwtAuthGuard)
  findMine(
    @CurrentUser('userId') userId: number,
    @Query('keyword') keyword?: string,
  ) {
    return this.consumerPostService.findMine(userId, keyword);
  }

  @Post(':id/like')
  @UseGuards(JwtAuthGuard)
  toggleLike(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerPostSocialService.toggleLike(userId, id);
  }

  @Post(':id/favorite')
  @UseGuards(JwtAuthGuard)
  toggleFavorite(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerPostSocialService.toggleFavorite(userId, id);
  }

  @Get(':id/comments')
  @UseGuards(OptionalJwtAuthGuard)
  findComments(
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PostCommentQuerySchema,
    @CurrentUser('userId') userId?: number,
  ) {
    return this.consumerPostSocialService.findComments(
      id,
      query.page,
      query.limit,
      query.sort ?? 'default',
      userId,
    );
  }

  @Get(':id/comments/:commentId/replies')
  @UseGuards(OptionalJwtAuthGuard)
  findCommentReplies(
    @Param('id', ParseIntPipe) id: number,
    @Param('commentId', ParseIntPipe) commentId: number,
    @Query() query: PostCommentReplyQuerySchema,
    @CurrentUser('userId') userId?: number,
  ) {
    return this.consumerPostSocialService.findCommentReplies(
      id,
      commentId,
      query.page,
      query.limit,
      userId,
    );
  }

  @Post(':id/comments')
  @UseGuards(JwtAuthGuard)
  createComment(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: CreatePostCommentBodySchema,
  ) {
    return this.consumerPostSocialService.createComment(userId, id, body);
  }

  @Delete('comments/:commentId')
  @UseGuards(JwtAuthGuard)
  removeComment(
    @CurrentUser('userId') userId: number,
    @Param('commentId', ParseIntPipe) commentId: number,
  ) {
    return this.consumerPostSocialService.removeComment(userId, commentId);
  }

  @Post('comments/:commentId/like')
  @UseGuards(JwtAuthGuard)
  toggleCommentLike(
    @CurrentUser('userId') userId: number,
    @Param('commentId', ParseIntPipe) commentId: number,
  ) {
    return this.consumerPostSocialService.toggleCommentLike(userId, commentId);
  }

  @Get(':id')
  @UseGuards(OptionalJwtAuthGuard)
  findOne(
    @Param('id', ParseIntPipe) id: number,
    @CurrentUser('userId') userId?: number,
  ) {
    return this.consumerPostService.findOne(id, userId);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard)
  update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: UpdatePostBodySchema,
  ) {
    return this.consumerPostService.update(userId, id, body);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard)
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerPostService.remove(userId, id);
  }

  @Patch(':id/product/off-shelf')
  @UseGuards(JwtAuthGuard)
  offShelfProduct(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerPostService.offShelfProduct(userId, id);
  }

  @Patch(':id/product/on-shelf')
  @UseGuards(JwtAuthGuard)
  onShelfProduct(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerPostService.onShelfProduct(userId, id);
  }

  @Patch(':id/off-shelf')
  @UseGuards(JwtAuthGuard)
  offShelf(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerPostService.offShelf(userId, id);
  }

  @Patch(':id/on-shelf')
  @UseGuards(JwtAuthGuard)
  onShelf(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerPostService.onShelf(userId, id);
  }
}
