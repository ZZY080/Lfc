import { BadRequestException } from '@nestjs/common';

const ALLOWED_MIME = new Set([
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/webp',
  'image/gif',
]);

const ALLOWED_EXT = /\.(jpe?g|png|webp|gif)$/i;

const ALLOWED_VIDEO_MIME = new Set([
  'video/mp4',
  'video/quicktime',
  'video/webm',
  'video/3gpp',
]);

const ALLOWED_VIDEO_EXT = /\.(mp4|mov|webm|3gp)$/i;

export function assertImageUploadFile(
  file: Express.Multer.File | undefined,
  label = '图片',
): asserts file is Express.Multer.File {
  if (!file) {
    throw new BadRequestException(`请上传${label}`);
  }
  const mime = file.mimetype?.toLowerCase() || '';
  if (!ALLOWED_MIME.has(mime) && !ALLOWED_EXT.test(file.originalname || '')) {
    throw new BadRequestException(`${label}仅支持 JPG、PNG、WEBP、GIF 格式`);
  }
}

export function assertVideoUploadFile(
  file: Express.Multer.File | undefined,
  label = '视频',
): asserts file is Express.Multer.File {
  if (!file) {
    throw new BadRequestException(`请上传${label}`);
  }
  const mime = file.mimetype?.toLowerCase() || '';
  if (!ALLOWED_VIDEO_MIME.has(mime) && !ALLOWED_VIDEO_EXT.test(file.originalname || '')) {
    throw new BadRequestException(`${label}仅支持 MP4、MOV、WEBM 格式`);
  }
}
