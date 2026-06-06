import { BadRequestException, Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import {
  isValidAlipayLoginId,
  maskAlipayLoginId,
  maskAlipayUserId,
  normalizeAlipayLoginId,
} from '@module/user/util/alipay-account.util';
import { AlipayService } from '@integration/alipay/service/alipay.service';
import { isAlipayLegacyUserId } from '@integration/alipay/util/alipay-receiver.util';

@Injectable()
export class UserAlipayService {
  private readonly logger = new Logger(UserAlipayService.name);

  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly alipayService: AlipayService,
  ) {}

  createOAuthAuthInfo(userId: number) {
    const targetId = `LFCAUTH${userId}${Date.now()}`;
    return this.alipayService.createAppAuthInfo(targetId);
  }

  async bindByAuthCode(userId: number, authCode: string) {
    const authCodeValue = authCode.trim();
    if (!authCodeValue) {
      throw new BadRequestException('授权码无效');
    }

    let oauthUser: { userId: string; nickName: string | null };
    try {
      oauthUser = await this.alipayService.exchangeOAuthUser(authCodeValue);
    } catch (error) {
      this.logger.warn(
        `支付宝授权换取用户信息失败。userId=${userId}, authCodePrefix=${authCodeValue.slice(0, 8)}, error=${String(error)}`,
      );
      throw error;
    }
    const user = await this.findUser(userId);
    user.alipayUserId = oauthUser.userId;
    user.alipayLoginId = null;
    // OAuth 只能拿到昵称，不能当作支付宝实名用于分账校验
    user.alipayRealName = null;
    user.alipayBoundAt = new Date();

    this.logger.log(
      `支付宝 OAuth 授权成功 userId=${userId}, accountType=${isAlipayLegacyUserId(oauthUser.userId) ? 'userId' : 'openId'}`,
    );

    await this.bindRoyaltyRelation(user);

    await this.userRepository.save(user);
    return this.toBindingDto(user);
  }

  async bindAccount(
    userId: number,
    alipayLoginId: string,
    alipayRealName?: string | null,
  ) {
    const loginId = normalizeAlipayLoginId(alipayLoginId);
    if (!isValidAlipayLoginId(loginId)) {
      throw new BadRequestException('请输入有效的支付宝手机号或邮箱');
    }
    const realName = alipayRealName?.trim() || null;
    if (!realName) {
      throw new BadRequestException('手动绑定支付宝时，请填写支付宝实名');
    }

    const user = await this.findUser(userId);
    user.alipayLoginId = loginId;
    user.alipayUserId = null;
    user.alipayRealName = realName;
    user.alipayBoundAt = new Date();

    await this.bindRoyaltyRelation(user);

    await this.userRepository.save(user);

    return this.toBindingDto(user);
  }

  async unbindAccount(userId: number) {
    const user = await this.findUser(userId);
    user.alipayLoginId = null;
    user.alipayUserId = null;
    user.alipayRealName = null;
    user.alipayBoundAt = null;
    user.alipayRoyaltyBoundAt = null;
    await this.userRepository.save(user);
    return this.toBindingDto(user);
  }

  async assertCanReceive(userId: number, roleLabel = '收款方') {
    const user = await this.findUser(userId);
    if (!user.alipayUserId && !user.alipayLoginId) {
      throw new BadRequestException(
        `${roleLabel}需先在设置中授权绑定支付宝收款账号`,
      );
    }
    if (!user.alipayRoyaltyBoundAt) {
      throw new BadRequestException(
        `${roleLabel}的支付宝分账关系未建立，请在设置中重新绑定收款账号`,
      );
    }
    return user;
  }

  toBindingDto(user: UserEntity) {
    return {
      alipayBound: Boolean(user.alipayUserId || user.alipayLoginId),
      alipayLoginIdMasked: this.maskBoundAccount(user),
      alipayRealName: user.alipayRealName,
      alipayBoundAt: user.alipayBoundAt,
      alipayRoyaltyBound: Boolean(user.alipayRoyaltyBoundAt),
      bindMethod: user.alipayUserId ? 'OAUTH' : user.alipayLoginId ? 'MANUAL' : null,
    };
  }

  private maskBoundAccount(user: UserEntity): string | null {
    if (user.alipayLoginId) {
      return maskAlipayLoginId(user.alipayLoginId);
    }
    if (user.alipayUserId) {
      return maskAlipayUserId(user.alipayUserId);
    }
    return null;
  }

  private async bindRoyaltyRelation(user: UserEntity) {
    const outRequestNo = `ROYALTY${user.id}${Date.now()}`;
    const royaltyRealName = user.alipayLoginId ? user.alipayRealName : null;
    try {
      await this.alipayService.bindRoyaltyRelation({
        outRequestNo,
        payeeUserId: user.alipayUserId,
        payeeLoginId: user.alipayLoginId,
        payeeRealName: royaltyRealName,
      });
      user.alipayRoyaltyBoundAt = new Date();
    } catch (error) {
      if (error instanceof BadRequestException) {
        const message = String(error.message);
        if (message.includes('已存在') || message.includes('重复')) {
          user.alipayRoyaltyBoundAt = user.alipayRoyaltyBoundAt ?? new Date();
          return;
        }
      }
      throw error;
    }
  }

  private async findUser(userId: number) {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) {
      throw new BadRequestException('用户不存在');
    }
    return user;
  }
}
