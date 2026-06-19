import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Inject,
  Injectable,
  NotFoundException,
  forwardRef,
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
import { NotificationService } from '@module/message/service/notification.service';
import { UserAlipayService } from '@module/user/service/user-alipay.service';
import { UserEntity } from '@module/user/entity/user.entity';
import { ConsumerActivitySocialService } from '@module/activity/service/consumer-activity-social.service';
import { ConsumerPromotionService } from '@module/promotion/service/consumer-promotion.service';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class ConsumerActivityService {
  private static readonly MAX_IMAGES = 20;

  constructor(
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    @InjectRepository(ActivityParticipantEntity)
    private readonly participantRepository: Repository<ActivityParticipantEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly notificationService: NotificationService,
    private readonly userAlipayService: UserAlipayService,
    private readonly consumerActivitySocialService: ConsumerActivitySocialService,
    @Inject(forwardRef(() => ConsumerPromotionService))
    private readonly consumerPromotionService: ConsumerPromotionService,
  ) {}

  async create(userId: number, body: CreateActivityBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    this.validateTimeRange(body.startTime, body.endTime);
    const normalized = this.normalizeActivityBody(body);
    const fee = this.normalizeFee(body.fee);
    if (Number.parseFloat(fee) > 0) {
      await this.userAlipayService.assertCanReceive(userId, '活动发起人');
    }

    const activity = this.activityRepository.create({
      ...normalized,
      location: body.location,
      latitude: body.latitude ?? null,
      longitude: body.longitude ?? null,
      startTime: new Date(body.startTime),
      endTime: new Date(body.endTime),
      maxParticipants: body.maxParticipants ?? 0,
      fee,
      authorId: userId,
      status: ActivityStatus.PENDING,
    });
    const saved = await this.activityRepository.save(activity);
    await this.notificationService.sendActivitySubmitted(userId, saved);
    return saved;
  }

  async findApproved() {
    const activities = await this.activityRepository.find({
      where: { status: ActivityStatus.APPROVED },
      relations: ['author', 'participants', 'participants.user'],
      order: { createdAt: 'DESC' },
    });
    const enriched = await this.consumerActivitySocialService.enrichActivities(activities);
    return this.consumerPromotionService.attachActivitiesPromotion(enriched);
  }

  async findApprovedPaginated(page?: number, limit?: number, userId?: number) {
    const { page: normalizedPage, limit: normalizedLimit } = normalizePagination(
      page,
      limit,
    );
    const maxSlots = (await this.consumerPromotionService.getPublicConfig())
      .activityMaxFeedSlots;
    const activePromotedCount =
      await this.consumerPromotionService.countActivePromotedActivities();
    const firstPagePromotedCount =
      this.consumerPromotionService.getFirstPagePromotedSlotCount(
        activePromotedCount,
      );

    let promotedActivities: ActivityEntity[] = [];
    if (normalizedPage === 1 && firstPagePromotedCount > 0) {
      promotedActivities =
        await this.consumerPromotionService.findActivePromotedActivities(
          maxSlots,
        );
    }
    const promotedIds = promotedActivities.map((item) => item.id);

    const regularSkip =
      normalizedPage <= 1
        ? 0
        : (normalizedPage - 1) * normalizedLimit - firstPagePromotedCount;
    const regularTake =
      normalizedPage === 1
        ? Math.max(normalizedLimit - promotedActivities.length, 0)
        : normalizedLimit;

    const regularQb = this.activityRepository
      .createQueryBuilder('activity')
      .leftJoinAndSelect('activity.author', 'author')
      .leftJoinAndSelect('activity.participants', 'participants')
      .leftJoinAndSelect('participants.user', 'participantUser')
      .where('activity.status = :status', { status: ActivityStatus.APPROVED });

    if (promotedIds.length > 0) {
      regularQb.andWhere('activity.id NOT IN (:...promotedIds)', { promotedIds });
    }

    const [regularActivities, totalRegular] = await regularQb
      .orderBy('activity.createdAt', 'DESC')
      .skip(Math.max(regularSkip, 0))
      .take(regularTake)
      .getManyAndCount();

    const total = totalRegular + activePromotedCount;
    const pageActivities =
      normalizedPage === 1
        ? [...promotedActivities, ...regularActivities]
        : regularActivities;

    const items = await this.consumerActivitySocialService.enrichActivities(
      pageActivities.map((activity) => ({
        ...activity,
        isJoined: userId
          ? activity.participants.some((item) => item.userId === userId)
          : false,
      })),
      userId,
    );

    return createPaginatedResult(
      this.consumerPromotionService.attachActivitiesPromotion(items, userId),
      total,
      normalizedPage,
      normalizedLimit,
    );
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

  findApprovedByAuthor(authorId: number) {
    return this.activityRepository.find({
      where: { authorId, status: ActivityStatus.APPROVED },
      relations: ['author'],
      order: { startTime: 'DESC' },
    });
  }

  async findByAuthorPaginated(
    authorId: number,
    isSelf: boolean,
    page?: number,
    limit?: number,
    viewerId?: number,
  ) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const [activities, total] = await this.activityRepository.findAndCount({
      where: isSelf
        ? { authorId }
        : { authorId, status: ActivityStatus.APPROVED },
      relations: isSelf ? ['participants', 'author'] : ['author', 'participants'],
      order: { createdAt: 'DESC' },
      skip,
      take: normalizedLimit,
    });
    const items = await this.consumerActivitySocialService.enrichActivities(
      activities.map((activity) => ({
        ...activity,
        isJoined: viewerId
          ? activity.participants.some((item) => item.userId === viewerId)
          : false,
      })),
      viewerId,
    );
    return createPaginatedResult(
      items,
      total,
      normalizedPage,
      normalizedLimit,
    );
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

  async findOneForViewer(id: number, userId?: number) {
    const activity = await this.findOne(id);
    const isAuthor = userId != null && activity.authorId === userId;
    if (activity.status === ActivityStatus.OFF_SHELF && !isAuthor) {
      throw new NotFoundException('活动不存在');
    }
    const isJoined = userId
      ? activity.participants.some((item) => item.userId === userId)
      : false;
    const [enriched] = await this.consumerActivitySocialService.enrichActivities(
      [{ ...activity, isJoined }],
      userId,
    );
    return this.consumerPromotionService.attachActivityPromotion(enriched, userId);
  }

  isUserJoined(activity: ActivityEntity, userId: number): boolean {
    return activity.participants.some((item) => item.userId === userId);
  }

  async update(userId: number, id: number, body: UpdateActivityBodyDto) {
    const activity = await this.findOne(id);
    if (activity.authorId !== userId) {
      throw new ForbiddenException('无权修改该活动');
    }

    if (body.startTime || body.endTime) {
      this.validateTimeRange(
        body.startTime ?? activity.startTime.toISOString(),
        body.endTime ?? activity.endTime.toISOString(),
      );
    }

    const normalized = this.normalizeActivityBody(
      {
        title: body.title ?? activity.title,
        description: body.description ?? activity.description,
        images: body.images ?? activity.images ?? [],
      },
      activity,
    );

    const locationChanged =
      body.location !== undefined && body.location !== activity.location;
    Object.assign(activity, {
      ...normalized,
      location: body.location ?? activity.location,
      latitude:
        body.latitude !== undefined
          ? body.latitude
          : locationChanged
            ? null
            : activity.latitude,
      longitude:
        body.longitude !== undefined
          ? body.longitude
          : locationChanged
            ? null
            : activity.longitude,
      startTime: body.startTime ? new Date(body.startTime) : activity.startTime,
      endTime: body.endTime ? new Date(body.endTime) : activity.endTime,
      maxParticipants: body.maxParticipants ?? activity.maxParticipants,
      fee: body.fee !== undefined ? this.normalizeFee(body.fee) : activity.fee,
      status: ActivityStatus.PENDING,
      reviewComment: null,
    });
    const saved = await this.activityRepository.save(activity);
    await this.notificationService.sendActivitySubmitted(userId, saved);
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

  async offShelf(userId: number, id: number) {
    const activity = await this.findOne(id);
    if (activity.authorId !== userId) {
      throw new ForbiddenException('无权操作该活动');
    }
    if (activity.status !== ActivityStatus.APPROVED) {
      throw new BadRequestException('仅已审核通过的活动可以下架');
    }
    activity.status = ActivityStatus.OFF_SHELF;
    activity.promotedUntil = null;
    await this.activityRepository.save(activity);
    return this.findOneForViewer(id, userId);
  }

  async onShelf(userId: number, id: number) {
    const activity = await this.findOne(id);
    if (activity.authorId !== userId) {
      throw new ForbiddenException('无权操作该活动');
    }
    if (activity.status !== ActivityStatus.OFF_SHELF) {
      throw new BadRequestException('仅已下架的活动可以重新上架');
    }
    activity.status = ActivityStatus.APPROVED;
    await this.activityRepository.save(activity);
    return this.findOneForViewer(id, userId);
  }

  async join(userId: number, activityId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const activity = await this.findOne(activityId);
    if (this.getActivityFee(activity) > 0) {
      throw new BadRequestException('该活动为付费活动，请先完成支付后再报名');
    }
    return this.createParticipation(userId, activity);
  }

  async joinAfterPayment(userId: number, activityId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const activity = await this.findOne(activityId);
    return this.createParticipation(userId, activity);
  }

  async assertCanJoin(userId: number, activityId: number) {
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
  }

  getActivityFee(activity: ActivityEntity): number {
    return Number.parseFloat(String(activity.fee ?? 0));
  }

  private async createParticipation(userId: number, activity: ActivityEntity) {
    if (activity.status !== ActivityStatus.APPROVED) {
      throw new BadRequestException('仅已审核通过的活动可以报名');
    }

    const existing = await this.participantRepository.findOne({
      where: { activityId: activity.id, userId },
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
      this.participantRepository.create({ activityId: activity.id, userId }),
    );

    const joiner = await this.userRepository.findOne({ where: { id: userId } });
    if (joiner && activity.authorId !== userId) {
      await this.notificationService.sendActivityJoin(
        activity.authorId,
        activity,
        joiner.studentId,
      );
    }

    return participant;
  }

  private normalizeFee(fee?: number): string {
    const value = fee ?? 0;
    if (value < 0) {
      throw new BadRequestException('活动费用不能小于0');
    }
    return value.toFixed(2);
  }

  async isJoined(userId: number, activityId: number): Promise<boolean> {
    const participant = await this.participantRepository.findOne({
      where: { activityId, userId },
    });
    return Boolean(participant);
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

  private normalizeActivityBody(
    body: Pick<CreateActivityBodyDto, 'title' | 'description' | 'images'>,
    existing?: ActivityEntity,
  ) {
    const description = body.description?.trim() ?? existing?.description ?? '';
    const images = (body.images ?? existing?.images ?? []).filter(Boolean);
    if (images.length > ConsumerActivityService.MAX_IMAGES) {
      throw new BadRequestException(
        `最多上传${ConsumerActivityService.MAX_IMAGES}张图片`,
      );
    }
    if (!description && images.length === 0) {
      throw new BadRequestException('请填写活动介绍或上传至少一张图片');
    }
    const title =
      body.title?.trim() ||
      description.slice(0, 30) ||
      (images.length > 0 ? '图片活动' : '校园活动');
    return { title, description, images };
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
