import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { ActivityStatus, UserRole } from '@shared/enum/user-role.enum';
import {
  CreateActivityBodyDto,
  UpdateActivityBodyDto,
} from '@module/activity/dto/activity.dto';
import { MessageService } from '@module/message/service/message.service';
import { UserEntity } from '@module/user/entity/user.entity';

@Injectable()
export class ConsumerActivityService {
  constructor(
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    @InjectRepository(ActivityParticipantEntity)
    private readonly participantRepository: Repository<ActivityParticipantEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly messageService: MessageService,
  ) {}

  async create(userId: number, body: CreateActivityBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    this.validateTimeRange(body.startTime, body.endTime);

    const activity = this.activityRepository.create({
      ...body,
      startTime: new Date(body.startTime),
      endTime: new Date(body.endTime),
      maxParticipants: body.maxParticipants ?? 0,
      authorId: userId,
      status: ActivityStatus.PENDING,
    });
    const saved = await this.activityRepository.save(activity);
    await this.messageService.sendActivitySubmitted(userId, saved);
    return saved;
  }

  findApproved() {
    return this.activityRepository.find({
      where: { status: ActivityStatus.APPROVED },
      relations: ['author', 'participants', 'participants.user'],
      order: { startTime: 'ASC' },
    });
  }

  findMine(userId: number) {
    return this.activityRepository.find({
      where: { authorId: userId },
      relations: ['participants'],
      order: { createdAt: 'DESC' },
    });
  }

  findMyParticipations(userId: number) {
    return this.participantRepository.find({
      where: { userId },
      relations: ['activity', 'activity.author'],
      order: { joinedAt: 'DESC' },
    });
  }

  async findOne(id: number) {
    const activity = await this.activityRepository.findOne({
      where: { id },
      relations: ['author', 'participants', 'participants.user'],
    });
    if (!activity) {
      throw new NotFoundException('活动不存在');
    }
    return activity;
  }

  async update(userId: number, id: number, body: UpdateActivityBodyDto) {
    const activity = await this.findOne(id);
    if (activity.authorId !== userId) {
      throw new ForbiddenException('无权修改该活动');
    }
    if (activity.status === ActivityStatus.APPROVED) {
      throw new BadRequestException('已审核通过的活动不可修改');
    }

    if (body.startTime || body.endTime) {
      this.validateTimeRange(
        body.startTime ?? activity.startTime.toISOString(),
        body.endTime ?? activity.endTime.toISOString(),
      );
    }

    Object.assign(activity, {
      ...body,
      startTime: body.startTime ? new Date(body.startTime) : activity.startTime,
      endTime: body.endTime ? new Date(body.endTime) : activity.endTime,
      status: ActivityStatus.PENDING,
    });
    const saved = await this.activityRepository.save(activity);
    await this.messageService.sendActivitySubmitted(userId, saved);
    return saved;
  }

  async remove(userId: number, id: number) {
    const activity = await this.findOne(id);
    if (activity.authorId !== userId) {
      throw new ForbiddenException('无权删除该活动');
    }
    await this.activityRepository.remove(activity);
    return { message: '删除成功' };
  }

  async join(userId: number, activityId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const activity = await this.findOne(activityId);
    if (activity.status !== ActivityStatus.APPROVED) {
      throw new BadRequestException('仅已审核通过的活动可以报名');
    }

    const existing = await this.participantRepository.findOne({
      where: { activityId, userId },
    });
    if (existing) {
      throw new ConflictException('您已报名该活动');
    }

    if (
      activity.maxParticipants > 0 &&
      activity.participants.length >= activity.maxParticipants
    ) {
      throw new BadRequestException('活动名额已满');
    }

    const participant = await this.participantRepository.save(
      this.participantRepository.create({ activityId, userId }),
    );

    const joiner = await this.userRepository.findOne({ where: { id: userId } });
    if (joiner && activity.authorId !== userId) {
      await this.messageService.sendActivityJoin(
        activity.authorId,
        activity,
        joiner.studentId,
      );
    }

    return participant;
  }

  async leave(userId: number, activityId: number) {
    const participant = await this.participantRepository.findOne({
      where: { activityId, userId },
    });
    if (!participant) {
      throw new NotFoundException('未找到报名记录');
    }
    await this.participantRepository.remove(participant);
    return { message: '已取消报名' };
  }

  private validateTimeRange(startTime: string, endTime: string) {
    const start = new Date(startTime);
    const end = new Date(endTime);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      throw new BadRequestException('活动时间格式不正确');
    }
    if (end <= start) {
      throw new BadRequestException('结束时间必须晚于开始时间');
    }
  }
}
