# Database migrations

When `MYSQL_SYNCHRONIZE=false` (recommended for production), apply scripts in this order:

1. `mysql-init.sql` — create database
2. `migrate-im-schema.sql`
3. `migrate-payment-schema.sql`
4. `migrate-user-real-name.sql`
5. `migrate-payment-order-center.sql`
6. `migrate-payment-royalty-settle.sql`
7. `migrate-activity-social.sql` — activity like/favorite tables
8. `migrate-post-view-count.sql` — post view_count column
9. `migrate-payment-idempotency.sql` — payment active_key, channel_trade_key, version

Example:

```bash
mysql -u root -p < scripts/mysql-init.sql
for f in migrate-im-schema.sql migrate-payment-schema.sql migrate-user-real-name.sql migrate-payment-order-center.sql migrate-payment-royalty-settle.sql migrate-activity-social.sql migrate-post-view-count.sql migrate-payment-idempotency.sql; do
  mysql -u root -p lfc < "scripts/$f"
done
```

Development with `MYSQL_SYNCHRONIZE=true` may create tables automatically, but activity social tables should still be verified if like/favorite APIs fail.
