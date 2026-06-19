import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { AdminUserService } from '@module/user/service/admin-user.service';
import {
  AdminCreateUserBodySchema,
  AdminUpdateUserBodySchema,
  AdminUpdateUserRoleBodySchema,
  AdminUpdateUserStatusBodySchema,
  AdminUserListQuerySchema,
} from '@module/user/schema/admin-user.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/user')
@UseGuards(JwtAuthGuard)
export class AdminUserController {
  constructor(private readonly adminUserService: AdminUserService) {}

  @Get()
  findAll(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminUserListQuerySchema,
  ) {
    return this.adminUserService.findAll(userId, query);
  }

  @Post()
  create(
    @CurrentUser('userId') userId: number,
    @Body() body: AdminCreateUserBodySchema,
  ) {
    return this.adminUserService.create(userId, body);
  }

  @Get(':id')
  findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminUserService.findOne(userId, id);
  }

  @Patch(':id')
  update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdateUserBodySchema,
  ) {
    return this.adminUserService.update(userId, id, body);
  }

  @Patch(':id/status')
  updateStatus(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdateUserStatusBodySchema,
  ) {
    return this.adminUserService.updateStatus(userId, id, body);
  }

  @Patch(':id/role')
  updateRole(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdateUserRoleBodySchema,
  ) {
    return this.adminUserService.updateRole(userId, id, body);
  }

  @Delete(':id')
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminUserService.remove(userId, id);
  }
}
