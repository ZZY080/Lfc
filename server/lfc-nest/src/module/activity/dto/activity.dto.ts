export interface CreateActivityBodyDto {
  title?: string;
  description?: string;
  images?: string[];
  location: string;
  latitude?: number;
  longitude?: number;
  startTime: string;
  endTime: string;
  maxParticipants?: number;
  fee?: number;
}

export interface UpdateActivityBodyDto {
  title?: string;
  description?: string;
  images?: string[];
  location?: string;
  latitude?: number;
  longitude?: number;
  startTime?: string;
  endTime?: string;
  maxParticipants?: number;
  fee?: number;
}

import { ActivityStatus } from '@shared/enum/user-role.enum';

export interface ReviewActivityBodyDto {
  status: ActivityStatus.APPROVED | ActivityStatus.REJECTED;
}
