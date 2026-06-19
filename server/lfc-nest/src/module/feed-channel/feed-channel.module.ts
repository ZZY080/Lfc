import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { FeedChannelEntity } from '@module/feed-channel/entity/feed-channel.entity';
import { FeedChannelService } from '@module/feed-channel/service/feed-channel.service';
import { ConsumerFeedChannelController } from '@module/feed-channel/controller/consumer-feed-channel.controller';

@Module({
  imports: [TypeOrmModule.forFeature([FeedChannelEntity])],
  controllers: [ConsumerFeedChannelController],
  providers: [FeedChannelService],
  exports: [FeedChannelService],
})
export class FeedChannelModule {}
