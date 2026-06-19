import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  Query,
  UseGuards,
} from '@nestjs/common';
import { AdminCommentService } from '@module/post/service/admin-comment.service';
import {
  AdminCommentListQuerySchema,
  AdminUpdateCommentBodySchema,
} from '@module/post/schema/admin-comment.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/comment')
@UseGuards(JwtAuthGuard)
export class AdminCommentController {
  constructor(private readonly adminCommentService: AdminCommentService) {}

  @Get()
  findAll(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminCommentListQuerySchema,
  ) {
    return this.adminCommentService.findAll(userId, query);
  }

  @Get(':id')
  findOne(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminCommentService.findOne(userId, id);
  }

  @Patch(':id')
  update(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminUpdateCommentBodySchema,
  ) {
    return this.adminCommentService.update(userId, id, body);
  }

  @Delete(':id')
  remove(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminCommentService.remove(userId, id);
  }
}
