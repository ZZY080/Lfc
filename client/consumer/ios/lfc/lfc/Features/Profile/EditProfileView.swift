import PhotosUI
import SwiftUI

struct EditProfileView: View {
    let profile: UserProfileDto?
    @Bindable var store: HomeStore
    let onBack: () -> Void

    @State private var nickname = ""
    @State private var bio = ""
    @State private var avatarItem: PhotosPickerItem?
    @State private var hydrated = false

    private var canSave: Bool {
        guard let profile else { return false }
        let originalBio = normalizeBio(profile.bio)
        return nickname.trimmingCharacters(in: .whitespacesAndNewlines) != (profile.nickname ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            || bio.trimmingCharacters(in: .whitespacesAndNewlines) != originalBio
            || avatarItem != nil
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
                Text("编辑资料")
                    .font(.system(size: 17, weight: .semibold))
                Spacer()
                Button("保存") {
                    Task {
                        var avatarPayload: ImageUploadPayload?
                        if let avatarItem {
                            let payloads = await PublishImageLoader.loadData(from: [avatarItem])
                            if let first = payloads.first {
                                avatarPayload = first
                            }
                        }
                        await store.updateProfile(
                            nickname: nickname,
                            bio: bio,
                            avatarData: avatarPayload,
                            onSuccess: onBack
                        )
                    }
                }
                .foregroundStyle(canSave ? XhsTheme.red : XhsTheme.textSecondary)
                .disabled(!canSave || store.isProfileUpdating)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.white)

            if profile == nil {
                EditProfileSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 20) {
                        PhotosPicker(selection: $avatarItem, matching: .images) {
                            ZStack(alignment: .bottomTrailing) {
                                profileAvatar
                                Image(systemName: "camera.fill")
                                    .font(.system(size: 12))
                                    .foregroundStyle(.white)
                                    .padding(6)
                                    .background(Color.black.opacity(0.55))
                                    .clipShape(Circle())
                            }
                        }

                        VStack(spacing: 0) {
                            profileField("名字", text: $nickname, placeholder: "填写名字")
                            Divider().padding(.leading, 16)
                            HStack {
                                Text("莲峰号")
                                Spacer()
                                Text(profile?.lfcNo ?? "—")
                                    .foregroundStyle(XhsTheme.textSecondary)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            Divider().padding(.leading, 16)
                            profileField("简介", text: $bio, placeholder: "填写简介", multiline: true)
                        }
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 16)

                        Text("莲峰号用于搜索与分享，暂不支持修改")
                            .font(.system(size: 12))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .padding(.horizontal, 20)
                    }
                    .padding(.top, 24)
                }
                .background(XhsTheme.background)
            }
        }
        .onAppear { hydrateIfNeeded() }
        .onChange(of: profile?.id) { _, _ in
            hydrated = false
            hydrateIfNeeded()
        }
    }

    @ViewBuilder
    private var profileAvatar: some View {
        if let avatarItem {
            AsyncImage(url: nil) { _ in
                Circle().fill(Color.gray.opacity(0.2))
            }
            .frame(width: 88, height: 88)
            .overlay {
                Text(profile?.displayName.prefix(1) ?? "?")
                    .font(.title)
            }
            .clipShape(Circle())
        } else if let url = profile?.avatarUrl, let imageURL = URL(string: url) {
            AsyncImage(url: imageURL) { phase in
                if case .success(let image) = phase {
                    image.resizable().scaledToFill()
                } else {
                    avatarPlaceholder
                }
            }
            .frame(width: 88, height: 88)
            .clipShape(Circle())
        } else {
            avatarPlaceholder
        }
    }

    private var avatarPlaceholder: some View {
        Circle()
            .fill(XhsTheme.red.opacity(0.15))
            .frame(width: 88, height: 88)
            .overlay {
                Text(profile?.displayName.prefix(1) ?? "?")
                    .font(.title2.bold())
                    .foregroundStyle(XhsTheme.red)
            }
    }

    private func profileField(_ label: String, text: Binding<String>, placeholder: String, multiline: Bool = false) -> some View {
        HStack(alignment: .top) {
            Text(label)
            Spacer()
            if multiline {
                TextField(placeholder, text: text, axis: .vertical)
                    .multilineTextAlignment(.trailing)
                    .lineLimit(2...4)
            } else {
                TextField(placeholder, text: text)
                    .multilineTextAlignment(.trailing)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    private func hydrateIfNeeded() {
        guard !hydrated, let profile else { return }
        nickname = profile.nickname ?? ""
        bio = normalizeBio(profile.bio)
        avatarItem = nil
        hydrated = true
    }

    private func normalizeBio(_ raw: String?) -> String {
        let value = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return value == "莲峰校园 · 记录校园生活" ? "" : value
    }
}
