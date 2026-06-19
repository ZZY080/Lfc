import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';
import { AdminStatsController } from '@module/admin/controller/admin-stats.controller';
import { AdminStatsService } from '@module/admin/service/admin-stats.service';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      UserEntity,
      PostEntity,
      ActivityEntity,
      PaymentOrderEntity,
      PaymentAfterSalesEntity,
    ]),
  ],
  controllers: [AdminStatsController],
  providers: [AdminStatsService, RoleAuthzService],
})
export class AdminModule {}
