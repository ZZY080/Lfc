import SwiftUI

struct LegalAgreementCheckbox: View {
    @Binding var isChecked: Bool
    let onOpenDocument: (LegalDocumentId) -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Toggle(isOn: $isChecked) {
                EmptyView()
            }
            .toggleStyle(.checkbox)
            .labelsHidden()

            Text(attributedAgreement)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .environment(\.openURL, OpenURLAction { url in
                    if let id = LegalDocumentId.fromRouteKey(url.absoluteString) {
                        onOpenDocument(id)
                        return .handled
                    }
                    return .systemAction
                })
        }
    }

    private var attributedAgreement: AttributedString {
        var result = AttributedString("我已阅读并同意 ")
        result.foregroundColor = .secondary

        result.append(link(LegalDocumentId.userAgreement))
        result.append(AttributedString("、"))
        result.append(link(LegalDocumentId.privacyPolicy))
        result.append(AttributedString(" 及 "))
        result.append(link(LegalDocumentId.communityGuidelines))

        return result
    }

    private func link(_ documentId: LegalDocumentId) -> AttributedString {
        var text = AttributedString(documentId.consentLinkTitle)
        text.link = URL(string: documentId.routeKey)
        text.foregroundColor = Color(red: 0.96, green: 0.15, blue: 0.24)
        text.font = .footnote.weight(.medium)
        return text
    }
}

private struct CheckboxToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                .foregroundStyle(configuration.isOn ? Color(red: 0.96, green: 0.15, blue: 0.24) : .secondary)
                .font(.title3)
        }
        .buttonStyle(.plain)
    }
}

private extension ToggleStyle where Self == CheckboxToggleStyle {
    static var checkbox: CheckboxToggleStyle { CheckboxToggleStyle() }
}
