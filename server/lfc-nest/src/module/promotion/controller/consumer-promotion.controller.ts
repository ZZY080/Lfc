import { Body, Controller, Get, Param, ParseIntPipe, Post, UseGuards } from '@nestjs/common';
import { ConsumerPromotionService } from '@module/promotion/service/consumer-promotion.service';
import { PromotionOrderBodyDto } from '@module/promotion/dto/promotion.dto';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/promotion')
export class ConsumerPromotionController {
  constructor(
    private readonly consumerPromotionService: ConsumerPromotionService,
  ) {}

  @Get('config')
  getConfig() {
    return this.consumerPromotionService.getPublicConfig();
  }

  @Post('post/:postId/boost')
  @UseGuards(JwtAuthGuard)
  boostPost(
    @CurrentUser('userId') userId: number,
    @Param('postId', ParseIntPipe) postId: number,
  ) {
    return this.consumerPromotionService.boostPost(userId, postId);
  }

  @Post('post/:postId/boost/order')
  @UseGuards(JwtAuthGuard)
  createPostBoostOrder(
    @CurrentUser('userId') userId: number,
    @Param('postId', ParseIntPipe) postId: number,
    @Body() body: PromotionOrderBodyDto,
  ) {
    return this.consumerPromotionService.createPostBoostOrder(
      userId,
      postId,
      body.bidAmount,
    );
  }

  @Post('activity/:activityId/promote')
  @UseGuards(JwtAuthGuard)
  promoteActivity(
    @CurrentUser('userId') userId: number,
    @Param('activityId', ParseIntPipe) activityId: number,
  ) {
    return this.consumerPromotionService.promoteActivity(userId, activityId);
  }

  @Post('activity/:activityId/promote/order')
  @UseGuards(JwtAuthGuard)
  createActivityPromoteOrder(
    @CurrentUser('userId') userId: number,
    @Param('activityId', ParseIntPipe) activityId: number,
    @Body() body: PromotionOrderBodyDto,
  ) {
    return this.consumerPromotionService.createActivityPromoteOrder(
      userId,
      activityId,
      body.bidAmount,
    );
  }
}
