import {
  ConflictException,
  Injectable,
  OnModuleInit,
  UnauthorizedException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserRole } from '@shared/enum/user-role.enum';
import { RedisService } from '@integration/redis/service/redis.service';
import {
  AuthTokenDto,
  LoginBodyDto,
  RegisterBodyDto,
} from '@module/auth/dto/consumer-auth.dto';
import { MessageService } from '@module/message/service/message.service';

@Injectable()
export class ConsumerAuthService implements OnModuleInit {
  private readonly PREFIX_REFRESH = 'refresh:token:consumer:';

  constructor(
    private readonly jwtService: JwtService,
    private readonly redisService: RedisService,
    private readonly configService: ConfigService,
    private readonly messageService: MessageService,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
  ) {}

  async onModuleInit() {
    await this.seedAdminUser();
  }

  async register(body: RegisterBodyDto, studentCardUrl: string): Promise<AuthTokenDto> {
    const existingEmail = await this.userRepository.findOne({
      where: { email: body.email },
    });
    if (existingEmail) {
      throw new ConflictException('该邮箱已被注册');
    }

    const existingStudent = await this.userRepository.findOne({
      where: { studentId: body.studentId },
    });
    if (existingStudent) {
      throw new ConflictException('该学号已被注册');
    }

    const hashedPassword = await bcrypt.hash(body.password, 10);
    const user = await this.userRepository.save(
      this.userRepository.create({
        email: body.email,
        password: hashedPassword,
        studentId: body.studentId,
        studentCardUrl,
        role: UserRole.CONSUMER,
      }),
    );

    await this.messageService.sendWelcome(user.id);

    return this.issueTokens(user);
  }

  async login(body: LoginBodyDto): Promise<AuthTokenDto> {
    const user = await this.userRepository.findOne({
      where: { email: body.email },
    });
    if (!user || user.role !== UserRole.CONSUMER) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    const valid = await bcrypt.compare(body.password, user.password);
    if (!valid) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    return this.issueTokens(user);
  }

  async refresh(refreshToken: string): Promise<AuthTokenDto> {
    let payload: { userId: number; role: string };
    try {
      payload = this.jwtService.verify(refreshToken);
    } catch {
      throw new UnauthorizedException('refreshToken 无效或已过期');
    }

    const stored = await this.redisService.get(
      `${this.PREFIX_REFRESH}${payload.userId}`,
    );
    if (!stored || stored !== refreshToken) {
      throw new UnauthorizedException('refreshToken 无效或已过期');
    }

    const user = await this.userRepository.findOne({
      where: { id: payload.userId },
    });
    if (!user || user.role !== UserRole.CONSUMER) {
      throw new UnauthorizedException('用户不存在');
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

  private async seedAdminUser() {
    const adminEmail = this.configService.get<string>(
      'ADMIN_EMAIL',
      'admin@lfc.edu',
    );
    const adminPassword = this.configService.get<string>(
      'ADMIN_PASSWORD',
      'admin123456',
    );

    const existing = await this.userRepository.findOne({
      where: { email: adminEmail },
    });
    if (existing) {
      return;
    }

    const hashedPassword = await bcrypt.hash(adminPassword, 10);
    await this.userRepository.save(
      this.userRepository.create({
        email: adminEmail,
        password: hashedPassword,
        studentId: 'ADMIN001',
        studentCardUrl: '/uploads/student-cards/admin-placeholder.jpg',
        role: UserRole.ADMIN,
      }),
    );
    console.log(`默认管理员已创建: ${adminEmail}`);
  }
}
