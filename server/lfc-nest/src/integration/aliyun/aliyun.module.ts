import { Global, Module } from '@nestjs/common';
import { ConsumerUploadController } from '@integration/aliyun/controller/consumer-upload.controller';
import { AliyunStorageService } from '@integration/aliyun/service/aliyun-storage.service';

@Global()
@Module({
  controllers: [ConsumerUploadController],
  providers: [AliyunStorageService],
  exports: [AliyunStorageService],
})
export class AliyunModule {}
