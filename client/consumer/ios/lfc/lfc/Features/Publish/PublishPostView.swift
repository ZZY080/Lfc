import PhotosUI
import SwiftUI

struct PublishPostView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onBindAlipay: () -> Void
    let onOpenLocationSearch: () -> Void
    let onSubmitSuccess: () -> Void

    @State private var title = ""
    @State private var content = ""
    @State private var category = defaultPostCategory
    @State private var locationLabel = ""
    @State private var latitude: Double?
    @State private var longitude: Double?
    @State private var attachProduct = false
    @State private var price = ""
    @State private var productCategory = defaultPostProductCategory
    @State private var pickerItems: [PhotosPickerItem] = []

    private var categories: [String] {
        store.publishCategories.isEmpty ? fallbackPublishCategories : store.publishCategories
    }

    private var hasLocation: Bool { hasValidCoordinate(latitude: latitude, longitude: longitude) }
    private var productPrice: Double { Double(price) ?? 0 }
    private var needsAlipay: Bool { attachProduct && productPrice > 0 && store.profileState.myProfile?.alipayBound != true }
    private var canSubmit: Bool {
        (!content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !pickerItems.isEmpty) && hasLocation
    }

    var body: some View {
        VStack(spacing: 0) {
            PublishTopBar(
                title: "发布笔记",
                actionLabel: "发布",
                onBack: onBack,
                onAction: submit,
                actionEnabled: canSubmit && (!attachProduct || productPrice > 0) && !needsAlipay,
                isSubmitting: store.isPublishSubmitting
            )

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if needsAlipay {
                        alipayBanner
                    }

                    PublishImagePickerGrid(
                        pickerItems: $pickerItems,
                        existingImageUrls: .constant([]),
                        maxCount: 9
                    )

                    TextField("添加标题（可选）", text: $title)
                        .font(.system(size: 18, weight: .bold))

                    TextField("分享你的校园生活、学习心得，或描述你要出售的商品…", text: $content, axis: .vertical)
                        .lineLimit(5...12)
                        .font(.system(size: 15))

                    PublishFieldCard {
                        Text("笔记类型")
                            .font(.system(size: 15, weight: .semibold))
                        Text("选择后笔记会出现在发现页对应频道")
                            .font(.system(size: 12))
                            .foregroundStyle(XhsTheme.textSecondary)
                        CategoryChipRow(options: categories, selection: $category)
                    }

                    locationSection

                    productSection
                }
                .padding(16)
            }
            .background(XhsTheme.background)
        }
        .background(Color.white)
        .onChange(of: store.pendingLocationPick) { _, pick in
            guard let pick else { return }
            locationLabel = pick.label
            latitude = pick.latitude
            longitude = pick.longitude
            store.consumePendingLocationPick()
        }
        .onAppear {
            if category == defaultPostCategory, let first = categories.first {
                category = first
            }
        }
    }

    private var alipayBanner: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("绑定支付宝后才能发布付费商品")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(XhsTheme.red)
            Text("买家通过支付宝付款，确认收货后款项会分账到你的支付宝。")
                .font(.system(size: 12))
                .foregroundStyle(XhsTheme.textSecondary)
            Button("去绑定", action: onBindAlipay)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(XhsTheme.red)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(XhsTheme.red.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private var locationSection: some View {
        PublishFieldCard {
            Text("发布位置")
                .font(.system(size: 15, weight: .semibold))
            Button(action: onOpenLocationSearch) {
                HStack {
                    Image(systemName: "mappin.and.ellipse")
                        .foregroundStyle(XhsTheme.red)
                    Text(locationLabel.isEmpty ? "选择位置" : locationLabel)
                        .foregroundStyle(locationLabel.isEmpty ? XhsTheme.textSecondary : XhsTheme.textPrimary)
                        .lineLimit(2)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.textSecondary)
                }
            }
            if !hasLocation {
                Text("发布笔记需要记录位置信息")
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.red)
            }
        }
    }

    private var productSection: some View {
        PublishFieldCard {
            Toggle("附带商品信息", isOn: $attachProduct)
                .tint(XhsTheme.red)
            if attachProduct {
                TextField("售价（元）", text: $price)
                    .keyboardType(.decimalPad)
                Picker("商品分类", selection: $productCategory) {
                    ForEach(postProductCategories, id: \.value) { item in
                        Text(item.label).tag(item.value)
                    }
                }
            }
        }
    }

    private func submit() {
        let product: PostProductRequest? = attachProduct ? PostProductRequest(
            price: productPrice,
            originalPrice: nil,
            category: productCategory,
            condition: "GOOD",
            deliveryMethod: "PICKUP"
        ) : nil

        Task {
            let payloads = await PublishImageLoader.loadData(from: pickerItems)
            await store.createPost(
                title: title,
                content: content,
                imagePayloads: payloads,
                category: category,
                product: product,
                latitude: latitude,
                longitude: longitude,
                location: locationLabel,
                onSuccess: onSubmitSuccess
            )
        }
    }
}
