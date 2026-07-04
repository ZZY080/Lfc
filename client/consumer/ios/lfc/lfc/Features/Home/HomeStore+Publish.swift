import Foundation

private let alipayBindRequiredMessage = "发布付费内容前请先绑定支付宝收款账号"

typealias ImageUploadPayload = (data: Data, filename: String)

extension HomeStore {
    func consumePendingLocationPick() {
        pendingLocationPick = nil
    }

    func loadPublishCategories() async {
        do {
            let catalog = try await api.getFeedChannelCatalog()
            publishCategories = catalog.publishCategories.isEmpty
                ? fallbackPublishCategories
                : catalog.publishCategories
        } catch {
            publishCategories = fallbackPublishCategories
        }
    }

    func loadPaymentConfig() async {
        paymentConfig = try? await api.getPaymentConfig()
    }

    func createPost(
        title: String,
        content: String,
        imagePayloads: [ImageUploadPayload],
        category: String,
        product: PostProductRequest?,
        latitude: Double?,
        longitude: Double?,
        location: String?,
        onSuccess: @escaping () -> Void
    ) async {
        let price = product?.price ?? 0
        if price > 0, profileState.myProfile?.alipayBound != true {
            toastError = alipayBindRequiredMessage
            return
        }

        isPublishSubmitting = true
        defer { isPublishSubmitting = false }

        do {
            let uploads = try await uploadImages(imagePayloads, scope: "post")
            let request = CreatePostRequest(
                title: title.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                category: category,
                content: content.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                images: uploads.nilIfEmpty,
                product: product,
                latitude: latitude,
                longitude: longitude,
                location: location?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
            )
            _ = try await api.createPost(request)
            toastMessage = "信息发布成功"
            await loadFeed(refresh: true)
            await loadProfile()
            onSuccess()
        } catch {
            toastError = parseError(error, fallback: "发布失败")
        }
    }

    func updatePost(
        id: Int,
        title: String,
        content: String,
        imagePayloads: [ImageUploadPayload],
        existingImages: [String],
        category: String,
        latitude: Double?,
        longitude: Double?,
        location: String?,
        onSuccess: @escaping () -> Void
    ) async {
        isPublishSubmitting = true
        defer { isPublishSubmitting = false }

        do {
            let newUrls = try await uploadImages(imagePayloads, scope: "post")
            let images = existingImages + newUrls
            let request = UpdatePostRequest(
                title: title.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                category: category,
                content: content.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                images: images.nilIfEmpty,
                product: nil,
                latitude: latitude,
                longitude: longitude,
                location: location?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
            )
            let updated = try await api.updatePost(id: id, request: request)
            selectedPost = updated
            toastMessage = "信息已更新，等待重新审核"
            await loadFeed(refresh: true)
            await loadProfile()
            onSuccess()
        } catch {
            toastError = parseError(error, fallback: "更新失败")
        }
    }

    func createActivity(
        title: String,
        description: String,
        location: String,
        latitude: Double?,
        longitude: Double?,
        startTime: Date,
        endTime: Date,
        maxParticipants: Int,
        fee: Double,
        imagePayloads: [ImageUploadPayload],
        onSuccess: @escaping () -> Void
    ) async {
        if fee > 0, profileState.myProfile?.alipayBound != true {
            toastError = alipayBindRequiredMessage
            return
        }

        isPublishSubmitting = true
        defer { isPublishSubmitting = false }

        do {
            let uploads = try await uploadImages(imagePayloads, scope: "activity")
            let request = CreateActivityRequest(
                title: title.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                description: description.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                images: uploads.nilIfEmpty,
                location: location,
                latitude: latitude,
                longitude: longitude,
                startTime: normalizeDateTime(startTime),
                endTime: normalizeDateTime(endTime),
                maxParticipants: maxParticipants,
                fee: fee > 0 ? fee : nil
            )
            _ = try await api.createActivity(request)
            toastMessage = "活动已提交，等待审核"
            await loadActivityFeed(refresh: true)
            await loadProfile()
            onSuccess()
        } catch {
            toastError = parseError(error, fallback: "发布失败")
        }
    }

    func updateActivity(
        id: Int,
        title: String,
        description: String,
        location: String,
        latitude: Double?,
        longitude: Double?,
        startTime: Date,
        endTime: Date,
        maxParticipants: Int,
        imagePayloads: [ImageUploadPayload],
        existingImages: [String],
        onSuccess: @escaping () -> Void
    ) async {
        isPublishSubmitting = true
        defer { isPublishSubmitting = false }

        do {
            let newUrls = try await uploadImages(imagePayloads, scope: "activity")
            let images = existingImages + newUrls
            let request = UpdateActivityRequest(
                title: title.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                description: description.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                images: images.nilIfEmpty,
                location: location,
                latitude: latitude,
                longitude: longitude,
                startTime: normalizeDateTime(startTime),
                endTime: normalizeDateTime(endTime),
                maxParticipants: maxParticipants,
                fee: nil
            )
            let updated = try await api.updateActivity(id: id, request: request)
            selectedActivity = updated
            toastMessage = "活动已更新，等待重新审核"
            await loadActivityFeed(refresh: true)
            await loadProfile()
            onSuccess()
        } catch {
            toastError = parseError(error, fallback: "更新失败")
        }
    }

    private func uploadImages(_ payloads: [ImageUploadPayload], scope: String) async throws -> [String] {
        var urls: [String] = []
        for payload in payloads {
            let response = try await api.uploadImage(
                data: payload.data,
                filename: payload.filename,
                mimeType: "image/jpeg",
                scope: scope
            )
            urls.append(response.url)
        }
        return urls
    }

    private func normalizeDateTime(_ date: Date) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.string(from: date)
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}

private extension Array {
    var nilIfEmpty: [Element]? {
        isEmpty ? nil : self
    }
}
