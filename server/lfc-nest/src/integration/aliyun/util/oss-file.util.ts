import { BadRequestException } from '@nestjs/common';

const ALLOWED_MIME = new Set([
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/webp',
  'image/gif',
]);

const ALLOWED_EXT = /\.(jpe?g|png|webp|gif)$/i;

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
