import SwiftUI

struct LegalConsentView: View {
    let onAgreed: () -> Void
    let onOpenDocument: (LegalDocumentId) -> Void

    private let accent = Color(red: 0.96, green: 0.15, blue: 0.24)

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("欢迎使用莲峰校园")
                        .font(.title.bold())
                        .foregroundStyle(.primary)

                    Text("为向您提供笔记分享、活动组织、闲置交易等服务，我们需要收集并使用必要的信息。请阅读并同意以下协议：")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineSpacing(4)

                    VStack(alignment: .leading, spacing: 10) {
                        consentLink(.userAgreement)
                        consentLink(.privacyPolicy)
                        consentLink(.communityGuidelines)
                        consentLink(.c2cSecurity)
                        consentLink(.c2cAfterSales)
                        consentLink(.promotionAds)
                        consentLink(.paymentTerms)
                        consentLink(.thirdPartySDK)
                    }
                    .padding(16)
                    .background(Color(.systemGray6))
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    Text("我们将严格保护您的个人信息安全。您可在「设置 - 法律与隐私」中随时查阅上述文件。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineSpacing(3)

                    Text("协议版本 v\(LegalConsentStore.currentVersion).0")
                        .font(.caption2)
                        .foregroundStyle(Color(.systemGray3))
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 32)
            }

            VStack(spacing: 8) {
                Button(action: onAgreed) {
                    Text("同意并继续")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                }
                .buttonStyle(.borderedProminent)
                .tint(accent)
                .clipShape(Capsule())

                Button("不同意并退出") {
                    exit(0)
                }
                .font(.subheadline)
                .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
        .background(Color(.systemBackground))
    }

    private func consentLink(_ documentId: LegalDocumentId) -> some View {
        Button {
            onOpenDocument(documentId)
        } label: {
            Text(documentId.consentLinkTitle)
                .font(.body.weight(.medium))
                .foregroundStyle(accent)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    LegalConsentView(onAgreed: {}, onOpenDocument: { _ in })
}
