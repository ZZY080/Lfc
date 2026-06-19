import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  deleteUser,
  fetchUsers,
  updateUserStatus,
} from '../api/user'
import { ApiError } from '../api/client'
import { Pagination } from '../components/Pagination'
import { CreateUserModal } from '../components/CreateUserModal'
import { EditUserModal } from '../components/EditUserModal'
import { PageHeader } from '../components/PageHeader'
import { RowActions } from '../components/RowActions'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { User, UserRole, UserStatus } from '../types'
import { formatDateTime } from '../utils/format'

const ROLE_LABELS: Record<UserRole, string> = {
  CONSUMER: '普通用户',
  ADMIN: '管理员',
}

const STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: '正常',
  BANNED: '已封禁',
}

export function UsersPage() {
  const { token, user: currentUser } = useAuth()
  const toast = useToast()
  const [users, setUsers] = useState<User[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [limit] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('')
  const [statusFilter, setStatusFilter] = useState<UserStatus | ''>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)

  const loadUsers = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchUsers(token, {
        page,
        limit,
        keyword: keyword || undefined,
        role: roleFilter || undefined,
        status: statusFilter || undefined,
      })
      setUsers(result.items)
      setTotal(result.total)
      setHasMore(result.hasMore)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载用户列表失败')
    } finally {
      setLoading(false)
    }
  }, [token, page, limit, keyword, roleFilter, statusFilter])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadUsers()
  }, [loadUsers])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(1)
    setKeyword(searchInput.trim())
  }

  async function handleBan(user: User) {
    if (!token) return
    const reason = window.prompt('请输入封禁原因', user.banReason ?? '违反平台规定')
    if (reason === null) return
    try {
      await updateUserStatus(token, user.id, 'BANNED', reason)
      toast.success('用户已封禁')
      void loadUsers()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '封禁失败')
    }
  }

  async function handleUnban(user: User) {
    if (!token) return
    try {
      await updateUserStatus(token, user.id, 'ACTIVE')
      toast.success('已解除封禁')
      void loadUsers()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '解封失败')
    }
  }

  async function handleDelete(user: User) {
    if (!token) return
    if (!window.confirm(`确定删除用户「${user.realName}」吗？此操作不可恢复。`)) return
    try {
      await deleteUser(token, user.id)
      toast.success('用户已删除')
      void loadUsers()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="用户管理"
        description="增删改查、封禁违规用户"
        loading={loading}
        onRefresh={() => void loadUsers()}
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setCreateOpen(true)}>
            创建账号
          </button>
        }
      />

      <div className="toolbar">
        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="搜索邮箱、学号、姓名、昵称…"
          />
          <button type="submit" className="btn btn-primary btn-sm">搜索</button>
        </form>
        <select
          className="filter-select"
          value={roleFilter}
          onChange={(e) => { setPage(1); setRoleFilter(e.target.value as UserRole | '') }}
        >
          <option value="">全部角色</option>
          <option value="CONSUMER">普通用户</option>
          <option value="ADMIN">管理员</option>
        </select>
        <select
          className="filter-select"
          value={statusFilter}
          onChange={(e) => { setPage(1); setStatusFilter(e.target.value as UserStatus | '') }}
        >
          <option value="">全部状态</option>
          <option value="ACTIVE">正常</option>
          <option value="BANNED">已封禁</option>
        </select>
      </div>

      {loading && <div className="page-state">加载中…</div>}
      {!loading && error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>邮箱</th>
                <th>学号</th>
                <th>姓名</th>
                <th>角色</th>
                <th>状态</th>
                <th>注册时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr><td colSpan={8} className="empty-cell">暂无用户</td></tr>
              ) : (
                users.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td className="cell-email">{user.email}</td>
                    <td>{user.studentId}</td>
                    <td>{user.realName}</td>
                    <td>{ROLE_LABELS[user.role]}</td>
                    <td>
                      <span className={`status-pill ${user.status === 'ACTIVE' ? 'success' : 'danger'}`}>
                        {STATUS_LABELS[user.status]}
                      </span>
                      {user.banReason && user.status === 'BANNED' && (
                        <div className="ban-reason">{user.banReason}</div>
                      )}
                    </td>
                    <td>{formatDateTime(user.createdAt)}</td>
                    <td>
                      <RowActions
                        actions={[
                          {
                            label: '编辑',
                            variant: 'primary',
                            onClick: () => setEditingUser(user),
                          },
                          user.status === 'BANNED'
                            ? {
                                label: '解封',
                                onClick: () => void handleUnban(user),
                                disabled: user.id === currentUser?.id,
                              }
                            : {
                                label: '封禁',
                                variant: 'danger',
                                onClick: () => void handleBan(user),
                                disabled:
                                  user.id === currentUser?.id || user.role === 'ADMIN',
                              },
                          {
                            label: '删除',
                            variant: 'danger',
                            onClick: () => void handleDelete(user),
                            disabled:
                              user.id === currentUser?.id || user.role === 'ADMIN',
                          },
                        ]}
                      />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          <Pagination page={page} total={total} limit={limit} hasMore={hasMore} onPageChange={setPage} />
        </div>
      )}

      <CreateUserModal open={createOpen} onClose={() => setCreateOpen(false)} onCreated={() => void loadUsers()} />
      <EditUserModal
        key={editingUser?.id ?? 'closed'}
        user={editingUser}
        onClose={() => setEditingUser(null)}
        onSaved={() => void loadUsers()}
      />
    </div>
  )
}
