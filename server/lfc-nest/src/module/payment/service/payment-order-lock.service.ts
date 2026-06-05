import { ConflictException, Injectable } from '@nestjs/common';
import { RedisService } from '@integration/redis/service/redis.service';
import {
  PAYMENT_CREATE_LOCK_TTL_SECONDS,
  PAYMENT_NOTIFY_PROCESS_LOCK_TTL_SECONDS,
  PAYMENT_PRODUCT_LOCK_TTL_SECONDS,
} from '@module/payment/util/payment-order.util';

@Injectable()
export class PaymentOrderLockService {
  constructor(private readonly redisService: RedisService) {}

  async withCreateLock<T>(scopeKey: string, handler: () => Promise<T>): Promise<T> {
    const lockKey = `payment:lock:create:${scopeKey}`;
    const token = await this.redisService.acquireLock(
      lockKey,
      PAYMENT_CREATE_LOCK_TTL_SECONDS,
    );
    if (!token) {
      throw new ConflictException('请勿重复提交，请稍后再试');
    }
    try {
      return await handler();
    } finally {
      await this.redisService.releaseLock(lockKey, token);
    }
  }

  async withNotifyLock<T>(
    outTradeNo: string,
    handler: () => Promise<T>,
  ): Promise<T | null> {
    const lockKey = `payment:lock:notify:${outTradeNo}`;
    const token = await this.redisService.acquireLock(
      lockKey,
      PAYMENT_NOTIFY_PROCESS_LOCK_TTL_SECONDS,
    );
    if (!token) {
      return null;
    }
    try {
      return await handler();
    } finally {
      await this.redisService.releaseLock(lockKey, token);
    }
  }

  async tryAcquireProductLock(postId: number, userId: number): Promise<boolean> {
    return this.redisService.setNx(
      this.productLockKey(postId),
      String(userId),
      PAYMENT_PRODUCT_LOCK_TTL_SECONDS,
    );
  }

  async refreshProductLock(postId: number, userId: number): Promise<void> {
    const key = this.productLockKey(postId);
    const holder = await this.redisService.get(key);
    if (holder === String(userId)) {
      await this.redisService.expire(key, PAYMENT_PRODUCT_LOCK_TTL_SECONDS);
    }
  }

  async releaseProductLock(postId: number, userId: number): Promise<void> {
    const key = this.productLockKey(postId);
    const holder = await this.redisService.get(key);
    if (holder === String(userId)) {
      await this.redisService.del(key);
    }
  }

  async assertProductAvailableForUser(postId: number, userId: number): Promise<void> {
    const holder = await this.redisService.get(this.productLockKey(postId));
    if (holder && holder !== String(userId)) {
      throw new ConflictException('其他用户正在购买，请稍后再试');
    }
  }

  private productLockKey(postId: number): string {
    return `payment:lock:product:${postId}`;
  }
}
