import SwiftUI

struct ProfileQrScanView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onProfileScanned: (Int) -> Void
    let onShowMyQr: () -> Void

    @State private var lfcNoInput = ""
    @State private var isLookingUp = false

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Color.black.ignoresSafeArea()
                VStack(spacing: 24) {
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.white.opacity(0.8), lineWidth: 2)
                        .frame(width: 220, height: 220)
                        .overlay {
                            Image(systemName: "qrcode.viewfinder")
                                .font(.system(size: 64))
                                .foregroundStyle(.white.opacity(0.85))
                        }
                    Text("输入莲峰号查找用户")
                        .font(.system(size: 14))
                        .foregroundStyle(.white.opacity(0.9))
                    TextField("10 位莲峰号", text: $lfcNoInput)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                        .padding(.horizontal, 40)
                    Button("查找用户") {
                        lookup()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(XhsTheme.red)
                    .disabled(isLookingUp || lfcNoInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    if isLookingUp {
                        ProgressView().tint(.white)
                    }
                    Text("也可扫描用户主页二维码（完整扫码能力后续接入）")
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            }
            .overlay(alignment: .topLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .foregroundStyle(.white)
                        .padding()
                }
            }
            .overlay(alignment: .top) {
                Text("扫一扫")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.top, 16)
            }
            .overlay(alignment: .bottom) {
                Button("我的二维码", action: onShowMyQr)
                    .foregroundStyle(.white)
                    .padding(.bottom, 32)
            }
        }
    }

    private func lookup() {
        let raw = lfcNoInput.trimmingCharacters(in: .whitespacesAndNewlines)
        let lfcNo = ProfileShareHelper.parseLfcNo(from: raw) ?? raw
        if let userId = ProfileShareHelper.parseUserId(from: raw) {
            onProfileScanned(userId)
            return
        }
        isLookingUp = true
        Task {
            if let profile = await store.lookupProfileByLfcNo(lfcNo) {
                onProfileScanned(profile.id)
            }
            isLookingUp = false
        }
    }
}
