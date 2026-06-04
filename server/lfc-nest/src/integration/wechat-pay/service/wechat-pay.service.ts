import {
  Injectable,
  Logger,
  ServiceUnavailableException,
} from '@nestjs/common';
import { Inject } from '@nestjs/common';
import { wechatPayConfiguration } from '@config/configuration';
import type { IWechatPayConfig } from '@config/configuration';
import { WechatAppPayResult } from '@integration/wechat-pay/dto/wechat-pay.dto';

@Injectable()
export class WechatPayService {
  private readonly logger = new Logger(WechatPayService.name);

  constructor(
    @Inject(wechatPayConfiguration.KEY)
    private readonly wechatPayConfig: IWechatPayConfig,
  ) {}

  isConfigured(): boolean {
    return Boolean(
      this.wechatPayConfig.appId &&
        this.wechatPayConfig.mchId &&
        this.wechatPayConfig.apiV3Key &&
        this.wechatPayConfig.privateKey &&
        this.wechatPayConfig.serialNo &&
        this.wechatPayConfig.notifyUrl,
    );
  }

  createAppPayOrder(input: {
    outTradeNo: string;
    totalAmount: string;
    description: string;
  }): WechatAppPayResult {
    this.assertConfigured();
    this.logger.warn(
      `微信支付 App 下单尚未实现: outTradeNo=${input.outTradeNo}`,
    );
    throw new ServiceUnavailableException('微信支付 App 下单暂未实现');
  }

  verifyNotify(_headers: Record<string, string>, _body: string): boolean {
    this.assertConfigured();
    return false;
  }

  private assertConfigured() {
    if (!this.isConfigured()) {
      throw new ServiceUnavailableException('微信支付未配置');
    }
  }
}
