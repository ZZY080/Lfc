import {
  BadRequestException,
  ForbiddenException,
  Inject,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { MoreThan, Repository } from 'typeorm';
import { promotionConfiguration } from '@config/configuration';
import type { IPromotionConfig } from '@config/configuration';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityStatus } from '@shared/enum/user-role.enum';
import {
  PromotionActionResultDto,
  PromotionConfigDto,
  PromotionMetaDto,
  PromotionPaymentResultDto,
} from '@module/promotion/dto/promotion.dto';
import {
  addHours,
  buildActivityPromotionMeta,
  buildPostPromotionMeta,
  formatPromotionMoney,
  isPromotionActive,
  parsePromotionMoneyInput,
} from '@module/promotion/util/promotion.util';
import { PaymentOrderService } from '@module/payment/service/payment-order.service';
import { PaymentCheckoutService } from '@module/payment/service/payment-checkout.service';
import { PaymentChannel } from '@shared/enum/payment.enum';

interface PromotionChargePlan {
  amount: string;
  displacementTargetId: number | null;
}

@Injectable()
export class ConsumerPromotionService {
  constructor(
    @Inject(promotionConfiguration.KEY)
    private readonly promotionConfig: IPromotionConfig,
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    private readonly paymentOrderService: PaymentOrderService,
    private readonly paymentCheckoutService: PaymentCheckoutService,
  ) {}

  async getPublicConfig(): Promise<PromotionConfigDto> {
    const activePostCount = await this.countActiveBoostedPosts();
    const activeActivityCount = await this.countActivePromotedActivities();
    const postSlotsFull = activePostCount >= this.promotionConfig.postMaxFeedSlots;
    const activitySlotsFull =
      activeActivityCount >= this.promotionConfig.activityMaxFeedSlots;

    const lowestPostBoostBid = postSlotsFull
      ? (await this.findLowestActiveBoostedPost())?.boostBidAmount ?? null
      : null;
    const lowestActivityPromoteBid = activitySlotsFull
      ? (await this.findLowestActivePromotedActivity())?.promoteBidAmount ?? null
      : null;

    return {
      postBoostHours: this.promotionConfig.postBoostHours,
      postCooldownHours: this.promotionConfig.postCooldownHours,
      postActiveLabel: this.promotionConfig.postActiveLabel,
      postActionLabel: this.promotionConfig.postActionLabel,
      postMaxFeedSlots: this.promotionConfig.postMaxFeedSlots,
      activityPromoteHours: this.promotionConfig.activityPromoteHours,
      activityCooldownHours: this.promotionConfig.activityCooldownHours,
      activityMaxFeedSlots: this.promotionConfig.activityMaxFeedSlots,
      activityActiveLabel: this.promotionConfig.activityActiveLabel,
      activityActionLabel: this.promotionConfig.activityActionLabel,
      paidEnabled: this.promotionConfig.paidEnabled,
      postBoostPrice: this.promotionConfig.postBoostPrice,
      activityPromotePrice: this.promotionConfig.activityPromotePrice,
      bidIncrement: this.promotionConfig.bidIncrement,
      lowestPostBoostBid,
      lowestActivityPromoteBid,
      postSlotsFull,
      activitySlotsFull,
    };
  }

  attachPostPromotion(post: PostEntity, viewerId?: number) {
    const isOwner = viewerId != null && post.authorId === viewerId;
    const promotion = this.buildPostMeta(post, isOwner);
    return { ...post, promotion };
  }

  attachActivityPromotion<T extends ActivityEntity>(
    activity: T,
    viewerId?: number,
  ) {
    const isOwner = viewerId != null && activity.authorId === viewerId;
    const promotion = this.buildActivityMeta(activity, isOwner);
    return { ...activity, promotion };
  }

  attachPostsPromotion(posts: PostEntity[], viewerId?: number) {
    return posts.map((post) => this.attachPostPromotion(post, viewerId));
  }

  attachActivitiesPromotion<T extends ActivityEntity>(
    activities: T[],
    viewerId?: number,
  ) {
    return activities.map((activity) =>
      this.attachActivityPromotion(activity, viewerId),
    );
  }

  async boostPost(userId: number, postId: number): Promise<PromotionActionResultDto> {
    if (this.promotionConfig.paidEnabled) {
      throw new BadRequestException('当前为付费擦亮模式，请先完成支付');
    }

    const post = await this.loadOwnedPost(userId, postId);
    this.assertPostBoostAvailable(post);

    const plan = await this.resolvePostBoostCharge(post);
    await this.applyPostBoost(post, plan.amount, plan.displacementTargetId);

    const promotion = this.buildPostMeta(post, true);
    return {
      message: `擦亮成功，${this.promotionConfig.postBoostHours} 小时内优先展示`,
      promotion,
    };
  }

  async createPostBoostOrder(
    userId: number,
    postId: number,
    bidAmount?: string,
  ): Promise<PromotionPaymentResultDto> {
    this.paymentCheckoutService.assertAlipayConfigured();
    if (!this.promotionConfig.paidEnabled) {
      throw new BadRequestException('当前为免费擦亮模式，可直接擦亮');
    }

    const post = await this.loadOwnedPost(userId, postId);
    this.assertPostBoostAvailable(post);
    const plan = await this.resolvePostBoostCharge(post, bidAmount);

    const order = await this.paymentOrderService.acquirePostBoostOrder({
      userId,
      postId,
      payeeId: this.promotionConfig.platformPayeeId,
      amount: plan.amount,
      subject: `笔记擦亮-${post.title}`.slice(0, 120),
      channel: PaymentChannel.ALIPAY,
    });

    const promotion = this.buildPostMeta(post, true);
    return {
      message: `支付 ¥${plan.amount} 完成擦亮`,
      payment: this.paymentCheckoutService.buildPaymentResultForOrder(order),
      promotion,
    };
  }

  async promoteActivity(
    userId: number,
    activityId: number,
  ): Promise<PromotionActionResultDto> {
    if (this.promotionConfig.paidEnabled) {
      throw new BadRequestException('当前为付费推广模式，请先完成支付');
    }

    const activity = await this.loadOwnedActivity(userId, activityId);
    this.assertActivityPromoteAvailable(activity);

    const plan = await this.resolveActivityPromoteCharge(activity);
    await this.applyActivityPromote(
      activity,
      plan.amount,
      plan.displacementTargetId,
    );

    const promotion = this.buildActivityMeta(activity, true);
    return {
      message: `推广成功，${this.promotionConfig.activityPromoteHours} 小时内优先展示`,
      promotion,
    };
  }

  async createActivityPromoteOrder(
    userId: number,
    activityId: number,
    bidAmount?: string,
  ): Promise<PromotionPaymentResultDto> {
    this.paymentCheckoutService.assertAlipayConfigured();
    if (!this.promotionConfig.paidEnabled) {
      throw new BadRequestException('当前为免费推广模式，可直接推广');
    }

    const activity = await this.loadOwnedActivity(userId, activityId);
    this.assertActivityPromoteAvailable(activity);
    const plan = await this.resolveActivityPromoteCharge(activity, bidAmount);

    const order = await this.paymentOrderService.acquireActivityPromoteOrder({
      userId,
      activityId,
      payeeId: this.promotionConfig.platformPayeeId,
      amount: plan.amount,
      subject: `活动推广-${activity.title}`.slice(0, 120),
      channel: PaymentChannel.ALIPAY,
    });

    const promotion = this.buildActivityMeta(activity, true);
    return {
      message: `支付 ¥${plan.amount} 完成推广`,
      payment: this.paymentCheckoutService.buildPaymentResultForOrder(order),
      promotion,
    };
  }

  async applyPostBoostAfterPayment(
    userId: number,
    postId: number,
    amount: string,
  ) {
    const post = await this.loadOwnedPost(userId, postId);
    const plan = await this.resolvePostBoostCharge(post, amount);
    if (plan.amount !== formatPromotionMoney(Number.parseFloat(amount))) {
      throw new BadRequestException('支付金额与擦亮方案不匹配');
    }
    await this.applyPostBoost(post, plan.amount, plan.displacementTargetId);
  }

  async applyActivityPromoteAfterPayment(
    userId: number,
    activityId: number,
    amount: string,
  ) {
    const activity = await this.loadOwnedActivity(userId, activityId);
    const plan = await this.resolveActivityPromoteCharge(activity, amount);
    if (plan.amount !== formatPromotionMoney(Number.parseFloat(amount))) {
      throw new BadRequestException('支付金额与推广方案不匹配');
    }
    await this.applyActivityPromote(
      activity,
      plan.amount,
      plan.displacementTargetId,
    );
  }

  async isPostBoostFulfilled(
    userId: number,
    postId: number,
    paidAt: Date,
  ): Promise<boolean> {
    const post = await this.postRepository.findOne({ where: { id: postId } });
    if (!post || post.authorId !== userId) {
      return false;
    }
    if (!isPromotionActive(post.boostedUntil) || !post.lastBoostedAt) {
      return false;
    }
    return post.lastBoostedAt.getTime() >= paidAt.getTime() - 1000;
  }

  async isActivityPromoteFulfilled(
    userId: number,
    activityId: number,
    paidAt: Date,
  ): Promise<boolean> {
    const activity = await this.activityRepository.findOne({
      where: { id: activityId },
    });
    if (!activity || activity.authorId !== userId) {
      return false;
    }
    if (!isPromotionActive(activity.promotedUntil) || !activity.lastPromotedAt) {
      return false;
    }
    return activity.lastPromotedAt.getTime() >= paidAt.getTime() - 1000;
  }

  async findActivePromotedActivities(limit: number) {
    const now = new Date();
    return this.activityRepository.find({
      where: {
        status: ActivityStatus.APPROVED,
        promotedUntil: MoreThan(now),
      },
      relations: ['author', 'participants', 'participants.user'],
      order: {
        promoteBidAmount: 'DESC',
        lastPromotedAt: 'DESC',
      },
      take: limit,
    });
  }

  async countActivePromotedActivities(): Promise<number> {
    const now = new Date();
    return this.activityRepository.count({
      where: {
        status: ActivityStatus.APPROVED,
        promotedUntil: MoreThan(now),
      },
    });
  }

  async findActiveBoostedPosts(limit: number) {
    const now = new Date();
    return this.postRepository.find({
      where: {
        boostedUntil: MoreThan(now),
      },
      relations: ['author'],
      order: {
        boostBidAmount: 'DESC',
        lastBoostedAt: 'DESC',
      },
      take: limit,
    });
  }

  async countActiveBoostedPosts(): Promise<number> {
    const now = new Date();
    return this.postRepository.count({
      where: {
        boostedUntil: MoreThan(now),
      },
    });
  }

  getFirstPageBoostedSlotCount(activeCount: number): number {
    return Math.min(activeCount, this.promotionConfig.postMaxFeedSlots);
  }

  getFirstPagePromotedSlotCount(activeCount: number): number {
    return Math.min(activeCount, this.promotionConfig.activityMaxFeedSlots);
  }

  private async loadOwnedPost(userId: number, postId: number) {
    const post = await this.postRepository.findOne({ where: { id: postId } });
    if (!post) {
      throw new NotFoundException('笔记不存在');
    }
    if (post.authorId !== userId) {
      throw new ForbiddenException('只能擦亮自己的笔记');
    }
    return post;
  }

  private async loadOwnedActivity(userId: number, activityId: number) {
    const activity = await this.activityRepository.findOne({
      where: { id: activityId },
    });
    if (!activity) {
      throw new NotFoundException('活动不存在');
    }
    if (activity.authorId !== userId) {
      throw new ForbiddenException('只能推广自己发布的活动');
    }
    if (activity.status !== ActivityStatus.APPROVED) {
      throw new BadRequestException('仅已审核通过的活动可推广');
    }
    return activity;
  }

  private assertPostBoostAvailable(post: PostEntity) {
    const meta = this.buildPostMeta(post, true);
    if (!meta.canApply) {
      throw new BadRequestException(
        meta.nextAvailableAt
          ? `擦亮冷却中，请在 ${this.formatDisplayTime(meta.nextAvailableAt)} 后再试`
          : '暂不可擦亮',
      );
    }
  }

  private assertActivityPromoteAvailable(activity: ActivityEntity) {
    const meta = this.buildActivityMeta(activity, true);
    if (!meta.canApply) {
      throw new BadRequestException(
        meta.nextAvailableAt
          ? `推广冷却中，请在 ${this.formatDisplayTime(meta.nextAvailableAt)} 后再试`
          : '暂不可推广',
      );
    }
  }

  private async resolvePostBoostCharge(
    post: PostEntity,
    bidAmount?: string,
  ): Promise<PromotionChargePlan> {
    const basePrice = Number.parseFloat(this.promotionConfig.postBoostPrice);
    let amount = basePrice;
    if (bidAmount != null && bidAmount !== '') {
      try {
        amount = parsePromotionMoneyInput(bidAmount, '出价');
      } catch (error) {
        throw new BadRequestException(
          error instanceof Error ? error.message : '出价格式不正确',
        );
      }
    }
    if (amount < basePrice) {
      throw new BadRequestException(`擦亮最低出价 ¥${this.promotionConfig.postBoostPrice}`);
    }

    const alreadyActive = isPromotionActive(post.boostedUntil);
    let displacementTargetId: number | null = null;

    if (!alreadyActive) {
      const activeCount = await this.countActiveBoostedPosts();
      if (activeCount >= this.promotionConfig.postMaxFeedSlots) {
        const lowest = await this.findLowestActiveBoostedPost();
        if (!lowest) {
          throw new BadRequestException('推广位已满，请稍后再试');
        }
        const minRequired =
          Number.parseFloat(lowest.boostBidAmount || '0') +
          Number.parseFloat(this.promotionConfig.bidIncrement);
        const hasExplicitBid = bidAmount != null && bidAmount !== '';
        if (!hasExplicitBid && amount + 1e-8 < minRequired) {
          amount = minRequired;
        } else if (amount + 1e-8 < minRequired) {
          throw new BadRequestException(
            `推广位已满，需出价 ¥${formatPromotionMoney(minRequired)} 以上抢位`,
          );
        }
        displacementTargetId = lowest.id;
      }
    }

    return {
      amount: formatPromotionMoney(amount),
      displacementTargetId,
    };
  }

  private async resolveActivityPromoteCharge(
    activity: ActivityEntity,
    bidAmount?: string,
  ): Promise<PromotionChargePlan> {
    const basePrice = Number.parseFloat(this.promotionConfig.activityPromotePrice);
    let amount = basePrice;
    if (bidAmount != null && bidAmount !== '') {
      try {
        amount = parsePromotionMoneyInput(bidAmount, '出价');
      } catch (error) {
        throw new BadRequestException(
          error instanceof Error ? error.message : '出价格式不正确',
        );
      }
    }
    if (amount < basePrice) {
      throw new BadRequestException(
        `推广最低出价 ¥${this.promotionConfig.activityPromotePrice}`,
      );
    }

    const alreadyActive = isPromotionActive(activity.promotedUntil);
    let displacementTargetId: number | null = null;

    if (!alreadyActive) {
      const activeCount = await this.countActivePromotedActivities();
      if (activeCount >= this.promotionConfig.activityMaxFeedSlots) {
        const lowest = await this.findLowestActivePromotedActivity();
        if (!lowest) {
          throw new BadRequestException('推广位已满，请稍后再试');
        }
        const minRequired =
          Number.parseFloat(lowest.promoteBidAmount || '0') +
          Number.parseFloat(this.promotionConfig.bidIncrement);
        const hasExplicitBid = bidAmount != null && bidAmount !== '';
        if (!hasExplicitBid && amount + 1e-8 < minRequired) {
          amount = minRequired;
        } else if (amount + 1e-8 < minRequired) {
          throw new BadRequestException(
            `推广位已满，需出价 ¥${formatPromotionMoney(minRequired)} 以上抢位`,
          );
        }
        displacementTargetId = lowest.id;
      }
    }

    return {
      amount: formatPromotionMoney(amount),
      displacementTargetId,
    };
  }

  private async applyPostBoost(
    post: PostEntity,
    amount: string,
    displacementTargetId: number | null,
  ) {
    if (displacementTargetId != null && displacementTargetId !== post.id) {
      await this.endPostBoost(displacementTargetId);
    }

    const now = new Date();
    post.lastBoostedAt = now;
    post.boostedUntil = addHours(now, this.promotionConfig.postBoostHours);
    post.boostBidAmount = amount;
    await this.postRepository.save(post);
  }

  private async applyActivityPromote(
    activity: ActivityEntity,
    amount: string,
    displacementTargetId: number | null,
  ) {
    if (displacementTargetId != null && displacementTargetId !== activity.id) {
      await this.endActivityPromote(displacementTargetId);
    }

    const now = new Date();
    activity.lastPromotedAt = now;
    activity.promotedUntil = addHours(
      now,
      this.promotionConfig.activityPromoteHours,
    );
    activity.promoteBidAmount = amount;
    await this.activityRepository.save(activity);
  }

  private async findLowestActiveBoostedPost() {
    const now = new Date();
    return this.postRepository.findOne({
      where: { boostedUntil: MoreThan(now) },
      order: {
        boostBidAmount: 'ASC',
        lastBoostedAt: 'ASC',
      },
    });
  }

  private async findLowestActivePromotedActivity() {
    const now = new Date();
    return this.activityRepository.findOne({
      where: {
        status: ActivityStatus.APPROVED,
        promotedUntil: MoreThan(now),
      },
      order: {
        promoteBidAmount: 'ASC',
        lastPromotedAt: 'ASC',
      },
    });
  }

  private async endPostBoost(postId: number) {
    const post = await this.postRepository.findOne({ where: { id: postId } });
    if (!post) {
      return;
    }
    post.boostedUntil = new Date();
    post.boostBidAmount = '0.00';
    await this.postRepository.save(post);
  }

  private async endActivityPromote(activityId: number) {
    const activity = await this.activityRepository.findOne({
      where: { id: activityId },
    });
    if (!activity) {
      return;
    }
    activity.promotedUntil = new Date();
    activity.promoteBidAmount = '0.00';
    await this.activityRepository.save(activity);
  }

  private buildPostMeta(post: PostEntity, isOwner: boolean): PromotionMetaDto {
    return buildPostPromotionMeta({
      boostedUntil: post.boostedUntil,
      lastBoostedAt: post.lastBoostedAt,
      boostBidAmount: post.boostBidAmount,
      isOwner,
      boostHours: this.promotionConfig.postBoostHours,
      cooldownHours: this.promotionConfig.postCooldownHours,
      activeLabel: this.promotionConfig.postActiveLabel,
      actionLabel: this.promotionConfig.postActionLabel,
      paidEnabled: this.promotionConfig.paidEnabled,
      basePrice: this.promotionConfig.postBoostPrice,
      minBidAmount: this.buildMinPostBidAmount(),
    });
  }

  private buildActivityMeta(
    activity: ActivityEntity,
    isOwner: boolean,
  ): PromotionMetaDto {
    return buildActivityPromotionMeta({
      promotedUntil: activity.promotedUntil,
      lastPromotedAt: activity.lastPromotedAt,
      promoteBidAmount: activity.promoteBidAmount,
      isOwner,
      promoteHours: this.promotionConfig.activityPromoteHours,
      cooldownHours: this.promotionConfig.activityCooldownHours,
      activeLabel: this.promotionConfig.activityActiveLabel,
      actionLabel: this.promotionConfig.activityActionLabel,
      paidEnabled: this.promotionConfig.paidEnabled,
      basePrice: this.promotionConfig.activityPromotePrice,
      minBidAmount: this.buildMinActivityBidAmount(),
    });
  }

  private buildMinPostBidAmount(): string | null {
    if (!this.promotionConfig.paidEnabled) {
      return null;
    }
    return this.promotionConfig.postBoostPrice;
  }

  private buildMinActivityBidAmount(): string | null {
    if (!this.promotionConfig.paidEnabled) {
      return null;
    }
    return this.promotionConfig.activityPromotePrice;
  }

  private formatDisplayTime(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return iso;
    }
    const pad = (value: number) => String(value).padStart(2, '0');
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}
