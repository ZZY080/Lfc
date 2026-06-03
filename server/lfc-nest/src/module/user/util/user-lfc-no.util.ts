import { randomInt } from 'crypto';

/** 10 位纯数字，首位不为 0，类似小红书号 */
const LFC_NO_MIN = 1_000_000_000;
const LFC_NO_MAX = 9_999_999_999;
const LFC_NO_PATTERN = /^[1-9]\d{9}$/;

export function generateLfcNo(): string {
  return String(randomInt(LFC_NO_MIN, LFC_NO_MAX + 1));
}

export function normalizeLfcNo(value?: string | null): string | null {
  const trimmed = value?.trim();
  if (!trimmed || !LFC_NO_PATTERN.test(trimmed)) {
    return null;
  }
  return trimmed;
}

export async function generateUniqueLfcNo(
  isTaken: (lfcNo: string) => Promise<boolean>,
): Promise<string> {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    const lfcNo = generateLfcNo();
    if (!(await isTaken(lfcNo))) {
      return lfcNo;
    }
  }
  throw new Error('生成莲峰号失败，请稍后重试');
}
