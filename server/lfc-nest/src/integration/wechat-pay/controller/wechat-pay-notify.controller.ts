import { Controller, Headers, Post, Req, Res } from '@nestjs/common';
import type { RawBodyRequest } from '@nestjs/common';
import type { Request, Response } from 'express';
import { WechatPayNotifyService } from '@integration/wechat-pay/service/wechat-pay-notify.service';

@Controller('wechat-pay')
export class WechatPayNotifyController {
  constructor(
    private readonly wechatPayNotifyService: WechatPayNotifyService,
  ) {}

  @Post('notify')
  async notify(
    @Req() req: RawBodyRequest<Request>,
    @Headers() headers: Record<string, string>,
    @Res() res: Response,
  ) {
    const body =
      typeof req.rawBody === 'string'
        ? req.rawBody
        : req.rawBody?.toString('utf8') ?? '';
    const ok = await this.wechatPayNotifyService.handleNotify(headers, body);
    res.status(ok ? 200 : 400).json({
      code: ok ? 'SUCCESS' : 'FAIL',
      message: ok ? '成功' : '失败',
    });
  }
}
