import SwiftUI

struct MyQrCodeView: View {
    let profile: UserProfileDto?
    let onBack: () -> Void

    private var link: String {
        guard let lfcNo = profile?.lfcNo else { return "" }
        return ProfileShareHelper.profileLink(lfcNo: lfcNo)
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.24, green: 0.33, blue: 0.38), Color(red: 0.1, green: 0.14, blue: 0.16)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .foregroundStyle(.white)
                            .padding(8)
                    }
                    Spacer()
                }
                .padding(.horizontal, 8)

                Spacer()

                if let profile {
                    avatarView(profile)
                    Text(profile.displayName)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.top, 16)
                    Text("莲峰号：\(profile.lfcNo)")
                        .font(.system(size: 14))
                        .foregroundStyle(.white.opacity(0.78))
                        .padding(.top, 4)
                    if let bio = profile.bio, !bio.isEmpty {
                        Text(bio)
                            .font(.system(size: 13))
                            .foregroundStyle(.white.opacity(0.72))
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                            .padding(.horizontal, 32)
                            .padding(.top, 8)
                    }

                    VStack(spacing: 12) {
                        if let qr = ProfileShareHelper.generateQRCode(from: link, size: 240) {
                            Image(uiImage: qr)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 220, height: 220)
                        }
                        Text("扫一扫，查看我的莲峰校园主页")
                            .font(.system(size: 14))
                            .foregroundStyle(XhsTheme.textPrimary)
                        Text(link)
                            .font(.system(size: 11))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(24)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .padding(.horizontal, 32)
                    .padding(.top, 28)
                } else {
                    QrCodeSkeleton()
                }

                Spacer()
            }
        }
    }

    @ViewBuilder
    private func avatarView(_ profile: UserProfileDto) -> some View {
        if let url = profile.avatarUrl, let imageURL = URL(string: url) {
            AsyncImage(url: imageURL) { phase in
                if case .success(let image) = phase {
                    image.resizable().scaledToFill()
                } else {
                    placeholderAvatar(profile)
                }
            }
            .frame(width: 72, height: 72)
            .clipShape(Circle())
        } else {
            placeholderAvatar(profile)
        }
    }

    private func placeholderAvatar(_ profile: UserProfileDto) -> some View {
        Circle()
            .fill(Color.white.opacity(0.2))
            .frame(width: 72, height: 72)
            .overlay {
                Text(profile.displayName.prefix(1))
                    .font(.title.bold())
                    .foregroundStyle(.white)
            }
    }
}
