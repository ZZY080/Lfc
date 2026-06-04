import { IsString, MaxLength, MinLength } from 'class-validator';
import { BindAlipayOAuthBodyDto } from '@module/user/dto/alipay-account.dto';

export class BindAlipayOAuthBodySchema implements BindAlipayOAuthBodyDto {
  @IsString()
  @MinLength(8)
  @MaxLength(128)
  authCode: string;
}
