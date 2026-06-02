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

export { appConfiguration, mysqlConfiguration, redisConfiguration };

export type { IAppConfig, IMysqlConfig, IRedisConfig };
