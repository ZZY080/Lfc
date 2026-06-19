import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import type { LegalDocument } from '../content/legal/types'
import { LegalDocumentView } from '../components/LegalDocumentView'
import '../components/LegalDocumentView.css'
import './PrivacyPolicyPage.css'

interface PublicLegalPageProps {
  legalDocument: LegalDocument
  relatedLinks?: { slug: string; title: string }[]
}

export function PublicLegalPage({ legalDocument, relatedLinks = [] }: PublicLegalPageProps) {
  useEffect(() => {
    window.document.title = `${legalDocument.appName} - ${legalDocument.title}`
    return () => {
      window.document.title = 'LFC 管理后台'
    }
  }, [legalDocument])

  return (
    <div className="legal-public-page">
      <header className="legal-public-header">
        <div className="legal-public-brand">
          <span className="legal-public-mark">LFC</span>
          <span>{legalDocument.appName}</span>
        </div>
        <Link to="/login" className="legal-public-login">
          管理后台登录
        </Link>
      </header>

      <main className="legal-public-main">
        {relatedLinks.length > 0 && (
          <nav className="legal-related-nav" aria-label="相关法律文档">
            {relatedLinks.map((item) => (
              <Link key={item.slug} to={`/legal/${item.slug}`}>
                {item.title}
              </Link>
            ))}
          </nav>
        )}
        <LegalDocumentView document={legalDocument} />
      </main>

      <footer className="legal-public-footer">
        <p>© {new Date().getFullYear()} 莲峰校园 · 本页面供 App 用户查阅及上架备案使用</p>
      </footer>
    </div>
  )
}
