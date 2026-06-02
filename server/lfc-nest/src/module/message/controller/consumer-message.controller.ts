import {
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  UseGuards,
} from '@nestjs/common';
import { ConsumerMessageService } from '@module/message/service/consumer-message.service';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/message')
@UseGuards(JwtAuthGuard)
export class ConsumerMessageController {
  constructor(private readonly consumerMessageService: ConsumerMessageService) {}

  @Get('unread-count')
  getUnreadCount(@CurrentUser('userId') userId: number) {
    return this.consumerMessageService.getUnreadCount(userId);
  }

  @Get()
  findAll(@CurrentUser('userId') userId: number) {
    return this.consumerMessageService.findAll(userId);
  }

  @Patch('read-all')
  markAllRead(@CurrentUser('userId') userId: number) {
    return this.consumerMessageService.markAllRead(userId);
  }

  @Get(':id')
  findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerMessageService.findOne(userId, id);
  }

  @Patch(':id/read')
  markRead(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerMessageService.markRead(userId, id);
  }

  @Delete(':id')
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerMessageService.remove(userId, id);
  }
}
