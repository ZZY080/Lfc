import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ConsumerChatService } from '@module/message/service/consumer-chat.service';
import {
  CreateConversationBodySchema,
  SendChatMessageBodySchema,
} from '@module/message/schema/chat.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';

@Controller('consumer/conversation')
@UseGuards(JwtAuthGuard)
export class ConsumerConversationController {
  constructor(private readonly consumerChatService: ConsumerChatService) {}

  @Get('unread-count')
  getUnreadCount(@CurrentUser('userId') userId: number) {
    return this.consumerChatService.getUnreadCount(userId);
  }

  @Get()
  findAll(
    @CurrentUser('userId') userId: number,
    @Query() query: PaginationQuerySchema,
  ) {
    return this.consumerChatService.findConversationsPaginated(
      userId,
      query.page,
      query.limit,
    );
  }

  @Post()
  create(
    @CurrentUser('userId') userId: number,
    @Body() body: CreateConversationBodySchema,
  ) {
    return this.consumerChatService.getOrCreateConversation(userId, body);
  }

  @Delete(':id')
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerChatService.removeConversation(userId, id);
  }

  @Get(':id/messages')
  findMessages(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.consumerChatService.findMessages(userId, id);
  }

  @Post(':id/messages')
  sendMessage(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: SendChatMessageBodySchema,
  ) {
    return this.consumerChatService.sendMessage(userId, id, body);
  }
}
