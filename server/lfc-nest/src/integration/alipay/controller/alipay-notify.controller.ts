import { Body, Controller, Post, Res } from '@nestjs/common';
import type { Response } from 'express';
import { AlipayNotifyService } from '@integration/alipay/service/alipay-notify.service';

@Controller('alipay')
export class AlipayNotifyController {
  constructor(private readonly alipayNotifyService: AlipayNotifyService) {}

  @Post('notify')
  async notify(
    @Body() body: Record<string, string>,
    @Res() res: Response,
  ) {
    const ok = await this.alipayNotifyService.handleNotify(body);
    res.type('text/plain').send(ok ? 'success' : 'failure');
  }
}
