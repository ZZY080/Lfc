import { Transform } from 'class-transformer';
import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';
import { LoginBodyDto, RegisterBodyDto } from '@module/auth/dto/consumer-auth.dto';

export class LoginBodySchema implements LoginBodyDto {
  @Transform(({ value }) => value?.trim())
  @IsEmail({}, { message: '请输入正确的邮箱地址' })
  @IsNotEmpty({ message: '邮箱不能为空' })
  email: string;

  @IsString()
  @MinLength(6, { message: '密码至少6位' })
  password: string;
}

export class RegisterBodySchema implements RegisterBodyDto {
  @Transform(({ value }) => value?.trim())
  @IsEmail({}, { message: '请输入正确的邮箱地址' })
  @IsNotEmpty({ message: '邮箱不能为空' })
  email: string;

  @IsString()
  @MinLength(6, { message: '密码至少6位' })
  password: string;

  @Transform(({ value }) => value?.trim())
  @IsString()
  @IsNotEmpty({ message: '学号不能为空' })
  studentId: string;
}
