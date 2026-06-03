import {
  Injectable,
  InternalServerErrorException,
  Logger,
  ServiceUnavailableException,
} from '@nestjs/common';
import { randomBytes } from 'node:crypto';
import OSS from 'ali-oss';
import path from 'path';
import { AliyunBucket } from '@integration/aliyun/enum/aliyun.enum';

@Injectable()
export class AliyunStorageService {
  private readonly logger = new Logger(AliyunStorageService.name);

  private readonly bucketMapping: Record<AliyunBucket, string> = {
    [AliyunBucket.PUBLIC]: process.env.ALIYUN_OSS_BUCKET_PUBLIC || '',
    [AliyunBucket.PRIVATE]: process.env.ALIYUN_OSS_BUCKET_PRIVATE || '',
    [AliyunBucket.PROCESSING]: process.env.ALIYUN_OSS_BUCKET_PROCESSING || '',
  };

  isConfigured(): boolean {
    return Boolean(
      process.env.ALIYUN_ACCESS_KEY_ID &&
        process.env.ALIYUN_ACCESS_KEY_SECRET &&
        process.env.ALIYUN_OSS_REGION &&
        this.bucketMapping[AliyunBucket.PUBLIC],
    );
  }

  makeObjectKey(prefix: string, originalName: string): string {
    const ext = path.extname(originalName || '').toLowerCase() || '.jpg';
    const slug = randomBytes(8).toString('hex');
    return `${prefix.replace(/\/$/, '')}/${Date.now()}-${slug}${ext}`;
  }

  async uploadFile(
    bucket: AliyunBucket,
    prefix: string,
    file: Express.Multer.File,
  ): Promise<string> {
    const objectKey = this.makeObjectKey(prefix, file.originalname);
    const mime = file.mimetype?.toLowerCase() || undefined;
    return this.putObject(bucket, objectKey, file.buffer, mime);
  }

  async putObject(
    bucket: AliyunBucket,
    objectKey: string,
    buffer: Buffer,
    contentType?: string,
  ): Promise<string> {
    if (!this.isConfigured()) {
      throw new ServiceUnavailableException('OSS 未配置，请联系管理员');
    }
    const bucketName = this.bucketMapping[bucket];
    if (!bucketName) {
      throw new ServiceUnavailableException(`OSS Bucket ${bucket} 未配置`);
    }
    try {
      const client = this.getClient(bucket);
      await client.put(objectKey, buffer, {
        headers: contentType ? { 'Content-Type': contentType } : {},
      });
      return this.buildObjectUrl(bucketName, objectKey);
    } catch (error) {
      this.logger.error(error);
      throw new InternalServerErrorException('OSS 上传失败');
    }
  }

  private buildObjectUrl(bucketName: string, objectKey: string): string {
    const region = process.env.ALIYUN_OSS_REGION!;
    return `https://${bucketName}.${region}.aliyuncs.com/${objectKey.replace(/^\//, '')}`;
  }

  private getClient(bucket: AliyunBucket): OSS {
    return new OSS({
      bucket: this.bucketMapping[bucket],
      accessKeyId: process.env.ALIYUN_ACCESS_KEY_ID || '',
      accessKeySecret: process.env.ALIYUN_ACCESS_KEY_SECRET || '',
      endpoint: process.env.ALIYUN_OSS_ENDPOINT || undefined,
      region: process.env.ALIYUN_OSS_REGION || '',
    });
  }
}
