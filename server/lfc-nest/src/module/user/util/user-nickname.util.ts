export function generateDefaultNickname(): string {
  const suffix = Math.floor(1000 + Math.random() * 899999);
  return `莲峰校园${suffix}`;
}

export function normalizeNickname(value?: string | null): string | null {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
}
