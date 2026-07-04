import PhotosUI
import SwiftUI

struct EditPostView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onOpenLocationSearch: () -> Void

    @State private var title = ""
    @State private var content = ""
    @State private var category = defaultPostCategory
    @State private var locationLabel = ""
    @State private var latitude: Double?
    @State private var longitude: Double?
    @State private var existingImages: [String] = []
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var hydrated = false

    private var post: PostDto? { store.selectedPost }
    private var hasLocation: Bool { hasValidCoordinate(latitude: latitude, longitude: longitude) }
    private var canSubmit: Bool {
        (!content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !existingImages.isEmpty || !pickerItems.isEmpty) && hasLocation
    }

    var body: some View {
        VStack(spacing: 0) {
            PublishTopBar(
                title: "编辑笔记",
                actionLabel: "保存",
                onBack: onBack,
                onAction: submit,
                actionEnabled: canSubmit && post != nil,
                isSubmitting: store.isPublishSubmitting
            )

            if post == nil {
                EditFormSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        PublishImagePickerGrid(
                            pickerItems: $pickerItems,
                            existingImageUrls: $existingImages,
                            maxCount: 9
                        )

                        TextField("标题", text: $title)
                            .font(.system(size: 18, weight: .bold))

                        TextField("内容", text: $content, axis: .vertical)
                            .lineLimit(5...12)

                        PublishFieldCard {
                            Text("笔记类型")
                                .font(.system(size: 15, weight: .semibold))
                            CategoryChipRow(
                                options: store.publishCategories.isEmpty ? fallbackPublishCategories : store.publishCategories,
                                selection: $category
                            )
                        }

                        PublishFieldCard {
                            Text("发布位置")
                                .font(.system(size: 15, weight: .semibold))
                            Button(action: onOpenLocationSearch) {
                                HStack {
                                    Image(systemName: "mappin.and.ellipse")
                                    Text(locationLabel.isEmpty ? "选择位置" : locationLabel)
                                        .lineLimit(2)
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                }
                                .foregroundStyle(XhsTheme.textPrimary)
                            }
                        }
                    }
                    .padding(16)
                }
                .background(XhsTheme.background)
            }
        }
        .background(Color.white)
        .onAppear { hydrateIfNeeded() }
        .onChange(of: store.selectedPost?.id) { _, _ in
            hydrated = false
            hydrateIfNeeded()
        }
        .onChange(of: store.pendingLocationPick) { _, pick in
            guard let pick else { return }
            locationLabel = pick.label
            latitude = pick.latitude
            longitude = pick.longitude
            store.consumePendingLocationPick()
        }
    }

    private func hydrateIfNeeded() {
        guard !hydrated, let post else { return }
        title = post.title
        content = post.content
        category = post.category
        locationLabel = post.location ?? ""
        latitude = post.latitude
        longitude = post.longitude
        existingImages = post.images ?? []
        hydrated = true
    }

    private func submit() {
        guard let post else { return }
        Task {
            let payloads = await PublishImageLoader.loadData(from: pickerItems)
            await store.updatePost(
                id: post.id,
                title: title,
                content: content,
                imagePayloads: payloads,
                existingImages: existingImages,
                category: category,
                latitude: latitude,
                longitude: longitude,
                location: locationLabel,
                onSuccess: onBack
            )
        }
    }
}
