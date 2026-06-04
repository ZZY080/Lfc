import { Injectable, Logger, OnModuleDestroy, OnModuleInit } from '@nestjs/common';
import { PaymentOrderService } from '@module/payment/service/payment-order.service';

@Injectable()
export class PaymentAutoConfirmService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(PaymentAutoConfirmService.name);
  private timer: NodeJS.Timeout | null = null;

  constructor(private readonly paymentOrderService: PaymentOrderService) {}

  onModuleInit() {
    this.timer = setInterval(() => {
      void this.runAutoConfirm();
    }, 60 * 60 * 1000);
    void this.runAutoConfirm();
  }

  onModuleDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  private async runAutoConfirm() {
    try {
      const count = await this.paymentOrderService.processAutoConfirmOrders();
      if (count > 0) {
        this.logger.log(`自动确认收货并完成分账 ${count} 笔`);
      }
    } catch (error) {
      this.logger.warn(`自动确认收货任务失败: ${String(error)}`);
    }
  }
}
