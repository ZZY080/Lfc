import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserRole, UserStatus } from '@shared/enum/user-role.enum';

@Injectable()
export class JwtAuthStrategy extends PassportStrategy(Strategy) {
  constructor(
    configService: ConfigService,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: configService.get<string>('JWT_SECRET', 'change-me'),
    });
  }

  async validate(payload: { userId: number; role: string }) {
    const user = await this.userRepository.findOne({
      where: { id: payload.userId },
      select: ['id', 'role', 'status'],
    });
    if (!user) {
      throw new UnauthorizedException('用户不存在');
    }
    if (user.status === UserStatus.BANNED && user.role !== UserRole.ADMIN) {
      throw new UnauthorizedException('账号已被禁用');
    }
    return { userId: payload.userId, role: payload.role };
  }
}
