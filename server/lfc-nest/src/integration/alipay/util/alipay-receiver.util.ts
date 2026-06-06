export type AlipayReceiverType = 'userId' | 'openId' | 'loginName';

export interface AlipayReceiverAccount {
  type: AlipayReceiverType;
  account: string;
}

/** 2088 开头的 16 位支付宝 userId */
export function isAlipayLegacyUserId(value: string | null | undefined): boolean {
  return /^2088\d{12}$/.test(value?.trim() ?? '');
}

export function resolveAlipayReceiver(input: {
  payeeUserId?: string | null;
  payeeLoginId?: string | null;
}): AlipayReceiverAccount | null {
  const userIdentifier = input.payeeUserId?.trim();
  if (userIdentifier) {
    if (isAlipayLegacyUserId(userIdentifier)) {
      return { type: 'userId', account: userIdentifier };
    }
    return { type: 'openId', account: userIdentifier };
  }

  const loginId = input.payeeLoginId?.trim();
  if (loginId) {
    return { type: 'loginName', account: loginId };
  }

  return null;
}
