import PhotosUI
import SwiftUI

struct EditActivityView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onOpenLocationSearch: () -> Void

    @State private var title = ""
    @State private var description = ""
    @State private var locationLabel = ""
    @State private var latitude: Double?
    @State private var longitude: Double?
    @State private var startTime = Date()
    @State private var endTime = Date()
    @State private var maxParticipants = "0"
    @State private var existingImages: [String] = []
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var hydrated = false

    private var activity: ActivityDto? { store.selectedActivity }
    private var hasLocation: Bool { hasValidCoordinate(latitude: latitude, longitude: longitude) }
    private var canSubmit: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && hasLocation && endTime > startTime
    }

    var body: some View {
        VStack(spacing: 0) {
            PublishTopBar(
                title: "编辑活动",
                actionLabel: "保存",
                onBack: onBack,
                onAction: submit,
                actionEnabled: canSubmit && activity != nil,
                isSubmitting: store.isPublishSubmitting
            )

            if activity == nil {
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

                        TextField("活动标题", text: $title)
                            .font(.system(size: 18, weight: .bold))

                        TextField("活动介绍", text: $description, axis: .vertical)
                            .lineLimit(4...10)

                        PublishFieldCard {
                            DatePicker("开始时间", selection: $startTime)
                            DatePicker("结束时间", selection: $endTime)
                            HStack {
                                Text("人数上限")
                                Spacer()
                                TextField("", text: $maxParticipants)
                                    .keyboardType(.numberPad)
                                    .multilineTextAlignment(.trailing)
                                    .frame(maxWidth: 120)
                            }
                        }

                        PublishFieldCard {
                            Button(action: onOpenLocationSearch) {
                                HStack {
                                    Image(systemName: "mappin.and.ellipse")
                                    Text(locationLabel.isEmpty ? "选择位置" : locationLabel)
                                        .lineLimit(2)
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                }
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
        .onChange(of: store.selectedActivity?.id) { _, _ in
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
        guard !hydrated, let activity else { return }
        title = activity.title
        description = activity.description ?? ""
        locationLabel = activity.location
        latitude = activity.latitude
        longitude = activity.longitude
        startTime = parseActivityDate(activity.startTime) ?? Date()
        endTime = parseActivityDate(activity.endTime) ?? Date().addingTimeInterval(3600)
        maxParticipants = String(activity.maxParticipants)
        existingImages = activity.images ?? []
        hydrated = true
    }

    private func submit() {
        guard let activity else { return }
        let participants = Int(maxParticipants) ?? 0
        Task {
            let payloads = await PublishImageLoader.loadData(from: pickerItems)
            await store.updateActivity(
                id: activity.id,
                title: title,
                description: description,
                location: locationLabel,
                latitude: latitude,
                longitude: longitude,
                startTime: startTime,
                endTime: endTime,
                maxParticipants: participants,
                imagePayloads: payloads,
                existingImages: existingImages,
                onSuccess: onBack
            )
        }
    }

    private func parseActivityDate(_ value: String) -> Date? {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = iso.date(from: value) { return date }
        iso.formatOptions = [.withInternetDateTime]
        if let date = iso.date(from: value) { return date }
        let fallback = DateFormatter()
        fallback.locale = Locale(identifier: "en_US_POSIX")
        fallback.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return fallback.date(from: value.prefix(19).description)
    }
}
