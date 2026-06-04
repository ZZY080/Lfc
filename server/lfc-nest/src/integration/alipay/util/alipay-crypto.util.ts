import crypto from 'crypto';

function formatPrivateKey(privateKey: string): string {
  const normalized = privateKey.replace(/\\n/g, '\n').trim();
  if (normalized.includes('BEGIN')) {
    return normalized;
  }
  const body = normalized.match(/.{1,64}/g)?.join('\n') ?? normalized;
  return `-----BEGIN RSA PRIVATE KEY-----\n${body}\n-----END RSA PRIVATE KEY-----`;
}

function formatPublicKey(publicKey: string): string {
  const normalized = publicKey.replace(/\\n/g, '\n').trim();
  if (normalized.includes('BEGIN')) {
    return normalized;
  }
  const body = normalized.match(/.{1,64}/g)?.join('\n') ?? normalized;
  return `-----BEGIN PUBLIC KEY-----\n${body}\n-----END PUBLIC KEY-----`;
}

function formatTimestamp(date: Date): string {
  const pad = (value: number) => value.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function getAlipaySignContent(params: Record<string, string>): string {
  return Object.keys(params)
    .filter(
      (key) =>
        key !== 'sign' &&
        key !== 'sign_type' &&
        params[key] !== undefined &&
        params[key] !== '',
    )
    .sort()
    .map((key) => `${key}=${params[key]}`)
    .join('&');
}

export function rsaSign(content: string, privateKey: string): string {
  const signer = crypto.createSign('RSA-SHA256');
  signer.update(content, 'utf8');
  return signer.sign(formatPrivateKey(privateKey), 'base64');
}

export function rsaVerify(
  content: string,
  sign: string,
  publicKey: string,
): boolean {
  const verifier = crypto.createVerify('RSA-SHA256');
  verifier.update(content, 'utf8');
  return verifier.verify(formatPublicKey(publicKey), sign, 'base64');
}

/** App 支付宝授权登录（AuthTask.authV2）参数串 */
export function buildAppAuthInfoString(input: {
  appId: string;
  pid: string;
  privateKey: string;
  targetId: string;
}): string {
  const params: Record<string, string> = {
    apiname: 'com.alipay.account.auth',
    method: 'alipay.open.auth.sdk.code.get',
    app_id: input.appId,
    app_name: 'mc',
    biz_type: 'openservice',
    pid: input.pid,
    product_id: 'APP_FAST_LOGIN',
    scope: 'auth_user',
    target_id: input.targetId,
    auth_type: 'AUTHACCOUNT',
    sign_type: 'RSA2',
  };
  const signContent = getAlipaySignContent(params);
  params.sign = rsaSign(signContent, input.privateKey);

  return Object.keys(params)
    .map((key) => `${key}=${encodeURIComponent(params[key])}`)
    .join('&');
}

export function buildAppPayOrderString(input: {
  appId: string;
  privateKey: string;
  notifyUrl: string;
  outTradeNo: string;
  totalAmount: string;
  subject: string;
  enableRoyalty?: boolean;
}): string {
  const bizContent: Record<string, unknown> = {
    out_trade_no: input.outTradeNo,
    total_amount: input.totalAmount,
    subject: input.subject,
    product_code: 'QUICK_MSECURITY_PAY',
  };
  if (input.enableRoyalty) {
    bizContent.royalty_info = { royalty_type: 'ROYALTY' };
  }

  const params: Record<string, string> = {
    app_id: input.appId,
    method: 'alipay.trade.app.pay',
    format: 'JSON',
    charset: 'utf-8',
    sign_type: 'RSA2',
    timestamp: formatTimestamp(new Date()),
    version: '1.0',
    notify_url: input.notifyUrl,
    biz_content: JSON.stringify(bizContent),
  };

  const signContent = getAlipaySignContent(params);
  params.sign = rsaSign(signContent, input.privateKey);

  return Object.keys(params)
    .map((key) => `${key}=${encodeURIComponent(params[key])}`)
    .join('&');
}

export function verifyAlipayNotify(
  payload: Record<string, string>,
  publicKey: string,
): boolean {
  const sign = payload.sign;
  if (!sign) {
    return false;
  }
  const content = getAlipaySignContent(payload);
  return rsaVerify(content, sign, publicKey);
}
