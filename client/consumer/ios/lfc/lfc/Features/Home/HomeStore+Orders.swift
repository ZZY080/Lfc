import Foundation

private let orderPageSize = 10
private let transactionPageSize = 20

extension HomeStore {
    func selectOrderTab(_ tab: String) {
        guard orderCenterState.selectedTab != tab else { return }
        orderCenterState = OrderCenterUiState(selectedTab: tab)
        Task { await loadOrders(refresh: true) }
    }

    func loadOrders(refresh: Bool = false) async {
        if refresh, orderCenterState.isRefreshing { return }
        if !refresh, orderCenterState.isLoadingMore { return }

        let page = refresh ? 1 : orderCenterState.page
        orderCenterState.isRefreshing = refresh
        orderCenterState.isInitialLoading = refresh && orderCenterState.orders.isEmpty
        orderCenterState.isLoadingMore = !refresh && orderCenterState.hasMore
        orderCenterState.page = page

        do {
            if refresh {
                orderCenterState.tabCounts = try? await api.getPaymentOrderTabCounts()
            }
            let response = try await api.getPaymentOrders(
                tab: orderCenterState.selectedTab,
                page: page,
                limit: orderPageSize
            )
            let merged: [PaymentOrderListItemDto]
            if refresh {
                merged = response.items
            } else {
                let existing = Set(orderCenterState.orders.map(\.outTradeNo))
                merged = orderCenterState.orders + response.items.filter { !existing.contains($0.outTradeNo) }
            }
            orderCenterState.orders = merged
            orderCenterState.page = response.page + 1
            orderCenterState.hasMore = response.hasMore
        } catch {
            toastError = parseError(error, fallback: "加载订单失败")
        }

        orderCenterState.isRefreshing = false
        orderCenterState.isInitialLoading = false
        orderCenterState.isLoadingMore = false
    }

    func loadMoreOrders() async {
        guard orderCenterState.hasMore,
              !orderCenterState.isLoadingMore,
              !orderCenterState.isRefreshing else { return }
        await loadOrders(refresh: false)
    }

    func payOrder(_ order: PaymentOrderListItemDto) async {
        orderCenterState.actingOutTradeNo = order.outTradeNo
        defer { orderCenterState.actingOutTradeNo = nil }

        do {
            let payment = try await api.repayPaymentOrder(outTradeNo: order.outTradeNo)
            if payment.alipayOrderStr.isEmpty {
                toastMessage = "请在支付宝 App 中完成支付：\(payment.subject)"
            } else {
                toastMessage = "订单已创建，请使用支付宝 App 完成支付"
            }
            await loadOrders(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "支付失败")
        }
    }

    func cancelOrder(_ order: PaymentOrderListItemDto) async {
        orderCenterState.actingOutTradeNo = order.outTradeNo
        defer { orderCenterState.actingOutTradeNo = nil }

        do {
            _ = try await api.cancelPaymentOrder(outTradeNo: order.outTradeNo)
            toastMessage = "订单已取消"
            await loadOrders(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "取消失败")
        }
    }

    func confirmOrderReceipt(_ order: PaymentOrderListItemDto) async {
        orderCenterState.actingOutTradeNo = order.outTradeNo
        defer { orderCenterState.actingOutTradeNo = nil }

        do {
            _ = try await api.confirmPaymentReceipt(outTradeNo: order.outTradeNo)
            toastMessage = "已确认收货"
            await loadOrders(refresh: true)
            await loadProfile()
        } catch {
            toastError = parseError(error, fallback: "确认收货失败")
        }
    }

    func submitOrderReview(_ order: PaymentOrderListItemDto, rating: Int, content: String?) async {
        orderCenterState.actingOutTradeNo = order.outTradeNo
        defer { orderCenterState.actingOutTradeNo = nil }

        do {
            _ = try await api.createPaymentOrderReview(
                outTradeNo: order.outTradeNo,
                request: CreateOrderReviewRequest(rating: rating, content: content?.nilIfBlank)
            )
            toastMessage = "评价已提交"
            await loadOrders(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "评价失败")
        }
    }

    func applyOrderAfterSales(_ order: PaymentOrderListItemDto, reason: String) async {
        orderCenterState.actingOutTradeNo = order.outTradeNo
        defer { orderCenterState.actingOutTradeNo = nil }

        do {
            _ = try await api.applyPaymentAfterSales(
                outTradeNo: order.outTradeNo,
                request: ApplyAfterSalesRequest(reason: reason)
            )
            toastMessage = "售后申请已提交"
            await loadOrders(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "申请失败")
        }
    }

    func loadPaymentTransactions(refresh: Bool = false) async {
        if refresh, paymentTransactionState.isRefreshing { return }
        if !refresh, paymentTransactionState.isLoadingMore { return }

        let page = refresh ? 1 : paymentTransactionState.page
        paymentTransactionState.isRefreshing = refresh
        paymentTransactionState.isInitialLoading = refresh && paymentTransactionState.items.isEmpty
        paymentTransactionState.isLoadingMore = !refresh && paymentTransactionState.hasMore
        paymentTransactionState.page = page

        do {
            let response = try await api.getPaymentTransactions(page: page, limit: transactionPageSize)
            let merged: [PaymentTransactionItemDto]
            if refresh {
                merged = response.items
            } else {
                let existing = Set(paymentTransactionState.items.map(\.txKey))
                merged = paymentTransactionState.items + response.items.filter { !existing.contains($0.txKey) }
            }
            paymentTransactionState.items = merged
            paymentTransactionState.page = response.page + 1
            paymentTransactionState.hasMore = response.hasMore
        } catch {
            toastError = parseError(error, fallback: "加载流水失败")
        }

        paymentTransactionState.isRefreshing = false
        paymentTransactionState.isInitialLoading = false
        paymentTransactionState.isLoadingMore = false
    }

    func loadMorePaymentTransactions() async {
        guard paymentTransactionState.hasMore,
              !paymentTransactionState.isLoadingMore,
              !paymentTransactionState.isRefreshing else { return }
        await loadPaymentTransactions(refresh: false)
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
