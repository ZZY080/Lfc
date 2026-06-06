import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, In, Repository } from 'typeorm';
import {
  PaymentBizType,
  PaymentTransactionType,
} from '@shared/enum/payment.enum';
import { PaymentTransactionItemDto } from '@module/payment/dto/payment.dto';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { isPlatformDirectRevenueBizType } from '@module/payment/util/payment-order.util';

interface PaymentTransactionRow {
  tx_type: PaymentTransactionType;
  tx_key: string;
  out_trade_no: string;
  amount: string;
  subject: string;
  biz_type: PaymentBizType;
  biz_id: number;
  payee_id: number;
  trade_no: string | null;
  occurred_at: Date;
}

@Injectable()
export class PaymentTransactionQueryService {
  constructor(
    private readonly dataSource: DataSource,
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
  ) {}

  async listForUser(userId: number, page?: number, limit?: number) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);

    const ledgerSql = `
      SELECT * FROM (
        SELECT
          'PAYMENT' AS tx_type,
          CONCAT('PAYMENT:', po.out_trade_no) AS tx_key,
          po.out_trade_no,
          po.amount,
          po.subject,
          po.biz_type,
          po.biz_id,
          po.payee_id,
          po.trade_no,
          po.paid_at AS occurred_at
        FROM payment_order po
        WHERE po.user_id = ?
          AND po.paid_at IS NOT NULL
        UNION ALL
        SELECT
          'REFUND' AS tx_type,
          CONCAT('REFUND:', po.out_trade_no) AS tx_key,
          po.out_trade_no,
          po.amount,
          po.subject,
          po.biz_type,
          po.biz_id,
          po.payee_id,
          po.trade_no,
          COALESCE(
            (
              SELECT a.processed_at
              FROM payment_after_sales a
              WHERE a.payment_order_id = po.id
                AND a.status = 'REFUNDED'
              ORDER BY a.processed_at DESC
              LIMIT 1
            ),
            po.updated_at
          ) AS occurred_at
        FROM payment_order po
        WHERE po.user_id = ?
          AND po.status = 'REFUNDED'
      ) ledger
      ORDER BY occurred_at DESC
      LIMIT ? OFFSET ?
    `;

    const countSql = `
      SELECT COUNT(*) AS total FROM (
        SELECT po.id
        FROM payment_order po
        WHERE po.user_id = ?
          AND po.paid_at IS NOT NULL
        UNION ALL
        SELECT po.id
        FROM payment_order po
        WHERE po.user_id = ?
          AND po.status = 'REFUNDED'
      ) ledger_count
    `;

    const [rows, countRows] = await Promise.all([
      this.dataSource.query(ledgerSql, [
        userId,
        userId,
        normalizedLimit,
        skip,
      ]) as Promise<PaymentTransactionRow[]>,
      this.dataSource.query(countSql, [userId, userId]) as Promise<
        Array<{ total: string | number }>
      >,
    ]);

    const total = Number(countRows[0]?.total ?? 0);
    const items = await this.enrichTransactions(rows);

    return createPaginatedResult(
      items,
      total,
      normalizedPage,
      normalizedLimit,
    );
  }

  private async enrichTransactions(
    rows: PaymentTransactionRow[],
  ): Promise<PaymentTransactionItemDto[]> {
    if (rows.length === 0) {
      return [];
    }

    const postIds = [
      ...new Set(
        rows
          .filter(
            (item) =>
              item.biz_type === PaymentBizType.POST_PRODUCT_PURCHASE ||
              item.biz_type === PaymentBizType.POST_BOOST,
          )
          .map((item) => item.biz_id),
      ),
    ];
    const activityIds = [
      ...new Set(
        rows
          .filter(
            (item) =>
              item.biz_type === PaymentBizType.ACTIVITY_JOIN ||
              item.biz_type === PaymentBizType.ACTIVITY_PROMOTE,
          )
          .map((item) => item.biz_id),
      ),
    ];
    const payeeIds = [...new Set(rows.map((item) => item.payee_id))];

    const [posts, activities, payees] = await Promise.all([
      postIds.length
        ? this.postRepository.find({ where: { id: In(postIds) } })
        : Promise.resolve([] as PostEntity[]),
      activityIds.length
        ? this.activityRepository.find({ where: { id: In(activityIds) } })
        : Promise.resolve([] as ActivityEntity[]),
      payeeIds.length
        ? this.userRepository.find({ where: { id: In(payeeIds) } })
        : Promise.resolve([] as UserEntity[]),
    ]);

    const postMap = new Map(posts.map((item) => [item.id, item]));
    const activityMap = new Map(activities.map((item) => [item.id, item]));
    const payeeMap = new Map(payees.map((item) => [item.id, item]));

    return rows.map((row) => {
      const post =
        row.biz_type === PaymentBizType.POST_PRODUCT_PURCHASE ||
        row.biz_type === PaymentBizType.POST_BOOST
          ? postMap.get(row.biz_id)
          : undefined;
      const activity =
        row.biz_type === PaymentBizType.ACTIVITY_JOIN ||
        row.biz_type === PaymentBizType.ACTIVITY_PROMOTE
          ? activityMap.get(row.biz_id)
          : undefined;
      const payee = payeeMap.get(row.payee_id);
      const isPlatformRevenue = isPlatformDirectRevenueBizType(row.biz_type);
      const isRefund = row.tx_type === PaymentTransactionType.REFUND;

      return {
        txKey: row.tx_key,
        type: row.tx_type,
        typeLabel: isRefund ? '退款' : '支付',
        direction: isRefund ? 'IN' : 'OUT',
        amount: row.amount,
        outTradeNo: row.out_trade_no,
        tradeNo: row.trade_no,
        subject: row.subject,
        bizType: row.biz_type,
        bizId: row.biz_id,
        bizTitle: post?.title ?? activity?.title ?? row.subject,
        coverImage: post?.images?.[0] ?? activity?.images?.[0] ?? null,
        counterpartyName: isPlatformRevenue
          ? '莲峰校园平台'
          : payee?.nickname?.trim() || `同学${row.payee_id}`,
        occurredAt: row.occurred_at,
      };
    });
  }
}
