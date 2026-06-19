import {
  IsEmail,
  IsEnum,
  IsOptional,
  IsString,
  MaxLength,
  MinLength,
  ValidateIf,
} from 'class-validator';
import {
  AdminCreateUserBodyDto,
  AdminUpdateUserBodyDto,
  AdminUpdateUserRoleBodyDto,
  AdminUpdateUserStatusBodyDto,
  AdminUserListQueryDto,
} from '@module/user/dto/admin-user.dto';
import { PaginationQuerySchema } from '@shared/schema/pagination.schema';
import { UserRole, UserStatus } from '@shared/enum/user-role.enum';

export class AdminUserListQuerySchema
  extends PaginationQuerySchema
  implements AdminUserListQueryDto
{
  @IsOptional()
  @IsString()
  @MaxLength(64)
  keyword?: string;

  @IsOptional()
  @IsEnum(UserRole)
  role?: UserRole;

  @IsOptional()
  @IsEnum(UserStatus)
  status?: UserStatus;
}

export class AdminCreateUserBodySchema implements AdminCreateUserBodyDto {
  @IsEmail()
  email: string;

  @IsString()
  @MinLength(6)
  @MaxLength(64)
  password: string;

  @IsString()
  @MinLength(1)
  @MaxLength(32)
  studentId: string;

  @IsString()
  @MinLength(1)
  @MaxLength(32)
  realName: string;

  @IsOptional()
  @IsEnum(UserRole)
  role?: UserRole;
}

export class AdminUpdateUserBodySchema implements AdminUpdateUserBodyDto {
  @IsOptional()
  @IsEmail()
  email?: string;

  @IsOptional()
  @IsString()
  @MinLength(1)
  @MaxLength(32)
  studentId?: string;

  @IsOptional()
  @IsString()
  @MinLength(1)
  @MaxLength(32)
  realName?: string;

  @IsOptional()
  @ValidateIf((_, value) => value !== null)
  @IsString()
  @MaxLength(50)
  nickname?: string | null;

  @IsOptional()
  @IsEnum(UserRole)
  role?: UserRole;
}

export class AdminUpdateUserStatusBodySchema
  implements AdminUpdateUserStatusBodyDto
{
  @IsEnum(UserStatus)
  status: UserStatus;

  @IsOptional()
  @IsString()
  @MaxLength(255)
  banReason?: string;
}

export class AdminUpdateUserRoleBodySchema implements AdminUpdateUserRoleBodyDto {
  @IsEnum(UserRole)
  role: UserRole;
}
