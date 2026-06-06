import type { PromotionMetaDto } from '@module/promotion/dto/promotion.dto';

export function isPromotionActive(until?: Date | string | null): boolean {
  if (!until) {
    return false;
  }
  const date = until instanceof Date ? until : new Date(until);
  return !Number.isNaN(date.getTime()) && date.getTime() > Date.now();
}

export function addHours(base: Date, hours: number): Date {
  return new Date(base.getTime() + hours * 60 * 60 * 1000);
}

export function toIso(value: Date | null | undefined): string | null {
  if (!value) {
    return null;
  }
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
}

export function parsePromotionMoneyInput(
  value: string | number | undefined,
  label: string,
): number {
  if (value == null || value === '') {
    throw new Error(`${label}不能为空`);
  }
  const parsed = Number.parseFloat(String(value));
  if (!Number.isFinite(parsed) || parsed < 0) {
    throw new Error(`${label}格式不正确`);
  }
  return parsed;
}

export function formatPromotionMoney(value: number): string {
  return value.toFixed(2);
}

export function comparePromotionMoney(a: string, b: string): number {
  return Number.parseFloat(a) - Number.parseFloat(b);
}

export function buildPostPromotionMeta(input: {
  boostedUntil: Date | null;
  lastBoostedAt: Date | null;
  boostBidAmount?: string | null;
  isOwner: boolean;
  boostHours: number;
  cooldownHours: number;
  activeLabel: string;
  actionLabel: string;
  paidEnabled: boolean;
  basePrice: string;
  minBidAmount: string | null;
}): PromotionMetaDto {
  const isActive = isPromotionActive(input.boostedUntil);
  const cooldownMs = input.cooldownHours * 60 * 60 * 1000;
  let nextAvailableAt: string | null = null;
  let canApply = input.isOwner;

  if (input.isOwner && input.lastBoostedAt) {
    const next = input.lastBoostedAt.getTime() + cooldownMs;
    if (next > Date.now()) {
      canApply = false;
      nextAvailableAt = new Date(next).toISOString();
    }
  }

  return {
    isActive,
    label: isActive ? input.activeLabel : null,
    badge: isActive ? input.activeLabel : null,
    until: toIso(input.boostedUntil),
    canApply,
    nextAvailableAt,
    cooldownHours: input.cooldownHours,
    durationHours: input.boostHours,
    requiresPayment: input.paidEnabled,
    price: input.paidEnabled ? input.basePrice : null,
    minBidAmount: input.paidEnabled ? input.minBidAmount : null,
    bidAmount: isActive ? input.boostBidAmount ?? input.basePrice : null,
  };
}

export function buildActivityPromotionMeta(input: {
  promotedUntil: Date | null;
  lastPromotedAt: Date | null;
  promoteBidAmount?: string | null;
  isOwner: boolean;
  promoteHours: number;
  cooldownHours: number;
  activeLabel: string;
  actionLabel: string;
  paidEnabled: boolean;
  basePrice: string;
  minBidAmount: string | null;
}): PromotionMetaDto {
  const isActive = isPromotionActive(input.promotedUntil);
  const cooldownMs = input.cooldownHours * 60 * 60 * 1000;
  let nextAvailableAt: string | null = null;
  let canApply = input.isOwner;

  if (input.isOwner && input.lastPromotedAt) {
    const next = input.lastPromotedAt.getTime() + cooldownMs;
    if (next > Date.now()) {
      canApply = false;
      nextAvailableAt = new Date(next).toISOString();
    }
  }

  return {
    isActive,
    label: isActive ? input.activeLabel : null,
    badge: isActive ? input.activeLabel : null,
    until: toIso(input.promotedUntil),
    canApply,
    nextAvailableAt,
    cooldownHours: input.cooldownHours,
    durationHours: input.promoteHours,
    requiresPayment: input.paidEnabled,
    price: input.paidEnabled ? input.basePrice : null,
    minBidAmount: input.paidEnabled ? input.minBidAmount : null,
    bidAmount: isActive ? input.promoteBidAmount ?? input.basePrice : null,
  };
}
