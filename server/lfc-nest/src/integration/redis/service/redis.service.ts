import { redisConfiguration } from '@config/configuration';
import type { IRedisConfig } from '@config/configuration';
import {
  Injectable,
  OnModuleInit,
  OnModuleDestroy,
  Inject,
} from '@nestjs/common';
import Redis from 'ioredis';

@Injectable()
export class RedisService implements OnModuleInit, OnModuleDestroy {
  private client: Redis;
  private readyPromise: Promise<void>;

  constructor(
    @Inject(redisConfiguration.KEY)
    private readonly redisConfig: IRedisConfig,
  ) {}

  onModuleInit() {
    this.client = new Redis({
      host: this.redisConfig.host,
      port: Number(this.redisConfig.port),
      password: this.redisConfig.password || undefined,
      db: this.redisConfig.db,
      maxRetriesPerRequest: null,
      enableOfflineQueue: true,
    });

    this.readyPromise = new Promise((resolve, reject) => {
      this.client.once('connect', () => {
        console.log('Redis连接成功');
        resolve();
      });
      this.client.once('error', (err) => {
        console.error('Redis连接失败', err);
        reject(err);
      });
    });

    return this.readyPromise;
  }

  async waitReady() {
    return this.readyPromise;
  }

  onModuleDestroy() {
    return this.client.quit();
  }

  async set(key: string, value: string, ttlSeconds?: number) {
    if (ttlSeconds) {
      await this.client.set(key, value, 'EX', ttlSeconds);
    } else {
      await this.client.set(key, value);
    }
  }

  async get(key: string): Promise<string | null> {
    return this.client.get(key);
  }

  async del(key: string): Promise<number> {
    return this.client.del(key);
  }

  async incr(key: string): Promise<number> {
    return this.client.incr(key);
  }

  async expire(key: string, ttlSeconds: number): Promise<void> {
    await this.client.expire(key, ttlSeconds);
  }

  getClient(): Redis {
    return this.client;
  }
}
