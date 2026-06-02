declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: 'development' | 'production' | 'test';
    HOST: string;
    PORT: string;

    MYSQL_HOST: string;
    MYSQL_PORT: string;
    MYSQL_USERNAME: string;
    MYSQL_PASSWORD: string;
    MYSQL_DATABASE: string;
    MYSQL_SYNCHRONIZE?: string;

    REDIS_PROTOCOL: string;
    REDIS_HOST: string;
    REDIS_PORT: string;
    REDIS_PASSWORD?: string;
    REDIS_DB: string;

    JWT_SECRET: string;
    JWT_ACCESS_EXPIRES_IN?: string;
    JWT_REFRESH_EXPIRES_IN?: string;

    ADMIN_EMAIL?: string;
    ADMIN_PASSWORD?: string;
  }
}
