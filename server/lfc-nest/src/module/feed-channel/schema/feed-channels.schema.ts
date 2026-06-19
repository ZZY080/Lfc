import { ArrayMinSize, IsArray, IsString } from 'class-validator';
import { UpdateFeedChannelsBodyDto } from '@module/feed-channel/dto/feed-channel.dto';

export class UpdateFeedChannelsBodySchema implements UpdateFeedChannelsBodyDto {
  @IsArray()
  @ArrayMinSize(1)
  @IsString({ each: true })
  channels: string[];
}
