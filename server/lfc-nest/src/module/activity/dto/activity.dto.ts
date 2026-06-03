export interface CreateActivityBodyDto {
  title?: string;
  description?: string;
  images?: string[];
  location: string;
  startTime: string;
  endTime: string;
  maxParticipants?: number;
}

export interface UpdateActivityBodyDto {
  title?: string;
  description?: string;
  images?: string[];
  location?: string;
  startTime?: string;
  endTime?: string;
  maxParticipants?: number;
}

import { ActivityStatus } from '@shared/enum/user-role.enum';

export interface ReviewActivityBodyDto {
  status: ActivityStatus.APPROVED | ActivityStatus.REJECTED;
}
