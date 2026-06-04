import { BadRequestException } from '@nestjs/common';
import { getAlipaySignContent, rsaSign } from '@integration/alipay/util/alipay-crypto.util';

function formatTimestamp(date: Date): string {
  const pad = (value: number) => value.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export async function executeAlipayOpenApi(input: {
  gateway: string;
  appId: string;
  privateKey: string;
  method: string;
  bizContent?: Record<string, unknown>;
  extraParams?: Record<string, string>;
  authToken?: string;
}): Promise<Record<string, unknown>> {
  const params: Record<string, string> = {
    app_id: input.appId,
    method: input.method,
    format: 'JSON',
    charset: 'utf-8',
    sign_type: 'RSA2',
    timestamp: formatTimestamp(new Date()),
    version: '1.0',
  };
  if (input.authToken) {
    params.auth_token = input.authToken;
  }
  if (input.extraParams) {
    Object.assign(params, input.extraParams);
  }
  if (input.bizContent) {
    params.biz_content = JSON.stringify(input.bizContent);
  }
  params.sign = rsaSign(getAlipaySignContent(params), input.privateKey);

  const body = Object.keys(params)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');

  const response = await fetch(input.gateway, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
    body,
  });

  const text = await response.text();
  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(text) as Record<string, unknown>;
  } catch {
    throw new BadRequestException(`支付宝接口响应异常: ${text.slice(0, 200)}`);
  }

  const responseKey = `${input.method.replace(/\./g, '_')}_response`;
  const payload = parsed[responseKey] as Record<string, unknown> | undefined;
  if (!payload) {
    const errorPayload = parsed.error_response as
      | Record<string, unknown>
      | undefined;
    if (errorPayload) {
      const code = String(errorPayload.code ?? '');
      const subCode = String(errorPayload.sub_code ?? '');
      const msg = String(errorPayload.msg ?? '');
      const subMsg = String(errorPayload.sub_msg ?? '');
      const detail = [code, subCode, msg, subMsg].filter((item) => item).join(" | ");
      throw new BadRequestException(
        `支付宝接口调用失败${detail ? `: ${detail}` : ''}`,
      );
    }
    throw new BadRequestException(`支付宝接口缺少响应体: ${text.slice(0, 200)}`);
  }
  return payload;
}
