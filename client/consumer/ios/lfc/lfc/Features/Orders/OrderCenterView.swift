import SwiftUI

struct OrderCenterView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onOpenTransactions: () -> Void

    @State private var reviewTarget: PaymentOrderListItemDto?
    @State private var reviewRating = 5
    @State private var reviewContent = ""
    @State private var afterSalesTarget: PaymentOrderListItemDto?
    @State private var afterSalesReason = ""
    @State private var cancelTarget: PaymentOrderListItemDto?
    @State private var confirmTarget: PaymentOrderListItemDto?

    private var state: OrderCenterUiState { store.orderCenterState }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                }
                Text("我的订单")
                    .font(.system(size: 17, weight: .semibold))
                Spacer()
                Button("收支流水", action: onOpenTransactions)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(XhsTheme.red)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.white)

            tabBar

            if state.isInitialLoading {
                OrderListSkeleton()
                Spacer(minLength: 0)
            } else if state.orders.isEmpty {
                ContentUnavailableView("暂无订单", systemImage: "bag")
                Spacer()
            } else {
                List {
                    ForEach(state.orders) { order in
                        orderRow(order)
                    }
                    if state.hasMore {
                        SkeletonLoadMoreFooter()
                            .task { await store.loadMoreOrders() }
                    }
                }
                .listStyle(.plain)
                .refreshable { await store.loadOrders(refresh: true) }
            }
        }
        .background(XhsTheme.background)
        .task { await store.loadOrders(refresh: true) }
        .sheet(item: $reviewTarget) { order in
            reviewSheet(order)
        }
        .sheet(item: $afterSalesTarget) { order in
            afterSalesSheet(order)
        }
        .confirmationDialog("取消订单？", isPresented: Binding(
            get: { cancelTarget != nil },
            set: { if !$0 { cancelTarget = nil } }
        )) {
            Button("确认取消", role: .destructive) {
                if let order = cancelTarget {
                    Task { await store.cancelOrder(order) }
                }
                cancelTarget = nil
            }
            Button("返回", role: .cancel) { cancelTarget = nil }
        }
        .confirmationDialog("确认已收到商品/完成履约？", isPresented: Binding(
            get: { confirmTarget != nil },
            set: { if !$0 { confirmTarget = nil } }
        )) {
            Button("确认收货") {
                if let order = confirmTarget {
                    Task { await store.confirmOrderReceipt(order) }
                }
                confirmTarget = nil
            }
            Button("取消", role: .cancel) { confirmTarget = nil }
        }
    }

    private var tabBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 0) {
                ForEach(orderCenterTabs, id: \.0) { tabKey, title in
                    let selected = state.selectedTab == tabKey
                    let count = tabCount(for: tabKey)
                    Button {
                        store.selectOrderTab(tabKey)
                    } label: {
                        VStack(spacing: 6) {
                            Text(count > 0 && tabKey != "all" ? "\(title)(\(count))" : title)
                                .font(.system(size: 14, weight: selected ? .bold : .regular))
                                .foregroundStyle(selected ? XhsTheme.textPrimary : XhsTheme.textSecondary)
                            Rectangle()
                                .fill(selected ? XhsTheme.red : Color.clear)
                                .frame(height: 3)
                        }
                        .padding(.horizontal, 14)
                    }
                }
            }
        }
        .background(Color.white)
    }

    private func tabCount(for key: String) -> Int {
        guard let counts = state.tabCounts else { return 0 }
        switch key {
        case "all": return counts.all
        case "pending_payment": return counts.pendingPayment
        case "awaiting_receipt": return counts.awaitingReceipt
        case "review": return counts.review
        case "after_sales": return counts.afterSales
        default: return 0
        }
    }

    private func orderRow(_ order: PaymentOrderListItemDto) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                orderCover(order)
                VStack(alignment: .leading, spacing: 4) {
                    Text(order.bizTitle)
                        .font(.system(size: 15, weight: .medium))
                        .lineLimit(2)
                    Text(order.statusLabel)
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.red)
                    Text("¥\(order.amount)")
                        .font(.system(size: 16, weight: .bold))
                }
                Spacer()
            }
            HStack(spacing: 8) {
                if order.canPay {
                    actionButton("去支付", filled: true) {
                        Task { await store.payOrder(order) }
                    }
                }
                if order.status == "PENDING_PAYMENT" || order.canPay {
                    actionButton("取消", filled: false) { cancelTarget = order }
                }
                if order.canConfirmReceipt {
                    actionButton("确认收货", filled: true) { confirmTarget = order }
                }
                if order.canReview {
                    actionButton("评价", filled: false) { reviewTarget = order }
                }
                if order.canApplyAfterSales {
                    actionButton("申请售后", filled: false) { afterSalesTarget = order }
                }
            }
            if store.orderCenterState.actingOutTradeNo == order.outTradeNo {
                InlineActionSkeleton()
            }
        }
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private func orderCover(_ order: PaymentOrderListItemDto) -> some View {
        if let url = order.coverImage, let imageURL = URL(string: url) {
            AsyncImage(url: imageURL) { phase in
                if case .success(let image) = phase {
                    image.resizable().scaledToFill()
                } else {
                    Color.gray.opacity(0.15)
                }
            }
            .frame(width: 72, height: 72)
            .clipShape(RoundedRectangle(cornerRadius: 8))
        } else {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.gray.opacity(0.12))
                .frame(width: 72, height: 72)
                .overlay {
                    Image(systemName: "bag")
                        .foregroundStyle(XhsTheme.textSecondary)
                }
        }
    }

    private func actionButton(_ title: String, filled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(filled ? XhsTheme.red : Color.clear)
                .foregroundStyle(filled ? .white : XhsTheme.red)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(XhsTheme.red, lineWidth: filled ? 0 : 1)
                )
                .clipShape(Capsule())
        }
    }

    private func reviewSheet(_ order: PaymentOrderListItemDto) -> some View {
        NavigationStack {
            Form {
                Section("评分") {
                    Picker("评分", selection: $reviewRating) {
                        ForEach(1...5, id: \.self) { value in
                            Text("\(value) 星").tag(value)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                Section("评价内容") {
                    TextField("选填", text: $reviewContent, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle("评价订单")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { reviewTarget = nil }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("提交") {
                        Task {
                            await store.submitOrderReview(order, rating: reviewRating, content: reviewContent)
                            reviewTarget = nil
                        }
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func afterSalesSheet(_ order: PaymentOrderListItemDto) -> some View {
        NavigationStack {
            Form {
                Section("售后原因") {
                    TextField("请描述问题", text: $afterSalesReason, axis: .vertical)
                        .lineLimit(3...8)
                }
            }
            .navigationTitle("申请售后")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { afterSalesTarget = nil }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("提交") {
                        Task {
                            await store.applyOrderAfterSales(order, reason: afterSalesReason)
                            afterSalesTarget = nil
                        }
                    }
                    .disabled(afterSalesReason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}

extension PaymentOrderListItemDto: @retroactive Hashable {
    public func hash(into hasher: inout Hasher) { hasher.combine(outTradeNo) }
    public static func == (lhs: PaymentOrderListItemDto, rhs: PaymentOrderListItemDto) -> Bool {
        lhs.outTradeNo == rhs.outTradeNo
    }
}
