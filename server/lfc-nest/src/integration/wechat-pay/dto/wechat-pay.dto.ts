export interface WechatAppPayResult {
  appId: string;
  partnerId: string;
  prepayId: string;
  packageValue: string;
  nonceStr: string;
  timeStamp: string;
  sign: string;
}

export interface WechatPayNotifyPayload {
  out_trade_no?: string;
  transaction_id?: string;
  trade_state?: string;
  amount?: {
    total?: number;
  };
  [key: string]: unknown;
}
