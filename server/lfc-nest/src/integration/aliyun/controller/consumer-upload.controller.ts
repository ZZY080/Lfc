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
import { assertImageUploadFile, assertVideoUploadFile } from '@integration/aliyun/util/oss-file.util';
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
    @Query('scope') scope?: 'post' | 'activity' | 'profile' | 'chat',
  ) {
    assertImageUploadFile(file, '图片');
    const prefix =
      scope === 'activity'
        ? 'activities'
        : scope === 'profile'
          ? 'profiles'
          : scope === 'chat'
            ? 'chat'
          : 'posts';
    const url = await this.aliyunStorageService.uploadFile(
      AliyunBucket.PUBLIC,
      prefix,
      file,
    );
    return { url };
  }

  @Post('video')
  @UseGuards(JwtAuthGuard)
  @UseInterceptors(
    FileInterceptor('file', {
      limits: { fileSize: 50 * 1024 * 1024 },
    }),
  )
  async uploadVideo(
    @UploadedFile() file: Express.Multer.File,
    @Query('scope') scope?: 'chat',
  ) {
    assertVideoUploadFile(file, '视频');
    const prefix = scope === 'chat' ? 'chat' : 'chat';
    const url = await this.aliyunStorageService.uploadFile(
      AliyunBucket.PUBLIC,
      prefix,
      file,
    );
    return { url };
  }
}
