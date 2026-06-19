import { Controller, Get, UseGuards } from '@nestjs/common';
import { AdminStatsService } from '@module/admin/service/admin-stats.service';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/stats')
@UseGuards(JwtAuthGuard)
export class AdminStatsController {
  constructor(private readonly adminStatsService: AdminStatsService) {}

  @Get('overview')
  getOverview(@CurrentUser('userId') userId: number) {
    return this.adminStatsService.getOverview(userId);
  }
}
