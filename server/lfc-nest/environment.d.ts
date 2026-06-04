declare namespace NodeJS {
  interface ProcessEnv {
    /** 运行环境：development | production | test */
    NODE_ENV: 'development' | 'production' | 'test';
    /** HTTP 服务监听地址 */
    HOST: string;
    /** HTTP 服务端口 */
    PORT: string;

    /** MySQL 主机 */
    MYSQL_HOST: string;
    /** MySQL 端口 */
    MYSQL_PORT: string;
    /** MySQL 用户名 */
    MYSQL_USERNAME: string;
    /** MySQL 密码 */
    MYSQL_PASSWORD: string;
    /** MySQL 数据库名 */
    MYSQL_DATABASE: string;
    /** 是否自动同步表结构，生产建议 false */
    MYSQL_SYNCHRONIZE?: string;

    /** Redis 连接协议 */
    REDIS_PROTOCOL: string;
    /** Redis 主机 */
    REDIS_HOST: string;
    /** Redis 端口 */
    REDIS_PORT: string;
    /** Redis 密码 */
    REDIS_PASSWORD?: string;
    /** Redis 数据库编号 */
    REDIS_DB: string;

    /** JWT 签名密钥 */
    JWT_SECRET: string;
    /** Access Token 有效期 */
    JWT_ACCESS_EXPIRES_IN?: string;
    /** Refresh Token 有效期 */
    JWT_REFRESH_EXPIRES_IN?: string;

    /** 管理员种子账号邮箱 */
    ADMIN_EMAIL?: string;
    /** 管理员种子账号密码 */
    ADMIN_PASSWORD?: string;

    /** 阿里云 AccessKey ID */
    ALIYUN_ACCESS_KEY_ID?: string;
    /** 阿里云 AccessKey Secret */
    ALIYUN_ACCESS_KEY_SECRET?: string;
    /** OSS Region */
    ALIYUN_OSS_REGION?: string;
    /** OSS Endpoint */
    ALIYUN_OSS_ENDPOINT?: string;
    /** 公开读 OSS Bucket */
    ALIYUN_OSS_BUCKET_PUBLIC?: string;
    /** 私有 OSS Bucket */
    ALIYUN_OSS_BUCKET_PRIVATE?: string;
    /** 处理中 OSS Bucket */
    ALIYUN_OSS_BUCKET_PROCESSING?: string;
    /** STS 临时上传角色 ARN */
    ALIYUN_OSS_ROLE_ARN?: string;

    /** 阿里云邮件 API Endpoint */
    ALIYUN_MAIL_ENDPOINT?: string;
    /** 发信地址 */
    ALIYUN_MAIL_ACCOUNT_NAME?: string;
    /** 是否使用回信地址 */
    ALIYUN_MAIL_REPLY_TO_ADDRESS?: string;
    /** 默认收信地址 */
    ALIYUN_MAIL_TO_ADDRESS?: string;

  /** 支付宝开放平台 AppID */
  ALIPAY_APP_ID?: string;
  /** 应用私钥（RSA2） */
  ALIPAY_PRIVATE_KEY?: string;
  /** 支付宝公钥 */
  ALIPAY_ALIPAY_PUBLIC_KEY?: string;
  /** 支付宝网关地址 */
  ALIPAY_GATEWAY?: string;
  /** 支付异步通知 URL */
  ALIPAY_NOTIFY_URL?: string;
  /** 商户 PID（App 支付宝授权登录必填） */
  ALIPAY_PID?: string;

    /** 微信 AppID */
    WECHAT_PAY_APP_ID?: string;
    /** 微信商户号 */
    WECHAT_PAY_MCH_ID?: string;
    /** 微信 API v3 密钥 */
    WECHAT_PAY_API_V3_KEY?: string;
    /** 微信商户 API 私钥 */
    WECHAT_PAY_PRIVATE_KEY?: string;
    /** 微信商户证书序列号 */
    WECHAT_PAY_SERIAL_NO?: string;
    /** 微信支付回调 URL */
    WECHAT_PAY_NOTIFY_URL?: string;

    /** C2C 平台服务费率，如 0.05 表示 5% */
    PAYMENT_PLATFORM_FEE_RATE?: string;
    /** 单笔最低平台服务费（元） */
    PAYMENT_PLATFORM_FEE_MIN?: string;
    /** 闲置交易自动确认收货天数，默认 7 */
    PAYMENT_AUTO_CONFIRM_DAYS?: string;
  }
}
