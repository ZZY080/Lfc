import PhotosUI
import SwiftUI

struct PublishTopBar: View {
    let title: String
    let actionLabel: String
    let onBack: () -> Void
    let onAction: () -> Void
    var actionEnabled: Bool = true
    var isSubmitting: Bool = false

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 36, height: 36)
            }
            Text(title)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(XhsTheme.textPrimary)
            Spacer()
            Button(action: onAction) {
                if isSubmitting {
                    ProgressView()
                        .tint(XhsTheme.red)
                } else {
                    Text(actionLabel)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(actionEnabled ? XhsTheme.red : XhsTheme.textSecondary)
                }
            }
            .disabled(!actionEnabled || isSubmitting)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(Color.white)
        .overlay(alignment: .bottom) {
            Divider().overlay(XhsTheme.divider)
        }
    }
}

struct PublishImagePickerGrid: View {
    @Binding var pickerItems: [PhotosPickerItem]
    @Binding var existingImageUrls: [String]
    let maxCount: Int

    var body: some View {
        let total = existingImageUrls.count + pickerItems.count
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(Array(existingImageUrls.enumerated()), id: \.offset) { index, url in
                    ZStack(alignment: .topTrailing) {
                        AsyncImage(url: URL(string: url)) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            default:
                                Color.gray.opacity(0.15)
                            }
                        }
                        .frame(width: 88, height: 88)
                        .clipShape(RoundedRectangle(cornerRadius: 8))

                        Button {
                            existingImageUrls.remove(at: index)
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.white, .black.opacity(0.55))
                        }
                        .offset(x: 4, y: -4)
                    }
                }

                ForEach(Array(pickerItems.enumerated()), id: \.offset) { index, _ in
                    ZStack(alignment: .topTrailing) {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(Color.gray.opacity(0.12))
                            .frame(width: 88, height: 88)
                            .overlay {
                                Image(systemName: "photo")
                                    .foregroundStyle(XhsTheme.textSecondary)
                            }
                        Button {
                            pickerItems.remove(at: index)
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.white, .black.opacity(0.55))
                        }
                        .offset(x: 4, y: -4)
                    }
                }

                if total < maxCount {
                    PhotosPicker(selection: $pickerItems, maxSelectionCount: maxCount - total, matching: .images) {
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(XhsTheme.divider, lineWidth: 1)
                            .frame(width: 88, height: 88)
                            .overlay {
                                VStack(spacing: 4) {
                                    Image(systemName: "plus")
                                    Text("添加")
                                        .font(.system(size: 11))
                                }
                                .foregroundStyle(XhsTheme.textSecondary)
                            }
                    }
                }
            }
        }
    }
}

struct CategoryChipRow: View {
    let options: [String]
    @Binding var selection: String

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                Button {
                    selection = option
                } label: {
                    Text(option)
                        .font(.system(size: 13))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(selection == option ? XhsTheme.red.opacity(0.12) : Color.gray.opacity(0.08))
                        .foregroundStyle(selection == option ? XhsTheme.red : XhsTheme.textPrimary)
                        .clipShape(Capsule())
                }
            }
        }
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, frame) in result.frames.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                proposal: ProposedViewSize(frame.size)
            )
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, frames: [CGRect]) {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var frames: [CGRect] = []

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            frames.append(CGRect(origin: CGPoint(x: x, y: y), size: size))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }

        return (CGSize(width: maxWidth, height: y + rowHeight), frames)
    }
}

struct PublishFieldCard<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            content()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(XhsTheme.divider, lineWidth: 0.5)
        )
    }
}

enum PublishImageLoader {
    static func loadData(from items: [PhotosPickerItem]) async -> [(Data, String)] {
        var results: [(Data, String)] = []
        for item in items {
            if let data = try? await item.loadTransferable(type: Data.self) {
                results.append((data, "image_\(UUID().uuidString).jpg"))
            }
        }
        return results
    }
}
