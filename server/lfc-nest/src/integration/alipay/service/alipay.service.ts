import {
  BadRequestException,
  Injectable,
  Logger,
  ServiceUnavailableException,
} from '@nestjs/common';
import { Inject } from '@nestjs/common';
import crypto from 'crypto';
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
import { resolveAlipayReceiver } from '@integration/alipay/util/alipay-receiver.util';

export interface AlipayRoyaltySettleResult {
  outRequestNo: string;
  tradeNo: string;
  settleNo: string | null;
}

export interface AlipayRefundRoyaltyParam {
  payeeUserId?: string | null;
  payeeLoginId?: string | null;
  amount: string;
}

export interface AlipayRefundResult {
  outRequestNo: string;
  tradeNo: string | null;
  refundAmount: string;
}

@Injectable()
export class AlipayService {
  private readonly logger = new Logger(AlipayService.name);

  constructor(
    @Inject(alipayConfiguration.KEY)
    private readonly alipayConfig: IAlipayConfig,
  ) {
    this.logBootstrapConfig();
  }

  isConfigured(): boolean {
    return this.getMissingPaymentConfigKeys().length === 0;
  }

  isOAuthConfigured(): boolean {
    return Boolean(
      this.alipayConfig.appId &&
        this.alipayConfig.privateKey &&
        this.alipayConfig.pid,
    );
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
      extraParams: {
        grant_type: 'authorization_code',
        code: authCode,
      },
    });

    const tokenCode = String(tokenPayload.code ?? '');
    const accessToken = String(tokenPayload.access_token ?? '');
    const userId = String(tokenPayload.user_id ?? '').trim();
    const openId = String(tokenPayload.open_id ?? '').trim();
    const accountId = userId || openId;
    const tokenLooksSuccessful = accountId.length > 0;
    if (tokenCode.trim().length > 0 && tokenCode !== '10000') {
      const subCode = String(tokenPayload.sub_code ?? '');
      const msg = String(tokenPayload.msg ?? '');
      const subMsg = String(tokenPayload.sub_msg ?? '');
      const detail = [tokenCode, subCode, msg, subMsg]
        .filter((item) => item)
        .join(' | ');
      throw new BadRequestException(
        detail || '授权失败',
      );
    }
    if (!tokenLooksSuccessful) {
      const payloadKeys = Object.keys(tokenPayload).join(',');
      throw new BadRequestException(
        `授权失败：未获取到支付宝用户标识（payloadKeys=${payloadKeys}）`,
      );
    }

    let nickName: string | null = null;
    if (accessToken) {
      try {
        const profilePayload = await executeAlipayOpenApi({
          gateway: this.alipayConfig.gateway,
          appId: this.alipayConfig.appId,
          privateKey: this.alipayConfig.privateKey,
          method: 'alipay.user.info.share',
          authToken: accessToken,
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

    return { userId: accountId, nickName };
  }

  private buildRoyaltyReceiver(input: {
    payeeUserId?: string | null;
    payeeLoginId?: string | null;
    payeeRealName?: string | null;
  }): Record<string, string> {
    const resolved = resolveAlipayReceiver(input);
    if (!resolved) {
      throw new BadRequestException('缺少支付宝收款方信息');
    }

    const receiver: Record<string, string> = {
      type: resolved.type,
      account: resolved.account,
      memo: '莲峰校园C2C收款方',
    };
    // userId / openId 绑定分账时不要传 name（OAuth 只能拿到昵称，会触发「全称不匹配」）
    if (resolved.type === 'loginName') {
      const realName = input.payeeRealName?.trim();
      if (!realName) {
        throw new BadRequestException('手动绑定支付宝时，请填写支付宝实名');
      }
      receiver.name = realName;
    }
    return receiver;
  }

  private buildRoyaltyTransferTarget(input: {
    payeeUserId?: string | null;
    payeeLoginId?: string | null;
    payeeRealName?: string | null;
  }): Record<string, string> {
    const resolved = resolveAlipayReceiver(input);
    if (!resolved) {
      throw new BadRequestException('缺少支付宝收款方信息');
    }

    const target: Record<string, string> = {};
    if (resolved.type === 'userId') {
      target.trans_in_type = 'userId';
      target.trans_in = resolved.account;
    } else if (resolved.type === 'openId') {
      target.trans_in_type = 'openId';
      target.trans_in = resolved.account;
    } else {
      target.trans_in_type = 'loginName';
      target.trans_in = resolved.account;
    }
    if (resolved.type === 'loginName' && input.payeeRealName?.trim()) {
      target.trans_in_name = input.payeeRealName.trim();
    }
    return target;
  }

  private buildRoyaltyTransferSource(input: {
    payeeUserId?: string | null;
    payeeLoginId?: string | null;
  }): Record<string, string> {
    const resolved = resolveAlipayReceiver(input);
    if (!resolved) {
      throw new BadRequestException('缺少分账回退收款方信息');
    }

    if (resolved.type === 'userId') {
      return { trans_out_type: 'userId', trans_out: resolved.account };
    }
    if (resolved.type === 'openId') {
      return { trans_out_type: 'openId', trans_out: resolved.account };
    }
    return { trans_out_type: 'loginName', trans_out: resolved.account };
  }

  async bindRoyaltyRelation(input: {
    outRequestNo: string;
    payeeUserId?: string | null;
    payeeLoginId?: string | null;
    payeeRealName?: string | null;
  }): Promise<void> {
    this.assertConfigured();

    const receiver = this.buildRoyaltyReceiver(input);

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
      const subCode = String(payload.sub_code ?? '');
      const subMsg = String(payload.sub_msg ?? payload.msg ?? '分账关系绑定失败');
      if (subCode === 'USER_NOT_EXIST' || subMsg.includes('分账接收方不存在')) {
        throw new BadRequestException(
          '分账接收方不存在：请确认你的支付宝账号已完成实名认证，然后在设置中重新授权绑定',
        );
      }
      if (subCode === 'USERNAME_NOT_MATCH' || subMsg.includes('全称不匹配')) {
        throw new BadRequestException(
          '分账接收方全称不匹配：请重新授权绑定，无需填写额外信息',
        );
      }
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
      ...this.buildRoyaltyTransferTarget(input),
    };

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

  async refundTrade(input: {
    outTradeNo: string;
    tradeNo?: string | null;
    refundAmount: string;
    outRequestNo: string;
    refundReason?: string;
    royaltyReturn?: AlipayRefundRoyaltyParam | null;
  }): Promise<AlipayRefundResult> {
    this.assertConfigured();

    const bizContent: Record<string, unknown> = {
      out_trade_no: input.outTradeNo,
      refund_amount: input.refundAmount,
      out_request_no: input.outRequestNo,
    };
    if (input.tradeNo) {
      bizContent.trade_no = input.tradeNo;
    }
    if (input.refundReason?.trim()) {
      bizContent.refund_reason = input.refundReason.trim().slice(0, 256);
    }
    if (input.royaltyReturn) {
      const royaltyItem: Record<string, string> = {
        royalty_type: 'transfer',
        amount: input.royaltyReturn.amount,
        desc: '退款分账回退'.slice(0, 64),
        ...this.buildRoyaltyTransferSource(input.royaltyReturn),
      };
      bizContent.refund_royalty_parameters = [royaltyItem];
    }

    const payload = await executeAlipayOpenApi({
      gateway: this.alipayConfig.gateway,
      appId: this.alipayConfig.appId,
      privateKey: this.alipayConfig.privateKey,
      method: 'alipay.trade.refund',
      bizContent,
    });

    const code = String(payload.code ?? '');
    if (code !== '10000') {
      const subMsg = String(payload.sub_msg ?? payload.msg ?? '退款失败');
      throw new BadRequestException(subMsg);
    }

    return {
      outRequestNo: input.outRequestNo,
      tradeNo: payload.trade_no ? String(payload.trade_no) : input.tradeNo ?? null,
      refundAmount: input.refundAmount,
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
    const missing = this.getMissingPaymentConfigKeys();
    if (missing.length > 0) {
      throw new ServiceUnavailableException(
        `支付宝支付未配置，请设置 ${missing.join('、')}`,
      );
    }
  }

  private assertOAuthConfigured() {
    if (!this.isOAuthConfigured()) {
      throw new ServiceUnavailableException(
        '支付宝授权未配置，请设置 ALIPAY_APP_ID、ALIPAY_PID、ALIPAY_PRIVATE_KEY',
      );
    }
  }

  private logBootstrapConfig() {
    const appId = this.alipayConfig.appId?.trim() || '(empty)';
    const pid = this.alipayConfig.pid?.trim() || '(empty)';
    const gateway = this.alipayConfig.gateway?.trim() || '(empty)';
    const privateKey = this.alipayConfig.privateKey?.trim() || '';
    const keyFingerprint = privateKey
      ? crypto
          .createHash('sha256')
          .update(privateKey.replace(/\\n/g, '\n'))
          .digest('hex')
          .slice(0, 16)
      : '(empty)';
    const alipayPublicKeyConfigured = Boolean(
      this.alipayConfig.alipayPublicKey?.trim(),
    );
    const notifyUrlConfigured = Boolean(this.alipayConfig.notifyUrl?.trim());
    this.logger.log(
      `Alipay config loaded appId=${appId}, pid=${pid}, gateway=${gateway}, privateKeySha256=${keyFingerprint}, alipayPublicKeyConfigured=${alipayPublicKeyConfigured}, notifyUrlConfigured=${notifyUrlConfigured}`,
    );
  }

  private getMissingPaymentConfigKeys(): string[] {
    const missing: string[] = [];
    if (!this.alipayConfig.appId?.trim()) {
      missing.push('ALIPAY_APP_ID');
    }
    if (!this.alipayConfig.privateKey?.trim()) {
      missing.push('ALIPAY_PRIVATE_KEY');
    }
    if (!this.alipayConfig.alipayPublicKey?.trim()) {
      missing.push('ALIPAY_ALIPAY_PUBLIC_KEY');
    }
    if (!this.alipayConfig.notifyUrl?.trim()) {
      missing.push('ALIPAY_NOTIFY_URL');
    }
    return missing;
  }
}
