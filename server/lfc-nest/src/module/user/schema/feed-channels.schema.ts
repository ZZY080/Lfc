import { ArrayMinSize, IsArray, IsString } from 'class-validator';
import { UpdateFeedChannelsBodyDto } from '@module/user/dto/feed-channels.dto';

export class UpdateFeedChannelsBodySchema implements UpdateFeedChannelsBodyDto {
  @IsArray()
  @ArrayMinSize(1)
  @IsString({ each: true })
  channels: string[];
}
