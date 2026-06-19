export interface AdminStatsOverviewDto {
  users: {
    total: number;
    consumers: number;
    admins: number;
    recent7Days: number;
  };
  posts: {
    total: number;
    pending: number;
    approved: number;
    rejected: number;
    offShelf: number;
    visible: number;
    hidden: number;
  };
  activities: {
    total: number;
    pending: number;
    approved: number;
    rejected: number;
    offShelf: number;
  };
  payments: {
    total: number;
    paid: number;
    totalPaidAmount: string;
    pendingAfterSales: number;
  };
}
