import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { PUBLIC_LEGAL_DOCUMENTS } from '../content/legal'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../hooks/useToast'

export function LegalHubAdminPage() {
  const toast = useToast()
  const origin = window.location.origin

  async function copyUrl(path: string) {
    try {
      await navigator.clipboard.writeText(`${origin}${path}`)
      toast.success('链接已复制')
    } catch {
      toast.error('复制失败，请手动复制')
    }
  }

  const documents = useMemo(
    () =>
      PUBLIC_LEGAL_DOCUMENTS.map((doc) => ({
        ...doc,
        path: doc.slug === 'privacy' ? '/privacy' : `/legal/${doc.slug}`,
      })),
    [],
  )

  return (
    <div className="page">
      <PageHeader
        title="法律文档"
        description="莲峰校园 App 公开法律文本，可用于上架备案、用户查阅及 C2C 交易保障说明"
      />

      <div className="manage-list">
        {documents.map((doc) => (
          <article key={doc.slug} className="manage-card legal-admin-card">
            <div className="manage-card-header">
              <div>
                <h3 className="manage-card-title">{doc.title}</h3>
                <p className="manage-meta">
                  {doc.description} · 更新：{doc.updatedAt}
                </p>
              </div>
            </div>

            <div className="legal-url-row">
              <code className="legal-url">{origin}{doc.path}</code>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => void copyUrl(doc.path)}
              >
                复制链接
              </button>
              <Link
                to={doc.path}
                target="_blank"
                rel="noreferrer"
                className="btn btn-ghost btn-sm"
              >
                预览
              </Link>
              <Link to={`/legal/manage/${doc.slug}`} className="btn btn-ghost btn-sm">
                管理预览
              </Link>
            </div>
          </article>
        ))}
      </div>

      <div className="manage-card legal-admin-card">
        <h3 className="manage-card-title">上架常用链接</h3>
        <p className="manage-meta">
          应用商店「隐私政策 URL」一般填写隐私政策地址；用户协议、售后规则等可在 App 内「设置 - 法律与隐私」引用上述公开页。
        </p>
        <ul className="legal-hub-tips">
          <li>隐私政策：{origin}/privacy</li>
          <li>C2C 安全保障：{origin}/legal/c2c-security</li>
          <li>退货售后规则：{origin}/legal/c2c-after-sales</li>
          <li>推广服务说明：{origin}/legal/promotion</li>
        </ul>
      </div>
    </div>
  )
}
