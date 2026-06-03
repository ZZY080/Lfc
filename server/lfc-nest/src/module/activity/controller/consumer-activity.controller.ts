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
import {
  CreateActivityBodySchema,
  UpdateActivityBodySchema,
} from '@module/activity/schema/activity.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';

@Controller('consumer/activity')
export class ConsumerActivityController {
  constructor(
    private readonly consumerActivityService: ConsumerActivityService,
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
  findApprovedFeed(@Query() query: PaginationQuerySchema) {
    return this.consumerActivityService.findApprovedPaginated(
      query.page,
      query.limit,
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
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.consumerActivityService.findOne(id);
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

  @Post(':id/join')
  @UseGuards(JwtAuthGuard)
  join(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerActivityService.join(userId, id);
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
