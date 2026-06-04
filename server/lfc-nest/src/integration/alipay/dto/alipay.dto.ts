export interface AlipayNotifyPayload {
  out_trade_no?: string;
  trade_no?: string;
  trade_status?: string;
  total_amount?: string;
  [key: string]: string | undefined;
}

export interface AlipayAppPayResult {
  orderStr: string;
}

export interface AlipayAppAuthInfoResult {
  authInfo: string;
}

export interface AlipayOAuthUserInfo {
  userId: string;
  nickName: string | null;
}
