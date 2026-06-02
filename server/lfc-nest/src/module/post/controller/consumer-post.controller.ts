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
import {
  CreatePostBodySchema,
  UpdatePostBodySchema,
} from '@module/post/schema/consumer-post.schema';
import { PostFeedQuerySchema } from '@module/post/schema/post-feed.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/post')
export class ConsumerPostController {
  constructor(private readonly consumerPostService: ConsumerPostService) {}

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
  findFeed(@Query() query: PostFeedQuerySchema) {
    return this.consumerPostService.findFeed(query);
  }

  @Get('mine')
  @UseGuards(JwtAuthGuard)
  findMine(@CurrentUser('userId') userId: number) {
    return this.consumerPostService.findMine(userId);
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.consumerPostService.findOne(id);
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
}
