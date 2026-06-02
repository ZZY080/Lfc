import { Injectable, UnauthorizedException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserRole } from '@shared/enum/user-role.enum';
import { RedisService } from '@integration/redis/service/redis.service';
import { AdminLoginBodyDto } from '@module/auth/dto/admin-auth.dto';
import { AuthTokenDto } from '@module/auth/dto/consumer-auth.dto';

@Injectable()
export class AdminAuthService {
  private readonly PREFIX_REFRESH = 'refresh:token:admin:';

  constructor(
    private readonly jwtService: JwtService,
    private readonly redisService: RedisService,
    private readonly configService: ConfigService,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
  ) {}

  async login(body: AdminLoginBodyDto): Promise<AuthTokenDto> {
    const user = await this.userRepository.findOne({
      where: { email: body.email },
    });
    if (!user || user.role !== UserRole.ADMIN) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    const valid = await bcrypt.compare(body.password, user.password);
    if (!valid) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    return this.issueTokens(user);
  }

  async logout(userId: number) {
    await this.redisService.del(`${this.PREFIX_REFRESH}${userId}`);
    return { message: '已退出登录' };
  }

  private async issueTokens(user: UserEntity): Promise<AuthTokenDto> {
    const payload = { userId: user.id, role: user.role };
    const accessToken = this.jwtService.sign(payload, {
      expiresIn: this.configService.get<string>('JWT_ACCESS_EXPIRES_IN', '2d') as `${number}d`,
    });
    const refreshToken = this.jwtService.sign(payload, {
      expiresIn: this.configService.get<string>('JWT_REFRESH_EXPIRES_IN', '10d') as `${number}d`,
    });

    const refreshTtl = 10 * 24 * 60 * 60;
    await this.redisService.set(
      `${this.PREFIX_REFRESH}${user.id}`,
      refreshToken,
      refreshTtl,
    );

    return {
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        email: user.email,
        studentId: user.studentId,
        role: user.role,
      },
    };
  }
}
