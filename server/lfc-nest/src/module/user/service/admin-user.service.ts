import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { UserEntity } from '@module/user/entity/user.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole, UserStatus } from '@shared/enum/user-role.enum';
import {
  AdminCreateUserBodyDto,
  AdminUpdateUserBodyDto,
  AdminUpdateUserRoleBodyDto,
  AdminUpdateUserStatusBodyDto,
  AdminUserListQueryDto,
} from '@module/user/dto/admin-user.dto';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';
import { generateDefaultNickname } from '@module/user/util/user-nickname.util';
import { generateUniqueLfcNo } from '@module/user/util/user-lfc-no.util';
import { RedisService } from '@integration/redis/service/redis.service';

@Injectable()
export class AdminUserService {
  private readonly CONSUMER_REFRESH_PREFIX = 'refresh:token:consumer:';
  private readonly ADMIN_REFRESH_PREFIX = 'refresh:token:admin:';

  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly configService: ConfigService,
    private readonly redisService: RedisService,
  ) {}

  async findAll(adminUserId: number, query: AdminUserListQueryDto) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const { page, limit, skip } = normalizePagination(query.page, query.limit);
    const qb = this.userRepository.createQueryBuilder('user');

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere(
        '(user.email LIKE :keyword OR user.studentId LIKE :keyword OR user.realName LIKE :keyword OR user.nickname LIKE :keyword OR user.lfcNo LIKE :keyword)',
        { keyword },
      );
    }

    if (query.role) {
      qb.andWhere('user.role = :role', { role: query.role });
    }

    if (query.status) {
      qb.andWhere('user.status = :status', { status: query.status });
    }

    qb.orderBy('user.createdAt', 'DESC').skip(skip).take(limit);
    const [items, total] = await qb.getManyAndCount();

    return createPaginatedResult(
      items.map((user) => this.sanitizeUser(user)),
      total,
      page,
      limit,
    );
  }

  async findOne(adminUserId: number, targetUserId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const user = await this.userRepository.findOne({
      where: { id: targetUserId },
    });
    if (!user) {
      throw new NotFoundException('用户不存在');
    }
    return this.sanitizeUser(user);
  }

  async create(adminUserId: number, body: AdminCreateUserBodyDto) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);

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

    const role = body.role ?? UserRole.CONSUMER;
    const hashedPassword = await bcrypt.hash(body.password, 10);
    const nickname = await this.generateUniqueNickname();
    const lfcNo = await this.createUniqueLfcNo();
    const region = this.configService.get<string>(
      'ALIYUN_OSS_REGION',
      'oss-cn-shanghai',
    );
    const privateBucket = this.configService.get<string>(
      'ALIYUN_OSS_BUCKET_PRIVATE',
      'lfc-dev-private',
    );

    const saved = await this.userRepository.save(
      this.userRepository.create({
        email: body.email.trim(),
        password: hashedPassword,
        studentId: body.studentId.trim(),
        realName: body.realName.trim(),
        studentCardUrl: `https://${privateBucket}.${region}.aliyuncs.com/system/admin-placeholder.jpg`,
        nickname,
        lfcNo,
        role,
        status: UserStatus.ACTIVE,
      }),
    );

    return this.sanitizeUser(saved);
  }

  async update(
    adminUserId: number,
    targetUserId: number,
    body: AdminUpdateUserBodyDto,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const user = await this.getUserOrThrow(targetUserId);

    if (body.email && body.email !== user.email) {
      const existing = await this.userRepository.findOne({
        where: { email: body.email },
      });
      if (existing) {
        throw new ConflictException('该邮箱已被注册');
      }
      user.email = body.email.trim();
    }

    if (body.studentId && body.studentId !== user.studentId) {
      const existing = await this.userRepository.findOne({
        where: { studentId: body.studentId },
      });
      if (existing) {
        throw new ConflictException('该学号已被注册');
      }
      user.studentId = body.studentId.trim();
    }

    if (body.realName !== undefined) {
      user.realName = body.realName.trim();
    }

    if (body.nickname !== undefined) {
      user.nickname = body.nickname?.trim() || null;
    }

    if (body.role !== undefined) {
      if (adminUserId === targetUserId && body.role !== UserRole.ADMIN) {
        throw new BadRequestException('不能修改自己的管理员角色');
      }
      user.role = body.role;
    }

    const saved = await this.userRepository.save(user);
    return this.sanitizeUser(saved);
  }

  async updateStatus(
    adminUserId: number,
    targetUserId: number,
    body: AdminUpdateUserStatusBodyDto,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    if (adminUserId === targetUserId) {
      throw new BadRequestException('不能修改自己的账号状态');
    }

    const user = await this.getUserOrThrow(targetUserId);
    if (user.role === UserRole.ADMIN && body.status === UserStatus.BANNED) {
      throw new BadRequestException('不能封禁管理员账号');
    }

    user.status = body.status;
    if (body.status === UserStatus.BANNED) {
      user.banReason = body.banReason?.trim() || '违反平台规定';
      user.bannedAt = new Date();
      await this.revokeUserSessions(user.id);
    } else {
      user.banReason = null;
      user.bannedAt = null;
    }

    const saved = await this.userRepository.save(user);
    return this.sanitizeUser(saved);
  }

  async updateRole(
    adminUserId: number,
    targetUserId: number,
    body: AdminUpdateUserRoleBodyDto,
  ) {
    return this.update(adminUserId, targetUserId, { role: body.role });
  }

  async remove(adminUserId: number, targetUserId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    if (adminUserId === targetUserId) {
      throw new BadRequestException('不能删除自己的账号');
    }

    const user = await this.getUserOrThrow(targetUserId);
    if (user.role === UserRole.ADMIN) {
      throw new BadRequestException('不能删除管理员账号');
    }

    await this.revokeUserSessions(user.id);
    await this.userRepository.delete(targetUserId);
    return { message: '用户已删除' };
  }

  private async getUserOrThrow(userId: number) {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) {
      throw new NotFoundException('用户不存在');
    }
    return user;
  }

  private async revokeUserSessions(userId: number) {
    await Promise.all([
      this.redisService.del(`${this.CONSUMER_REFRESH_PREFIX}${userId}`),
      this.redisService.del(`${this.ADMIN_REFRESH_PREFIX}${userId}`),
    ]);
  }

  private sanitizeUser(user: UserEntity) {
    const { password: _password, ...rest } = user;
    return rest;
  }

  private async generateUniqueNickname(): Promise<string> {
    for (let attempt = 0; attempt < 5; attempt += 1) {
      const nickname = generateDefaultNickname();
      const exists = await this.userRepository.exist({ where: { nickname } });
      if (!exists) {
        return nickname;
      }
    }
    return `${generateDefaultNickname()}${Date.now() % 100000}`;
  }

  private createUniqueLfcNo() {
    return generateUniqueLfcNo((lfcNo) =>
      this.userRepository.exist({ where: { lfcNo } }),
    );
  }
}
