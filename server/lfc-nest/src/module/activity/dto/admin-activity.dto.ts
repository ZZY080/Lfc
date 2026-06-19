import { ActivityStatus } from '@shared/enum/user-role.enum';

export interface AdminUpdateActivityBodyDto {
  title?: string;
  description?: string;
  location?: string;
  status?: ActivityStatus;
  reviewComment?: string;
  maxParticipants?: number;
  fee?: number;
}
