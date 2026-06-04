import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserAlipayService } from '@module/user/service/user-alipay.service';
import { AlipayModule } from '@integration/alipay/alipay.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([UserEntity]),
    forwardRef(() => AlipayModule),
  ],
  providers: [UserAlipayService],
  exports: [UserAlipayService],
})
export class UserAlipayModule {}
