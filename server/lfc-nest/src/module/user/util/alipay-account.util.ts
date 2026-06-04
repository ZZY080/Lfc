export function maskAlipayLoginId(loginId: string | null | undefined): string | null {
  if (!loginId) {
    return null;
  }
  const trimmed = loginId.trim();
  if (/^1\d{10}$/.test(trimmed)) {
    return `${trimmed.slice(0, 3)}****${trimmed.slice(-4)}`;
  }
  const atIndex = trimmed.indexOf('@');
  if (atIndex > 1) {
    return `${trimmed.slice(0, 2)}***${trimmed.slice(atIndex)}`;
  }
  if (trimmed.length <= 4) {
    return '*'.repeat(trimmed.length);
  }
  return `${trimmed.slice(0, 2)}***${trimmed.slice(-2)}`;
}

export function maskAlipayUserId(userId: string | null | undefined): string | null {
  if (!userId) {
    return null;
  }
  const trimmed = userId.trim();
  if (trimmed.length <= 8) {
    return trimmed;
  }
  return `${trimmed.slice(0, 4)}****${trimmed.slice(-4)}`;
}

export function normalizeAlipayLoginId(loginId: string): string {
  return loginId.trim();
}

export function isValidAlipayLoginId(loginId: string): boolean {
  const normalized = normalizeAlipayLoginId(loginId);
  return /^1\d{10}$/.test(normalized) || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized);
}
