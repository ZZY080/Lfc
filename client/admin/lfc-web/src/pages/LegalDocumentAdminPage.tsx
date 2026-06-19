import { Link, useParams } from 'react-router-dom'
import { getLegalDocumentBySlug } from '../content/legal'
import { LegalDocumentView } from '../components/LegalDocumentView'
import { PageHeader } from '../components/PageHeader'
import '../components/LegalDocumentView.css'

export function LegalDocumentAdminPage() {
  const { slug } = useParams<{ slug: string }>()
  const document = slug ? getLegalDocumentBySlug(slug) : undefined

  if (!document) {
    return (
      <div className="page">
        <div className="page-state">文档不存在</div>
        <Link to="/legal">返回法律文档</Link>
      </div>
    )
  }

  const publicPath =
    document.slug === 'privacy' ? '/privacy' : `/legal/${document.slug}`

  return (
    <div className="page">
      <PageHeader title={document.title} description={document.description} />

      <div className="manage-card legal-admin-card">
        <div className="legal-url-row">
          <code className="legal-url">
            {window.location.origin}{publicPath}
          </code>
          <Link
            to={publicPath}
            target="_blank"
            rel="noreferrer"
            className="btn btn-ghost btn-sm"
          >
            公开页预览
          </Link>
          <Link to="/legal" className="btn btn-ghost btn-sm">
            返回列表
          </Link>
        </div>
      </div>

      <div className="manage-card legal-admin-preview">
        <LegalDocumentView document={document} />
      </div>
    </div>
  )
}
