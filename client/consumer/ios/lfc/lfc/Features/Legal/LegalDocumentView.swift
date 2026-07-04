import SwiftUI

struct LegalDocumentView: View {
    let documentId: LegalDocumentId
    let onBack: () -> Void

    private var document: LegalDocument {
        legalDocument(of: documentId)
    }

    var body: some View {
        VStack(spacing: 0) {
            HomeStubNavigationBar(title: document.title, onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("更新日期：\(document.updatedAt)")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    ForEach(document.sections, id: \.heading) { section in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(section.heading)
                                .font(.headline)
                                .foregroundStyle(.primary)

                            ForEach(section.paragraphs, id: \.self) { paragraph in
                                Text(paragraph)
                                    .font(.subheadline)
                                    .foregroundStyle(.primary)
                                    .lineSpacing(4)
                                    .padding(.bottom, 4)
                            }
                        }
                        .padding(.bottom, 8)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
            }
        }
        .background(Color(.systemBackground))
    }
}

#Preview {
    LegalDocumentView(documentId: .privacyPolicy, onBack: {})
}
