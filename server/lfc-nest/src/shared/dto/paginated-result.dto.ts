export interface PaginatedResultDto<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  hasMore: boolean;
}

export function normalizePagination(page?: number, limit?: number) {
  const normalizedPage = Math.max(page ?? 1, 1);
  const normalizedLimit = Math.min(Math.max(limit ?? 10, 1), 30);
  return {
    page: normalizedPage,
    limit: normalizedLimit,
    skip: (normalizedPage - 1) * normalizedLimit,
  };
}

export function createPaginatedResult<T>(
  items: T[],
  total: number,
  page: number,
  limit: number,
): PaginatedResultDto<T> {
  return {
    items,
    total,
    page,
    limit,
    hasMore: page * limit < total,
  };
}
