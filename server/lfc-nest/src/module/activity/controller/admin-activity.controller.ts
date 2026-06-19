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
import { AdminActivityService } from '@module/activity/service/admin-activity.service';
import {
  AdminActivityListQuerySchema,
  AdminUpdateActivityBodySchema,
  ReviewActivityBodySchema,
} from '@module/activity/schema/activity.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/activity')
@UseGuards(JwtAuthGuard)
export class AdminActivityController {
  constructor(private readonly adminActivityService: AdminActivityService) {}

  @Get('pending')
  findPending(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminActivityListQuerySchema,
  ) {
    return this.adminActivityService.findPending(userId, query);
  }

  @Get()
  findAll(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminActivityListQuerySchema,
  ) {
    return this.adminActivityService.findAll(userId, query);
  }

  @Get(':id')
  findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminActivityService.findOne(userId, id);
  }

  @Patch(':id')
  update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdateActivityBodySchema,
  ) {
    return this.adminActivityService.update(userId, id, body);
  }

  @Patch(':id/review')
  review(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: ReviewActivityBodySchema,
  ) {
    return this.adminActivityService.review(userId, id, body);
  }

  @Delete(':id')
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminActivityService.remove(userId, id);
  }
}
