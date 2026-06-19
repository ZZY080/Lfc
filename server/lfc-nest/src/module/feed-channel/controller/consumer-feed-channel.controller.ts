import { Controller, Get } from '@nestjs/common';
import { FeedChannelService } from '@module/feed-channel/service/feed-channel.service';

@Controller('consumer/feed-channels')
export class ConsumerFeedChannelController {
  constructor(private readonly feedChannelService: FeedChannelService) {}

  @Get()
  getCatalog() {
    return this.feedChannelService.getCatalog();
  }
}
