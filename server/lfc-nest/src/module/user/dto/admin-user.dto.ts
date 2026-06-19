import { UserRole, UserStatus } from '@shared/enum/user-role.enum';

export interface AdminUserListQueryDto {
  page?: number;
  limit?: number;
  keyword?: string;
  role?: UserRole;
  status?: UserStatus;
}

export interface AdminCreateUserBodyDto {
  email: string;
  password: string;
  studentId: string;
  realName: string;
  role?: UserRole;
}

export interface AdminUpdateUserBodyDto {
  email?: string;
  studentId?: string;
  realName?: string;
  nickname?: string | null;
  role?: UserRole;
}

export interface AdminUpdateUserStatusBodyDto {
  status: UserStatus;
  banReason?: string;
}

export interface AdminUpdateUserRoleBodyDto {
  role: UserRole;
}
