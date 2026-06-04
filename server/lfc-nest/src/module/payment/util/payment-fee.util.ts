export interface PlatformSettlement {
  platformFee: string;
  payeeAmount: string;
}

export function calculatePlatformSettlement(
  totalAmount: string,
  feeRate: number,
  minFee = 0,
): PlatformSettlement {
  const total = Number.parseFloat(totalAmount);
  if (!Number.isFinite(total) || total <= 0) {
    return { platformFee: '0.00', payeeAmount: '0.00' };
  }

  const normalizedRate = Number.isFinite(feeRate) && feeRate > 0 ? feeRate : 0;
  let fee = total * normalizedRate;
  if (minFee > 0) {
    fee = Math.max(fee, minFee);
  }
  fee = Math.round(fee * 100) / 100;

  if (fee >= total) {
    fee = Math.max(0, Math.round((total - 0.01) * 100) / 100);
  }

  const payee = Math.round((total - fee) * 100) / 100;
  return {
    platformFee: fee.toFixed(2),
    payeeAmount: payee.toFixed(2),
  };
}

export function formatFeeRateLabel(feeRate: number): string {
  const percent = feeRate * 100;
  const rounded = Math.round(percent * 100) / 100;
  return Number.isInteger(rounded) ? `${rounded}%` : `${rounded}%`;
}
