import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtAuthStrategy } from '@shared/strategy/jwt-auth.strategy';
import { UserEntity } from '@module/user/entity/user.entity';
import { ConsumerAuthController } from '@module/auth/controller/consumer-auth.controller';
import { AdminAuthController } from '@module/auth/controller/admin-auth.controller';
import { ConsumerAuthService } from '@module/auth/service/consumer-auth.service';
import { AdminAuthService } from '@module/auth/service/admin-auth.service';
import { MessageModule } from '@module/message/message.module';

@Module({
  imports: [TypeOrmModule.forFeature([UserEntity]), MessageModule],
  controllers: [ConsumerAuthController, AdminAuthController],
  providers: [ConsumerAuthService, AdminAuthService, JwtAuthStrategy],
})
export class AuthModule {}
