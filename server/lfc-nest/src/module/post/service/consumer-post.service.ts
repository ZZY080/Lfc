import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  CreatePostBodyDto,
  PostFeedQueryDto,
  PostFeedResultDto,
  UpdatePostBodyDto,
} from '@module/post/dto/consumer-post.dto';

@Injectable()
export class ConsumerPostService {
  constructor(
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async create(userId: number, body: CreatePostBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const post = this.postRepository.create({ ...body, authorId: userId });
    return this.postRepository.save(post);
  }

  findAll() {
    return this.postRepository.find({
      relations: ['author'],
      order: { createdAt: 'DESC' },
    });
  }

  async findFeed(query: PostFeedQueryDto): Promise<PostFeedResultDto> {
    const page = query.page ?? 1;
    const limit = Math.min(query.limit ?? 10, 30);
    const skip = (page - 1) * limit;
    const sort = query.sort ?? (query.tab === '最新' ? 'latest' : 'recommend');

    const qb = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.author', 'author');

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere('(post.title LIKE :keyword OR post.content LIKE :keyword)', {
        keyword,
      });
    } else if (query.tab && !['推荐', '最新'].includes(query.tab)) {
      const tabKeyword = `%${query.tab.trim()}%`;
      qb.andWhere('(post.title LIKE :tabKeyword OR post.content LIKE :tabKeyword)', {
        tabKeyword,
      });
    }

    if (sort === 'latest') {
      qb.orderBy('post.createdAt', 'DESC');
    } else {
      qb.orderBy('post.id', 'DESC');
    }

    const [items, total] = await qb.skip(skip).take(limit).getManyAndCount();

    return {
      items,
      total,
      page,
      limit,
      hasMore: skip + items.length < total,
    };
  }

  findMine(userId: number) {
    return this.postRepository.find({
      where: { authorId: userId },
      order: { createdAt: 'DESC' },
    });
  }

  async findOne(id: number) {
    const post = await this.postRepository.findOne({
      where: { id },
      relations: ['author'],
    });
    if (!post) {
      throw new NotFoundException('信息不存在');
    }
    return post;
  }

  async update(userId: number, id: number, body: UpdatePostBodyDto) {
    const post = await this.findOne(id);
    if (post.authorId !== userId) {
      throw new ForbiddenException('无权修改该信息');
    }
    Object.assign(post, body);
    return this.postRepository.save(post);
  }

  async remove(userId: number, id: number) {
    const post = await this.findOne(id);
    if (post.authorId !== userId) {
      throw new ForbiddenException('无权删除该信息');
    }
    await this.postRepository.remove(post);
    return { message: '删除成功' };
  }
}
