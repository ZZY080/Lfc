import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
  Unique,
} from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';

@Entity({ name: 'post_comment_like' })
@Unique(['commentId', 'userId'])
export class PostCommentLikeEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'comment_id' })
  commentId: number;

  @Column({ name: 'user_id' })
  userId: number;

  @ManyToOne(() => PostCommentEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'comment_id' })
  comment: PostCommentEntity;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
