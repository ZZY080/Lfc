import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, MoreThan, Repository } from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { ActivityStatus, UserRole } from '@shared/enum/user-role.enum';
import { PaymentOrderStatus, PaymentAfterSalesStatus } from '@shared/enum/payment.enum';
import { PostStatus } from '@shared/enum/post-status.enum';
import { AdminStatsOverviewDto } from '@module/admin/dto/admin-stats.dto';

@Injectable()
export class AdminStatsService {
  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    @InjectRepository(PaymentAfterSalesEntity)
    private readonly afterSalesRepository: Repository<PaymentAfterSalesEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async getOverview(adminUserId: number): Promise<AdminStatsOverviewDto> {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);

    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

    const paidStatuses = [
      PaymentOrderStatus.PAID,
      PaymentOrderStatus.CONFIRMED,
      PaymentOrderStatus.SETTLED,
    ];

    const [
      totalUsers,
      consumerCount,
      adminCount,
      recentUsers,
      totalPosts,
      pendingPosts,
      approvedPosts,
      rejectedPosts,
      offShelfPosts,
      visiblePosts,
      hiddenPosts,
      totalActivities,
      pendingActivities,
      approvedActivities,
      rejectedActivities,
      offShelfActivities,
      totalPayments,
      paidPayments,
      paidAmountRow,
      pendingAfterSales,
    ] = await Promise.all([
      this.userRepository.count(),
      this.userRepository.count({ where: { role: UserRole.CONSUMER } }),
      this.userRepository.count({ where: { role: UserRole.ADMIN } }),
      this.userRepository.count({
        where: { createdAt: MoreThan(sevenDaysAgo) },
      }),
      this.postRepository.count(),
      this.postRepository.count({ where: { status: PostStatus.PENDING } }),
      this.postRepository.count({ where: { status: PostStatus.APPROVED } }),
      this.postRepository.count({ where: { status: PostStatus.REJECTED } }),
      this.postRepository.count({ where: { status: PostStatus.OFF_SHELF } }),
      this.postRepository.count({ where: { isVisible: true } }),
      this.postRepository.count({ where: { isVisible: false } }),
      this.activityRepository.count(),
      this.activityRepository.count({
        where: { status: ActivityStatus.PENDING },
      }),
      this.activityRepository.count({
        where: { status: ActivityStatus.APPROVED },
      }),
      this.activityRepository.count({
        where: { status: ActivityStatus.REJECTED },
      }),
      this.activityRepository.count({
        where: { status: ActivityStatus.OFF_SHELF },
      }),
      this.paymentOrderRepository.count(),
      this.paymentOrderRepository.count({
        where: { status: In(paidStatuses) },
      }),
      this.paymentOrderRepository
        .createQueryBuilder('order')
        .select('COALESCE(SUM(order.amount), 0)', 'total')
        .where('order.status IN (:...statuses)', { statuses: paidStatuses })
        .getRawOne<{ total: string }>(),
      this.afterSalesRepository.count({
        where: { status: PaymentAfterSalesStatus.PENDING },
      }),
    ]);

    return {
      users: {
        total: totalUsers,
        consumers: consumerCount,
        admins: adminCount,
        recent7Days: recentUsers,
      },
      posts: {
        total: totalPosts,
        pending: pendingPosts,
        approved: approvedPosts,
        rejected: rejectedPosts,
        offShelf: offShelfPosts,
        visible: visiblePosts,
        hidden: hiddenPosts,
      },
      activities: {
        total: totalActivities,
        pending: pendingActivities,
        approved: approvedActivities,
        rejected: rejectedActivities,
        offShelf: offShelfActivities,
      },
      payments: {
        total: totalPayments,
        paid: paidPayments,
        totalPaidAmount: paidAmountRow?.total ?? '0',
        pendingAfterSales,
      },
    };
  }
}
