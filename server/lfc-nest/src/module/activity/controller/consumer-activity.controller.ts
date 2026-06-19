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
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { ConsumerActivitySocialService } from '@module/activity/service/consumer-activity-social.service';
import {
  CreateActivityBodySchema,
  UpdateActivityBodySchema,
  ActivityFeedQuerySchema,
} from '@module/activity/schema/activity.schema';
import { OptionalJwtAuthGuard } from '@shared/guard/optional-jwt-auth.guard';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/activity')
export class ConsumerActivityController {
  constructor(
    private readonly consumerActivityService: ConsumerActivityService,
    private readonly consumerActivitySocialService: ConsumerActivitySocialService,
  ) {}

  @Post()
  @UseGuards(JwtAuthGuard)
  create(
    @CurrentUser('userId') userId: number,
    @Body() body: CreateActivityBodySchema,
  ) {
    return this.consumerActivityService.create(userId, body);
  }

  @Get()
  findApproved() {
    return this.consumerActivityService.findApproved();
  }

  @Get('feed')
  @UseGuards(OptionalJwtAuthGuard)
  findApprovedFeed(
    @Query() query: ActivityFeedQuerySchema,
    @CurrentUser('userId') userId?: number,
  ) {
    return this.consumerActivityService.findApprovedPaginated(
      query.page,
      query.limit,
      userId,
      query.keyword,
    );
  }

  @Get('mine')
  @UseGuards(JwtAuthGuard)
  findMine(@CurrentUser('userId') userId: number) {
    return this.consumerActivityService.findMine(userId);
  }

  @Get('participations/mine')
  @UseGuards(JwtAuthGuard)
  findMyParticipations(@CurrentUser('userId') userId: number) {
    return this.consumerActivityService.findMyParticipations(userId);
  }

  @Get(':id')
  @UseGuards(OptionalJwtAuthGuard)
  findOne(
    @Param('id', ParseIntPipe) id: number,
    @CurrentUser('userId') userId?: number,
  ) {
    return this.consumerActivityService.findOneForViewer(id, userId);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard)
  update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: UpdateActivityBodySchema,
  ) {
    return this.consumerActivityService.update(userId, id, body);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard)
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivityService.remove(userId, id);
  }

  @Patch(':id/off-shelf')
  @UseGuards(JwtAuthGuard)
  offShelf(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivityService.offShelf(userId, id);
  }

  @Patch(':id/on-shelf')
  @UseGuards(JwtAuthGuard)
  onShelf(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivityService.onShelf(userId, id);
  }

  @Post(':id/join')
  @UseGuards(JwtAuthGuard)
  join(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivityService.join(userId, id);
  }

  @Post(':id/like')
  @UseGuards(JwtAuthGuard)
  toggleLike(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivitySocialService.toggleLike(userId, id);
  }

  @Post(':id/favorite')
  @UseGuards(JwtAuthGuard)
  toggleFavorite(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivitySocialService.toggleFavorite(userId, id);
  }

  @Delete(':id/join')
  @UseGuards(JwtAuthGuard)
  leave(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivityService.leave(userId, id);
  }
}
