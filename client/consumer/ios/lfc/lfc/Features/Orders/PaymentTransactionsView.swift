import SwiftUI

struct PaymentTransactionsView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void

    private var state: PaymentTransactionLedgerUiState { store.paymentTransactionState }

    var body: some View {
        VStack(spacing: 0) {
            HomeStubNavigationBar(title: "收支流水", onBack: onBack)

            if state.isInitialLoading {
                PaymentLedgerSkeleton()
                Spacer(minLength: 0)
            } else if state.items.isEmpty {
                ContentUnavailableView("暂无流水记录", systemImage: "yensign.circle")
                Spacer()
            } else {
                List {
                    ForEach(state.items) { item in
                        transactionRow(item)
                    }
                    if state.hasMore {
                        SkeletonLoadMoreFooter()
                            .task { await store.loadMorePaymentTransactions() }
                    }
                }
                .listStyle(.plain)
                .refreshable { await store.loadPaymentTransactions(refresh: true) }
            }
        }
        .background(XhsTheme.background)
        .task { await store.loadPaymentTransactions(refresh: true) }
    }

    private func transactionRow(_ item: PaymentTransactionItemDto) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.typeLabel)
                    .font(.system(size: 14, weight: .medium))
                Text(item.bizTitle)
                    .font(.system(size: 13))
                    .foregroundStyle(XhsTheme.textSecondary)
                    .lineLimit(1)
                Text(item.counterpartyName)
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.textSecondary)
                Text(item.occurredAt)
                    .font(.system(size: 11))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            Spacer()
            Text("\(item.direction == "IN" ? "+" : "-")¥\(item.amount)")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(item.direction == "IN" ? XhsTheme.red : XhsTheme.textPrimary)
        }
        .padding(.vertical, 6)
    }
}
