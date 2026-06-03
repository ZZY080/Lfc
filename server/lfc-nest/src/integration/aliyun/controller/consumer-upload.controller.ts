import {
  Controller,
  Post,
  Query,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { AliyunBucket } from '@integration/aliyun/enum/aliyun.enum';
import { AliyunStorageService } from '@integration/aliyun/service/aliyun-storage.service';
import { assertImageUploadFile } from '@integration/aliyun/util/oss-file.util';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';

@Controller('consumer/upload')
export class ConsumerUploadController {
  constructor(private readonly aliyunStorageService: AliyunStorageService) {}

  @Post('image')
  @UseGuards(JwtAuthGuard)
  @UseInterceptors(
    FileInterceptor('file', {
      limits: { fileSize: 10 * 1024 * 1024 },
    }),
  )
  async uploadImage(
    @UploadedFile() file: Express.Multer.File,
    @Query('scope') scope?: 'post' | 'activity' | 'profile',
  ) {
    assertImageUploadFile(file, '图片');
    const prefix =
      scope === 'activity'
        ? 'activities'
        : scope === 'profile'
          ? 'profiles'
          : 'posts';
    const url = await this.aliyunStorageService.uploadFile(
      AliyunBucket.PUBLIC,
      prefix,
      file,
    );
    return { url };
  }
}
