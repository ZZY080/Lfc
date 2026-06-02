import { Transform } from 'class-transformer';
import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';
import { AdminLoginBodyDto } from '@module/auth/dto/admin-auth.dto';

export class AdminLoginBodySchema implements AdminLoginBodyDto {
  @Transform(({ value }) => value?.trim())
  @IsEmail({}, { message: '请输入正确的邮箱地址' })
  @IsNotEmpty({ message: '邮箱不能为空' })
  email: string;

  @IsString()
  @MinLength(6, { message: '密码至少6位' })
  password: string;
}
