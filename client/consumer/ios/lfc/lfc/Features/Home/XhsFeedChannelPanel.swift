import SwiftUI

struct XhsFeedChannelPanel: View {
    let myChannels: [String]
    let recommendedChannels: [String]
    let isEditMode: Bool
    let onToggleEditMode: () -> Void
    let onEnterEditMode: () -> Void
    let onCollapse: () -> Void
    let onChannelClick: (String) -> Void
    let onAddChannel: (String) -> Void
    let onRemoveChannel: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            panelHeader

            FlowLayout(spacing: 10) {
                ForEach(myChannels, id: \.self) { channel in
                    let removable = isEditMode && channel != feedRecommendChannel
                    FeedChannelChip(
                        label: channel,
                        selected: true,
                        isEditMode: isEditMode,
                        showRemoveBadge: removable,
                        onTap: {
                            if !isEditMode { onChannelClick(channel) }
                        },
                        onLongPress: isEditMode ? nil : onEnterEditMode,
                        onRemove: removable ? { onRemoveChannel(channel) } : nil
                    )
                }
            }
            .padding(.horizontal, 12)

            HStack(spacing: 8) {
                Text("推荐频道")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(XhsTheme.textPrimary)
                Text("点击添加频道")
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            .padding(.horizontal, 14)
            .padding(.top, 18)
            .padding(.bottom, 8)

            FlowLayout(spacing: 10) {
                ForEach(recommendedChannels, id: \.self) { channel in
                    FeedChannelChip(
                        label: "+\(channel)",
                        selected: false,
                        isEditMode: false,
                        showRemoveBadge: false,
                        onTap: { onAddChannel(channel) }
                    )
                }
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 12)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(XhsTheme.background)
    }

    private var panelHeader: some View {
        HStack(alignment: .center, spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text("我的频道")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(XhsTheme.textPrimary)
                Text(isEditMode ? "点击 × 移除频道" : "长按进入编辑，点击进入频道")
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            Spacer(minLength: 8)
            Button(isEditMode ? "完成编辑" : "进入编辑") {
                onToggleEditMode()
            }
            .font(.system(size: 13))
            .foregroundStyle(XhsTheme.textSecondary)
            .simultaneousGesture(
                LongPressGesture(minimumDuration: 0.35).onEnded { _ in
                    onEnterEditMode()
                }
            )
            Button(action: onCollapse) {
                Image(systemName: "chevron.up")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
    }
}

private struct FeedChannelChip: View {
    let label: String
    let selected: Bool
    let isEditMode: Bool
    var showRemoveBadge: Bool = false
    let onTap: () -> Void
    var onLongPress: (() -> Void)? = nil
    var onRemove: (() -> Void)? = nil

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Text(label)
                .font(.system(size: 14))
                .foregroundStyle(XhsTheme.textPrimary)
                .lineLimit(1)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .frame(minWidth: 56, minHeight: 36)
                .background(selected ? Color(red: 0.96, green: 0.96, blue: 0.96) : Color.white)
                .overlay {
                    if !selected {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .stroke(Color(red: 0.91, green: 0.91, blue: 0.91), lineWidth: 0.5)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                .padding(.top, isEditMode ? 8 : 0)
                .contentShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                .onTapGesture(perform: onTap)
                .onLongPressGesture {
                    onLongPress?()
                }

            if showRemoveBadge, let onRemove {
                Button(action: onRemove) {
                    Image(systemName: "xmark")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 18, height: 18)
                        .background(Color(red: 0.74, green: 0.74, blue: 0.74))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .offset(x: 4, y: isEditMode ? 2 : -6)
            }
        }
    }
}
