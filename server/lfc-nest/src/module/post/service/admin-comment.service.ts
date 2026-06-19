import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  AdminCommentListQueryDto,
  AdminUpdateCommentBodyDto,
} from '@module/post/dto/admin-comment.dto';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class AdminCommentService {
  constructor(
    @InjectRepository(PostCommentEntity)
    private readonly commentRepository: Repository<PostCommentEntity>,
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async findAll(adminUserId: number, query: AdminCommentListQueryDto) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const { page, limit, skip } = normalizePagination(query.page, query.limit);
    const qb = this.commentRepository
      .createQueryBuilder('comment')
      .leftJoinAndSelect('comment.author', 'author')
      .leftJoinAndSelect('comment.post', 'post');

    if (query.visibility === 'visible') {
      qb.andWhere('comment.isVisible = :isVisible', { isVisible: true });
    } else if (query.visibility === 'hidden') {
      qb.andWhere('comment.isVisible = :isVisible', { isVisible: false });
    }

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere('comment.content LIKE :keyword', { keyword });
    }

    if (query.postId) {
      qb.andWhere('comment.postId = :postId', { postId: query.postId });
    }

    qb.orderBy('comment.createdAt', 'DESC').skip(skip).take(limit);
    const [items, total] = await qb.getManyAndCount();

    return createPaginatedResult(items, total, page, limit);
  }

  async findOne(adminUserId: number, commentId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const comment = await this.commentRepository.findOne({
      where: { id: commentId },
      relations: ['author', 'post'],
    });
    if (!comment) {
      throw new NotFoundException('评论不存在');
    }
    return comment;
  }

  async update(
    adminUserId: number,
    commentId: number,
    body: AdminUpdateCommentBodyDto,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const comment = await this.getCommentOrThrow(commentId);

    if (body.content !== undefined) {
      comment.content = body.content;
    }
    if (body.isVisible !== undefined) {
      comment.isVisible = body.isVisible;
    }

    return this.commentRepository.save(comment);
  }

  async remove(adminUserId: number, commentId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const comment = await this.getCommentOrThrow(commentId);
    await this.commentRepository.delete(commentId);

    const post = await this.postRepository.findOne({
      where: { id: comment.postId },
    });
    if (post) {
      post.commentCount = Math.max(0, post.commentCount - 1);
      await this.postRepository.save(post);
    }

    return { message: '评论已删除' };
  }

  private async getCommentOrThrow(commentId: number) {
    const comment = await this.commentRepository.findOne({
      where: { id: commentId },
    });
    if (!comment) {
      throw new NotFoundException('评论不存在');
    }
    return comment;
  }
}
