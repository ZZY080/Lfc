import {
  Body,
  Controller,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  UseGuards,
} from '@nestjs/common';
import { AdminActivityService } from '@module/activity/service/admin-activity.service';
import { ReviewActivityBodySchema } from '@module/activity/schema/activity.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/activity')
@UseGuards(JwtAuthGuard)
export class AdminActivityController {
  constructor(private readonly adminActivityService: AdminActivityService) {}

  @Get('pending')
  findPending(@CurrentUser('userId') userId: number) {
    return this.adminActivityService.findPending(userId);
  }

  @Get()
  findAll(@CurrentUser('userId') userId: number) {
    return this.adminActivityService.findAll(userId);
  }

  @Patch(':id/review')
  review(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: ReviewActivityBodySchema,
  ) {
    return this.adminActivityService.review(userId, id, body);
  }
}
