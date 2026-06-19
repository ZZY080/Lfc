import {
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ConsumerNotificationService } from '@module/message/service/consumer-notification.service';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';

@Controller('consumer/notification')
@UseGuards(JwtAuthGuard)
export class ConsumerNotificationController {
  constructor(
    private readonly consumerNotificationService: ConsumerNotificationService,
  ) {}

  @Get('unread-count')
  getUnreadCount(@CurrentUser('userId') userId: number) {
    return this.consumerNotificationService.getUnreadCount(userId);
  }

  @Get()
  findAll(
    @CurrentUser('userId') userId: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.consumerNotificationService.findPaginated(
      userId,
      query.page,
      query.limit,
    );
  }

  @Patch('read-all')
  markAllRead(@CurrentUser('userId') userId: number) {
    return this.consumerNotificationService.markAllRead(userId);
  }

  @Get(':id')
  findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerNotificationService.findOne(userId, id);
  }

  @Delete(':id')
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerNotificationService.remove(userId, id);
  }
}
