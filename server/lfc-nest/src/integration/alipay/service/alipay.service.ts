import {
  BadRequestException,
  Injectable,
  Logger,
  ServiceUnavailableException,
} from '@nestjs/common';
import { Inject } from '@nestjs/common';
import { alipayConfiguration } from '@config/configuration';
import type { IAlipayConfig } from '@config/configuration';
import {
  AlipayAppAuthInfoResult,
  AlipayAppPayResult,
  AlipayNotifyPayload,
  AlipayOAuthUserInfo,
} from '@integration/alipay/dto/alipay.dto';
import {
  buildAppAuthInfoString,
  buildAppPayOrderString,
  verifyAlipayNotify,
} from '@integration/alipay/util/alipay-crypto.util';
import { executeAlipayOpenApi } from '@integration/alipay/util/alipay-openapi.util';

export interface AlipayRoyaltySettleResult {
  outRequestNo: string;
  tradeNo: string;
  settleNo: string | null;
}

@Injectable()
export class AlipayService {
  private readonly logger = new Logger(AlipayService.name);

  constructor(
    @Inject(alipayConfiguration.KEY)
    private readonly alipayConfig: IAlipayConfig,
  ) {}

  isConfigured(): boolean {
    return Boolean(
      this.alipayConfig.appId &&
        this.alipayConfig.privateKey &&
        this.alipayConfig.alipayPublicKey &&
        this.alipayConfig.notifyUrl,
    );
  }

  isOAuthConfigured(): boolean {
    return this.isConfigured() && Boolean(this.alipayConfig.pid);
  }

  createAppAuthInfo(targetId: string): AlipayAppAuthInfoResult {
    this.assertOAuthConfigured();
    const authInfo = buildAppAuthInfoString({
      appId: this.alipayConfig.appId,
      pid: this.alipayConfig.pid,
      privateKey: this.alipayConfig.privateKey,
      targetId,
    });
    return { authInfo };
  }

  async exchangeOAuthUser(authCode: string): Promise<AlipayOAuthUserInfo> {
    this.assertOAuthConfigured();

    const tokenPayload = await executeAlipayOpenApi({
      gateway: this.alipayConfig.gateway,
      appId: this.alipayConfig.appId,
      privateKey: this.alipayConfig.privateKey,
      method: 'alipay.system.oauth.token',
      bizContent: {
        grant_type: 'authorization_code',
        code: authCode,
      },
    });

    const tokenCode = String(tokenPayload.code ?? '');
    if (tokenCode !== '10000') {
      const subMsg = String(tokenPayload.sub_msg ?? tokenPayload.msg ?? '授权失败');
      throw new BadRequestException(subMsg);
    }

    const accessToken = String(tokenPayload.access_token ?? '');
    const userId = String(tokenPayload.user_id ?? tokenPayload.open_id ?? '');
    if (!userId) {
      throw new BadRequestException('未获取到支付宝用户标识');
    }

    let nickName: string | null = null;
    if (accessToken) {
      try {
        const profilePayload = await executeAlipayOpenApi({
          gateway: this.alipayConfig.gateway,
          appId: this.alipayConfig.appId,
          privateKey: this.alipayConfig.privateKey,
          method: 'alipay.user.info.share',
          bizContent: {
            auth_token: accessToken,
          },
        });
        if (String(profilePayload.code ?? '') === '10000') {
          nickName = profilePayload.nick_name
            ? String(profilePayload.nick_name)
            : null;
        }
      } catch (error) {
        this.logger.warn(`查询支付宝用户信息失败: ${String(error)}`);
      }
    }

    return { userId, nickName };
  }

  async bindRoyaltyRelation(input: {
    outRequestNo: string;
    payeeUserId?: string | null;
    payeeLoginId?: string | null;
    payeeRealName?: string | null;
  }): Promise<void> {
    this.assertConfigured();

    const receiver: Record<string, string> = {
      memo: '莲峰校园C2C收款方',
    };
    if (input.payeeUserId) {
      receiver.type = 'userId';
      receiver.account = input.payeeUserId;
    } else if (input.payeeLoginId) {
      receiver.type = 'loginName';
      receiver.account = input.payeeLoginId;
    } else {
      throw new BadRequestException('缺少支付宝收款方信息');
    }
    if (input.payeeRealName?.trim()) {
      receiver.name = input.payeeRealName.trim();
    }

    const payload = await executeAlipayOpenApi({
      gateway: this.alipayConfig.gateway,
      appId: this.alipayConfig.appId,
      privateKey: this.alipayConfig.privateKey,
      method: 'alipay.trade.royalty.relation.bind',
      bizContent: {
        out_request_no: input.outRequestNo,
        receiver_list: [receiver],
      },
    });

    const code = String(payload.code ?? '');
    if (code !== '10000') {
      const subMsg = String(payload.sub_msg ?? payload.msg ?? '分账关系绑定失败');
      throw new BadRequestException(subMsg);
    }
  }

  async settleOrderRoyalty(input: {
    outRequestNo: string;
    tradeNo: string;
    payeeUserId?: string | null;
    payeeLoginId?: string | null;
    payeeRealName?: string | null;
    payeeAmount: string;
    desc: string;
  }): Promise<AlipayRoyaltySettleResult> {
    this.assertConfigured();

    const royaltyItem: Record<string, string> = {
      royalty_type: 'transfer',
      amount: input.payeeAmount,
      desc: input.desc.slice(0, 64),
    };
    if (input.payeeUserId) {
      royaltyItem.trans_in_type = 'userId';
      royaltyItem.trans_in = input.payeeUserId;
    } else if (input.payeeLoginId) {
      royaltyItem.trans_in_type = 'loginName';
      royaltyItem.trans_in = input.payeeLoginId;
    } else {
      throw new BadRequestException('缺少支付宝收款方信息');
    }
    if (input.payeeRealName?.trim()) {
      royaltyItem.trans_in_name = input.payeeRealName.trim();
    }

    const payload = await executeAlipayOpenApi({
      gateway: this.alipayConfig.gateway,
      appId: this.alipayConfig.appId,
      privateKey: this.alipayConfig.privateKey,
      method: 'alipay.trade.order.settle',
      bizContent: {
        out_request_no: input.outRequestNo,
        trade_no: input.tradeNo,
        royalty_parameters: [royaltyItem],
        royalty_finish: 'true',
      },
    });

    const code = String(payload.code ?? '');
    if (code !== '10000') {
      const subMsg = String(payload.sub_msg ?? payload.msg ?? '分账失败');
      throw new BadRequestException(subMsg);
    }

    return {
      outRequestNo: input.outRequestNo,
      tradeNo: input.tradeNo,
      settleNo: payload.settle_no ? String(payload.settle_no) : null,
    };
  }

  createAppPayOrder(input: {
    outTradeNo: string;
    totalAmount: string;
    subject: string;
    enableRoyalty?: boolean;
  }): AlipayAppPayResult {
    this.assertConfigured();
    const orderStr = buildAppPayOrderString({
      appId: this.alipayConfig.appId,
      privateKey: this.alipayConfig.privateKey,
      notifyUrl: this.alipayConfig.notifyUrl,
      outTradeNo: input.outTradeNo,
      totalAmount: input.totalAmount,
      subject: input.subject,
      enableRoyalty: input.enableRoyalty,
    });
    return { orderStr };
  }

  verifyNotify(payload: AlipayNotifyPayload): boolean {
    this.assertConfigured();
    try {
      return verifyAlipayNotify(
        payload as Record<string, string>,
        this.alipayConfig.alipayPublicKey,
      );
    } catch (error) {
      this.logger.warn(`支付宝回调验签失败: ${String(error)}`);
      return false;
    }
  }

  assertAmountMatches(expected: string, actual?: string) {
    if (!actual) {
      throw new BadRequestException('支付宝回调缺少金额');
    }
    if (
      Number.parseFloat(actual).toFixed(2) !==
      Number.parseFloat(expected).toFixed(2)
    ) {
      throw new BadRequestException('支付宝回调金额不匹配');
    }
  }

  private assertConfigured() {
    if (!this.isConfigured()) {
      throw new ServiceUnavailableException('支付宝支付未配置');
    }
  }

  private assertOAuthConfigured() {
    if (!this.isOAuthConfigured()) {
      throw new ServiceUnavailableException(
        '支付宝授权未配置，请设置 ALIPAY_APP_ID、ALIPAY_PID 及密钥',
      );
    }
  }
}
