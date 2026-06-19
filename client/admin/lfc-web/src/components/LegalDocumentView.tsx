import type { LegalDocument } from '../content/legal/types'
import './LegalDocumentView.css'

interface LegalDocumentViewProps {
  document: LegalDocument
  showMeta?: boolean
}

export function LegalDocumentView({ document, showMeta = true }: LegalDocumentViewProps) {
  return (
    <article className="legal-document">
      {showMeta && (
        <header className="legal-document-header">
          <p className="legal-document-app">{document.appName} App</p>
          <h1>{document.title}</h1>
          <p className="legal-document-updated">更新日期：{document.updatedAt}</p>
        </header>
      )}

      {document.sections.map((section) => (
        <section key={section.heading} className="legal-section">
          <h2>{section.heading}</h2>
      {section.paragraphs.map((paragraph, index) =>
        paragraph === '' ? (
          <br key={`${section.heading}-sp-${index}`} />
        ) : (
          <p key={`${section.heading}-p-${index}`}>{paragraph}</p>
        ),
      )}
        </section>
      ))}
    </article>
  )
}
