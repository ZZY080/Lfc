import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import './Layout.css'

const NAV_ITEMS: { to: string; label: string; end?: boolean }[] = [
  { to: '/', label: '数据概览', end: true },
  { to: '/activities', label: '活动管理' },
  { to: '/posts', label: '笔记管理' },
  { to: '/comments', label: '评论管理' },
  { to: '/payments', label: '支付管理' },
  { to: '/users', label: '用户管理' },
  { to: '/legal', label: '法律文档' },
]

export function Layout() {
  const { user, logout } = useAuth()

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">LFC</span>
          <span className="brand-text">管理后台</span>
        </div>

        <nav className="nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? 'active' : '')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <span className="user-email">{user?.email}</span>
            <span className="user-role">管理员</span>
          </div>
          <button type="button" className="btn btn-ghost" onClick={() => void logout()}>
            退出登录
          </button>
        </div>
      </aside>

      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
