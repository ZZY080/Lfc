import { IsOptional, IsString, MaxLength } from 'class-validator';
import { BindAlipayAccountBodyDto } from '@module/user/dto/alipay-account.dto';

export class BindAlipayAccountBodySchema implements BindAlipayAccountBodyDto {
  @IsString()
  @MaxLength(64)
  alipayLoginId: string;

  @IsOptional()
  @IsString()
  @MaxLength(32)
  alipayRealName?: string;
}
