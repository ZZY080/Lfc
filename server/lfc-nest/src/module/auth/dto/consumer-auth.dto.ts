export interface LoginBodyDto {
  email: string;
  password: string;
}

export interface RegisterBodyDto {
  email: string;
  password: string;
  studentId: string;
}

export interface AuthTokenDto {
  accessToken: string;
  refreshToken: string;
  user: {
    id: number;
    email: string;
    studentId: string;
    role: string;
  };
}
