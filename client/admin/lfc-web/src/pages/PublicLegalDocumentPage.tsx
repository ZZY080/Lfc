import { Link, useParams } from 'react-router-dom'
import {
  getLegalDocumentBySlug,
  PUBLIC_LEGAL_DOCUMENTS,
} from '../content/legal'
import { PublicLegalPage } from './PublicLegalPage'

export function PublicLegalDocumentPage() {
  const { slug } = useParams<{ slug: string }>()
  const document = slug ? getLegalDocumentBySlug(slug) : undefined

  if (!document) {
    return (
      <div className="legal-public-page">
        <main className="legal-public-main">
          <p>未找到该法律文档。</p>
          <Link to="/privacy">返回隐私政策</Link>
        </main>
      </div>
    )
  }

  const relatedLinks = PUBLIC_LEGAL_DOCUMENTS
    .filter((item) => item.slug !== document.slug)
    .map((item) => ({ slug: item.slug, title: item.title }))

  return <PublicLegalPage legalDocument={document} relatedLinks={relatedLinks} />
}

export function PrivacyPolicyPage() {
  const document = getLegalDocumentBySlug('privacy')
  if (!document) return null

  const relatedLinks = PUBLIC_LEGAL_DOCUMENTS
    .filter((item) => item.slug !== 'privacy')
    .map((item) => ({ slug: item.slug, title: item.title }))

  return <PublicLegalPage legalDocument={document} relatedLinks={relatedLinks} />
}
