import {
  Body,
  Controller,
  Post,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { AliyunBucket } from '@integration/aliyun/enum/aliyun.enum';
import { AliyunStorageService } from '@integration/aliyun/service/aliyun-storage.service';
import { assertImageUploadFile } from '@integration/aliyun/util/oss-file.util';
import { ConsumerAuthService } from '@module/auth/service/consumer-auth.service';
import {
  LoginBodySchema,
  RegisterBodySchema,
} from '@module/auth/schema/consumer-auth.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/auth')
export class ConsumerAuthController {
  constructor(
    private readonly consumerAuthService: ConsumerAuthService,
    private readonly aliyunStorageService: AliyunStorageService,
  ) {}

  @Post('register')
  @UseInterceptors(
    FileInterceptor('studentCard', {
      limits: { fileSize: 5 * 1024 * 1024 },
    }),
  )
  async register(
    @Body() body: RegisterBodySchema,
    @UploadedFile() file: Express.Multer.File,
  ) {
    assertImageUploadFile(file, '学生证照片');
    const studentCardUrl = await this.aliyunStorageService.uploadFile(
      AliyunBucket.PRIVATE,
      'student-cards',
      file,
    );
    return this.consumerAuthService.register(body, studentCardUrl);
  }

  @Post('login')
  login(@Body() body: LoginBodySchema) {
    return this.consumerAuthService.login(body);
  }

  @Post('refresh')
  refresh(@Body('refreshToken') refreshToken: string) {
    return this.consumerAuthService.refresh(refreshToken);
  }

  @Post('logout')
  @UseGuards(JwtAuthGuard)
  logout(@CurrentUser('userId') userId: number) {
    return this.consumerAuthService.logout(userId);
  }
}
