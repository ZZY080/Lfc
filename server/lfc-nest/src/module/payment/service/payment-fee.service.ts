import { Injectable } from '@nestjs/common';
import { Inject } from '@nestjs/common';
import { paymentConfiguration } from '@config/configuration';
import type { IPaymentConfig } from '@config/configuration';
import {
  calculatePlatformSettlement,
  formatFeeRateLabel,
} from '@module/payment/util/payment-fee.util';

@Injectable()
export class PaymentFeeService {
  constructor(
    @Inject(paymentConfiguration.KEY)
    private readonly paymentConfig: IPaymentConfig,
  ) {}

  calculateSettlement(totalAmount: string) {
    return calculatePlatformSettlement(
      totalAmount,
      this.paymentConfig.platformFeeRate,
      this.paymentConfig.platformFeeMin,
    );
  }

  getPublicConfig() {
    return {
      platformFeeRate: this.paymentConfig.platformFeeRate,
      platformFeeRateLabel: formatFeeRateLabel(
        this.paymentConfig.platformFeeRate,
      ),
      platformFeeMin: this.paymentConfig.platformFeeMin,
      autoConfirmDays: this.paymentConfig.autoConfirmDays,
    };
  }
}
