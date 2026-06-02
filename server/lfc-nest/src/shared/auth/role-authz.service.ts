import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserRole } from '@shared/enum/user-role.enum';

@Injectable()
export class RoleAuthzService {
  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
  ) {}

  async assertRole(userId: number, role: UserRole): Promise<UserEntity> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) {
      throw new NotFoundException('用户不存在');
    }
    if (user.role !== role) {
      throw new ForbiddenException('无权访问该资源');
    }
    return user;
  }
}
