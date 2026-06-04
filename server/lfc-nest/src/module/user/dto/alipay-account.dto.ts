export interface BindAlipayOAuthBodyDto {
  authCode: string;
}

export interface BindAlipayAccountBodyDto {
  alipayLoginId: string;
  alipayRealName?: string;
}

export interface AlipayAccountBindingDto {
  alipayBound: boolean;
  alipayLoginIdMasked: string | null;
  alipayRealName: string | null;
  alipayBoundAt: Date | null;
  alipayRoyaltyBound?: boolean;
  bindMethod?: 'OAUTH' | 'MANUAL' | null;
}

export interface AlipayOAuthAuthInfoDto {
  authInfo: string;
}
