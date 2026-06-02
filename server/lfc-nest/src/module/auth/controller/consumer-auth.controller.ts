import {
  BadRequestException,
  Body,
  Controller,
  Post,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { existsSync, mkdirSync } from 'fs';
import { extname, join } from 'path';
import { ConsumerAuthService } from '@module/auth/service/consumer-auth.service';
import {
  LoginBodySchema,
  RegisterBodySchema,
} from '@module/auth/schema/consumer-auth.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('consumer/auth')
export class ConsumerAuthController {
  constructor(private readonly consumerAuthService: ConsumerAuthService) {}

  @Post('register')
  @UseInterceptors(
    FileInterceptor('studentCard', {
      storage: diskStorage({
        destination: (_req, _file, cb) => {
          const uploadDir = join(process.cwd(), 'uploads', 'student-cards');
          if (!existsSync(uploadDir)) {
            mkdirSync(uploadDir, { recursive: true });
          }
          cb(null, uploadDir);
        },
        filename: (_req, file, cb) => {
          const uniqueSuffix = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
          cb(null, `${uniqueSuffix}${extname(file.originalname)}`);
        },
      }),
      fileFilter: (_req, file, cb) => {
        const allowedMime = /\/(jpg|jpeg|png|webp)$/;
        const allowedExt = /\.(jpe?g|png|webp)$/i;
        if (allowedMime.test(file.mimetype) || allowedExt.test(file.originalname)) {
          cb(null, true);
          return;
        }
        cb(new BadRequestException('学生证仅支持 jpg/jpeg/png/webp 格式'), false);
      },
      limits: { fileSize: 5 * 1024 * 1024 },
    }),
  )
  register(
    @Body() body: RegisterBodySchema,
    @UploadedFile() file: Express.Multer.File,
  ) {
    if (!file) {
      throw new BadRequestException('请上传学生证照片');
    }
    return this.consumerAuthService.register(
      body,
      `/uploads/student-cards/${file.filename}`,
    );
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
