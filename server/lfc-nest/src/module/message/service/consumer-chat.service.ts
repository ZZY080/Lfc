import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConversationEntity } from '@module/message/entity/conversation.entity';
import { ChatMessageType, MessageEntity } from '@module/message/entity/message.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  CreateConversationBodyDto,
  SendChatMessageBodyDto,
} from '@module/message/dto/chat.dto';
import {
  formatChatProductPreview,
  parseChatProductPayload,
} from '@module/message/util/chat-product.util';
import {
  formatChatActivitySharePreview,
  formatChatPostSharePreview,
  parseChatActivitySharePayload,
  parseChatPostSharePayload,
} from '@module/message/util/chat-share.util';

@Injectable()
export class ConsumerChatService {
  constructor(
    @InjectRepository(ConversationEntity)
    private readonly conversationRepository: Repository<ConversationEntity>,
    @InjectRepository(MessageEntity)
    private readonly messageRepository: Repository<MessageEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async findConversations(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const conversations = await this.conversationRepository.find({
      where: [{ userOneId: userId }, { userTwoId: userId }],
      relations: ['userOne', 'userTwo'],
      order: { lastMessageAt: 'DESC', updatedAt: 'DESC' },
    });

    return conversations.map((conversation) => this.toConversationItem(conversation, userId));
  }

  async getUnreadCount(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const conversations = await this.conversationRepository.find({
      where: [{ userOneId: userId }, { userTwoId: userId }],
      select: ['id', 'userOneId', 'userTwoId', 'userOneUnreadCount', 'userTwoUnreadCount'],
    });

    const count = conversations.reduce(
      (sum, conversation) => sum + this.getUnreadCountForUser(conversation, userId),
      0,
    );
    return { count };
  }

  async getOrCreateConversation(userId: number, body: CreateConversationBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const peerUserId = body.peerUserId;

    if (peerUserId === userId) {
      throw new BadRequestException('不能与自己发起私信');
    }

    const peer = await this.userRepository.findOne({ where: { id: peerUserId } });
    if (!peer) {
      throw new NotFoundException('对方用户不存在');
    }

    const [userOneId, userTwoId] = this.normalizeParticipantIds(userId, peerUserId);
    let conversation = await this.conversationRepository.findOne({
      where: { userOneId, userTwoId },
      relations: ['userOne', 'userTwo'],
    });

    if (!conversation) {
      conversation = await this.conversationRepository.save(
        this.conversationRepository.create({
          userOneId,
          userTwoId,
          lastMessageContent: null,
          lastMessageAt: null,
          userOneUnreadCount: 0,
          userTwoUnreadCount: 0,
        }),
      );
      conversation = await this.conversationRepository.findOne({
        where: { id: conversation.id },
        relations: ['userOne', 'userTwo'],
      });
    }

    return this.toConversationItem(conversation!, userId);
  }

  async findMessages(userId: number, conversationId: number) {
    const conversation = await this.getConversationForUser(userId, conversationId);
    const messages = await this.messageRepository.find({
      where: { conversationId },
      relations: ['sender'],
      order: { createdAt: 'ASC' },
    });

    await this.messageRepository.update(
      { conversationId, isRead: false, senderId: this.getPeerUserId(conversation, userId) },
      { isRead: true },
    );

    if (conversation.userOneId === userId) {
      conversation.userOneUnreadCount = 0;
    } else {
      conversation.userTwoUnreadCount = 0;
    }
    await this.conversationRepository.save(conversation);

    return messages;
  }

  async sendMessage(
    userId: number,
    conversationId: number,
    body: SendChatMessageBodyDto,
  ) {
    const conversation = await this.getConversationForUser(userId, conversationId);
    const content = body.content.trim();
    const messageType = body.messageType ?? ChatMessageType.TEXT;

    if (messageType === ChatMessageType.PRODUCT) {
      const payload = parseChatProductPayload(content);
      if (!payload) {
        throw new BadRequestException('商品消息格式不正确');
      }
    }
    if (messageType === ChatMessageType.POST) {
      const payload = parseChatPostSharePayload(content);
      if (!payload) {
        throw new BadRequestException('笔记消息格式不正确');
      }
    }
    if (messageType === ChatMessageType.ACTIVITY) {
      const payload = parseChatActivitySharePayload(content);
      if (!payload) {
        throw new BadRequestException('活动消息格式不正确');
      }
    }

    const saved = await this.messageRepository.save(
      this.messageRepository.create({
        conversationId,
        senderId: userId,
        content,
        messageType,
        isRead: false,
      }),
    );

    conversation.lastMessageContent =
      messageType === ChatMessageType.IMAGE
        ? '[图片]'
        : messageType === ChatMessageType.VIDEO
          ? '[视频]'
          : messageType === ChatMessageType.PRODUCT
            ? formatChatProductPreview(parseChatProductPayload(content)!)
            : messageType === ChatMessageType.POST
              ? formatChatPostSharePreview(parseChatPostSharePayload(content)!)
              : messageType === ChatMessageType.ACTIVITY
                ? formatChatActivitySharePreview(parseChatActivitySharePayload(content)!)
                : content.slice(0, 500);
    conversation.lastMessageAt = saved.createdAt;

    const peerUserId = this.getPeerUserId(conversation, userId);
    if (conversation.userOneId === peerUserId) {
      conversation.userOneUnreadCount += 1;
    } else {
      conversation.userTwoUnreadCount += 1;
    }

    await this.conversationRepository.save(conversation);

    return this.messageRepository.findOne({
      where: { id: saved.id },
      relations: ['sender'],
    });
  }

  private async getConversationForUser(userId: number, conversationId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const conversation = await this.conversationRepository.findOne({
      where: { id: conversationId },
      relations: ['userOne', 'userTwo'],
    });
    if (!conversation) {
      throw new NotFoundException('会话不存在');
    }
    if (conversation.userOneId !== userId && conversation.userTwoId !== userId) {
      throw new ForbiddenException('无权访问该会话');
    }
    return conversation;
  }

  private normalizeParticipantIds(userA: number, userB: number): [number, number] {
    return userA < userB ? [userA, userB] : [userB, userA];
  }

  private getPeerUserId(conversation: ConversationEntity, userId: number) {
    return conversation.userOneId === userId
      ? conversation.userTwoId
      : conversation.userOneId;
  }

  private getUnreadCountForUser(conversation: ConversationEntity, userId: number) {
    return conversation.userOneId === userId
      ? conversation.userOneUnreadCount
      : conversation.userTwoUnreadCount;
  }

  private toConversationItem(conversation: ConversationEntity, userId: number) {
    const peer = conversation.userOneId === userId ? conversation.userTwo : conversation.userOne;
    return {
      id: conversation.id,
      peerUserId: peer?.id ?? this.getPeerUserId(conversation, userId),
      peerStudentId: peer?.studentId ?? `同学${this.getPeerUserId(conversation, userId)}`,
      peerNickname: peer?.nickname ?? null,
      peerAvatarUrl: peer?.avatarUrl ?? null,
      lastMessageContent: conversation.lastMessageContent,
      lastMessageAt: conversation.lastMessageAt,
      unreadCount: this.getUnreadCountForUser(conversation, userId),
      createdAt: conversation.createdAt,
      updatedAt: conversation.updatedAt,
    };
  }
}
