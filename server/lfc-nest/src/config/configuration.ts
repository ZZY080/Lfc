import { registerAs } from '@nestjs/config';

const appConfiguration = registerAs(
  'app',
  async (): Promise<IAppConfig> => ({
    host: process.env.HOST,
    port: parseInt(process.env.PORT, 10),
  }),
);

const mysqlConfiguration = registerAs(
  'mysql',
  async (): Promise<IMysqlConfig> => ({
    host: process.env.MYSQL_HOST,
    port: parseInt(process.env.MYSQL_PORT, 10),
    username: process.env.MYSQL_USERNAME,
    password: process.env.MYSQL_PASSWORD,
    database: process.env.MYSQL_DATABASE,
  }),
);

const redisConfiguration = registerAs(
  'redis',
  async (): Promise<IRedisConfig> => ({
    protocol: process.env.REDIS_PROTOCOL,
    host: process.env.REDIS_HOST,
    port: process.env.REDIS_PORT,
    password: process.env.REDIS_PASSWORD,
    db: Number.parseInt(process.env.REDIS_DB, 10),
  }),
);

interface IAppConfig {
  host: string;
  port: number;
}

interface IMysqlConfig {
  host: string;
  port: number;
  username: string;
  password: string;
  database: string;
}

interface IRedisConfig {
  protocol: string;
  host: string;
  port: string;
  password?: string;
  db: number;
}

const alipayConfiguration = registerAs(
  'alipay',
  async (): Promise<IAlipayConfig> => ({
    appId: process.env.ALIPAY_APP_ID ?? '',
    privateKey: normalizePem(process.env.ALIPAY_PRIVATE_KEY),
    alipayPublicKey: normalizePem(process.env.ALIPAY_ALIPAY_PUBLIC_KEY),
    gateway:
      process.env.ALIPAY_GATEWAY ??
      'https://openapi.alipay.com/gateway.do',
    notifyUrl: process.env.ALIPAY_NOTIFY_URL ?? '',
    /** 商户 PID，App 支付宝授权登录必填 */
    pid: process.env.ALIPAY_PID ?? '',
  }),
);

interface IAlipayConfig {
  appId: string;
  privateKey: string;
  alipayPublicKey: string;
  gateway: string;
  notifyUrl: string;
  pid: string;
}

const wechatPayConfiguration = registerAs(
  'wechatPay',
  async (): Promise<IWechatPayConfig> => ({
    appId: process.env.WECHAT_PAY_APP_ID ?? '',
    mchId: process.env.WECHAT_PAY_MCH_ID ?? '',
    apiV3Key: process.env.WECHAT_PAY_API_V3_KEY ?? '',
    privateKey: normalizePem(process.env.WECHAT_PAY_PRIVATE_KEY),
    serialNo: process.env.WECHAT_PAY_SERIAL_NO ?? '',
    notifyUrl: process.env.WECHAT_PAY_NOTIFY_URL ?? '',
  }),
);

interface IWechatPayConfig {
  appId: string;
  mchId: string;
  apiV3Key: string;
  privateKey: string;
  serialNo: string;
  notifyUrl: string;
}

const paymentConfiguration = registerAs(
  'payment',
  async (): Promise<IPaymentConfig> => ({
    /** C2C 平台服务费率，如 0.05 表示 5% */
    platformFeeRate: parseFeeRate(process.env.PAYMENT_PLATFORM_FEE_RATE),
    /** 单笔最低平台服务费（元），0 表示不限制 */
    platformFeeMin: parseMoney(process.env.PAYMENT_PLATFORM_FEE_MIN),
    /** 闲置交易自动确认收货天数，超时后自动分账给卖家 */
    autoConfirmDays: parseAutoConfirmDays(process.env.PAYMENT_AUTO_CONFIRM_DAYS),
  }),
);

interface IPaymentConfig {
  platformFeeRate: number;
  platformFeeMin: number;
  autoConfirmDays: number;
}

function parseFeeRate(value?: string): number {
  const parsed = Number.parseFloat(value ?? '0.05');
  if (!Number.isFinite(parsed) || parsed < 0) {
    return 0.05;
  }
  if (parsed > 0.5) {
    return 0.5;
  }
  return parsed;
}

function parseMoney(value?: string): number {
  const parsed = Number.parseFloat(value ?? '0');
  if (!Number.isFinite(parsed) || parsed < 0) {
    return 0;
  }
  return Math.round(parsed * 100) / 100;
}

function parseAutoConfirmDays(value?: string): number {
  const parsed = Number.parseInt(value ?? '7', 10);
  if (!Number.isFinite(parsed) || parsed < 1) {
    return 7;
  }
  if (parsed > 30) {
    return 30;
  }
  return parsed;
}

function normalizePem(value?: string): string {
  if (!value) {
    return '';
  }
  let normalized = value.trim();
  const quoted =
    (normalized.startsWith('"') && normalized.endsWith('"')) ||
    (normalized.startsWith("'") && normalized.endsWith("'"));
  if (quoted && normalized.length >= 2) {
    normalized = normalized.slice(1, -1);
  }
  return normalized
    .replace(/\\r/g, '')
    .replace(/\r/g, '')
    .replace(/\\n/g, '\n')
    .trim();
}

export {
  appConfiguration,
  mysqlConfiguration,
  redisConfiguration,
  alipayConfiguration,
  wechatPayConfiguration,
  paymentConfiguration,
};

export type {
  IAppConfig,
  IMysqlConfig,
  IRedisConfig,
  IAlipayConfig,
  IWechatPayConfig,
  IPaymentConfig,
};
