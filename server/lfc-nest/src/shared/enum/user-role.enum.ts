export enum UserRole {
  CONSUMER = 'CONSUMER',
  ADMIN = 'ADMIN',
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  BANNED = 'BANNED',
}

export enum ActivityStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  /** 发起人主动下架，不再公开展示 */
  OFF_SHELF = 'OFF_SHELF',
}
