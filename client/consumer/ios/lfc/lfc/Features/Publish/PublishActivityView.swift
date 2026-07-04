import PhotosUI
import SwiftUI

struct PublishActivityView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onBindAlipay: () -> Void
    let onOpenLocationSearch: () -> Void
    let onSubmitSuccess: () -> Void

    @State private var title = ""
    @State private var description = ""
    @State private var locationLabel = ""
    @State private var latitude: Double?
    @State private var longitude: Double?
    @State private var startTime = Date().addingTimeInterval(3600)
    @State private var endTime = Date().addingTimeInterval(7200)
    @State private var maxParticipants = "20"
    @State private var fee = ""
    @State private var pickerItems: [PhotosPickerItem] = []

    private var hasLocation: Bool { hasValidCoordinate(latitude: latitude, longitude: longitude) }
    private var feeAmount: Double { Double(fee) ?? 0 }
    private var needsAlipay: Bool { feeAmount > 0 && store.profileState.myProfile?.alipayBound != true }
    private var canSubmit: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && hasLocation && endTime > startTime
    }

    var body: some View {
        VStack(spacing: 0) {
            PublishTopBar(
                title: "发布活动",
                actionLabel: "发布",
                onBack: onBack,
                onAction: submit,
                actionEnabled: canSubmit && !needsAlipay,
                isSubmitting: store.isPublishSubmitting
            )

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if needsAlipay {
                        Text("绑定支付宝后才能发布付费活动")
                            .font(.system(size: 13))
                            .foregroundStyle(XhsTheme.red)
                        Button("去绑定", action: onBindAlipay)
                            .foregroundStyle(XhsTheme.red)
                    }

                    PublishImagePickerGrid(
                        pickerItems: $pickerItems,
                        existingImageUrls: .constant([]),
                        maxCount: 9
                    )

                    TextField("活动标题", text: $title)
                        .font(.system(size: 18, weight: .bold))

                    TextField("活动介绍、注意事项…", text: $description, axis: .vertical)
                        .lineLimit(4...10)

                    PublishFieldCard {
                        Text("时间与人数")
                            .font(.system(size: 15, weight: .semibold))
                        DatePicker("开始时间", selection: $startTime)
                        DatePicker("结束时间", selection: $endTime)
                        HStack {
                            Text("人数上限")
                            Spacer()
                            TextField("0 表示不限", text: $maxParticipants)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                                .frame(maxWidth: 120)
                        }
                        HStack {
                            Text("报名费用（元）")
                            Spacer()
                            TextField("0 为免费", text: $fee)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .frame(maxWidth: 120)
                        }
                    }

                    PublishFieldCard {
                        Text("活动地点")
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
                                    .foregroundStyle(XhsTheme.textSecondary)
                            }
                        }
                    }
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
    }

    private func submit() {
        let participants = Int(maxParticipants) ?? 0
        Task {
            let payloads = await PublishImageLoader.loadData(from: pickerItems)
            await store.createActivity(
                title: title,
                description: description,
                location: locationLabel,
                latitude: latitude,
                longitude: longitude,
                startTime: startTime,
                endTime: endTime,
                maxParticipants: participants,
                fee: feeAmount,
                imagePayloads: payloads,
                onSuccess: onSubmitSuccess
            )
        }
    }
}
