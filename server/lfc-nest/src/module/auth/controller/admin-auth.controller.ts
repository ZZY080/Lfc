import { Body, Controller, Post, UseGuards } from '@nestjs/common';
import { AdminAuthService } from '@module/auth/service/admin-auth.service';
import { AdminLoginBodySchema } from '@module/auth/schema/admin-auth.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/auth')
export class AdminAuthController {
  constructor(private readonly adminAuthService: AdminAuthService) {}

  @Post('login')
  login(@Body() body: AdminLoginBodySchema) {
    return this.adminAuthService.login(body);
  }

  @Post('logout')
  @UseGuards(JwtAuthGuard)
  logout(@CurrentUser('userId') userId: number) {
    return this.adminAuthService.logout(userId);
  }
}
