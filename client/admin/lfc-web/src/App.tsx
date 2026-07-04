import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthSessionGuard } from './components/AuthSessionGuard'
import { Layout } from './components/Layout'
import { GuestRoute, ProtectedRoute } from './components/ProtectedRoute'
import { ToastProvider } from './components/Toast'
import { AuthProvider } from './context/AuthContext'
import {
  ActivitiesPage,
  ActivityDetailPage,
  AnalyticsPage,
  DashboardPage,
  CommentsPage,
  LegalDocumentAdminPage,
  LegalHubAdminPage,
  PaymentsPage,
  PostDetailPage,
  PostsPage,
  PrivacyPolicyPage,
  UsersPage,
} from './pages'
import { PublicLegalDocumentPage } from './pages/PublicLegalDocumentPage'
import { LoginPage } from './pages/LoginPage'

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <AuthSessionGuard />
          <Routes>
            <Route element={<GuestRoute />}>
              <Route path="/login" element={<LoginPage />} />
            </Route>

            <Route path="/privacy" element={<PrivacyPolicyPage />} />
            <Route path="/privacy-policy" element={<Navigate to="/privacy" replace />} />
            <Route path="/legal/:slug" element={<PublicLegalDocumentPage />} />

            <Route element={<ProtectedRoute />}>
              <Route element={<Layout />}>
                <Route index element={<DashboardPage />} />
                <Route path="analytics" element={<AnalyticsPage />} />
                <Route path="activities/pending" element={<Navigate to="/activities?status=PENDING" replace />} />
                <Route path="activities" element={<ActivitiesPage />} />
                <Route path="activities/:activityId" element={<ActivityDetailPage />} />
                <Route path="posts/pending" element={<Navigate to="/posts?status=PENDING" replace />} />
                <Route path="posts" element={<PostsPage />} />
              <Route path="posts/:postId" element={<PostDetailPage />} />
              <Route path="comments" element={<CommentsPage />} />
              <Route path="orders" element={<Navigate to="/payments?tab=orders" replace />} />
              <Route path="after-sales" element={<Navigate to="/payments?tab=after-sales&status=PENDING" replace />} />
              <Route path="payments" element={<PaymentsPage />} />
                <Route path="legal" element={<LegalHubAdminPage />} />
                <Route path="legal/privacy" element={<Navigate to="/legal" replace />} />
                <Route path="legal/manage/:slug" element={<LegalDocumentAdminPage />} />
                <Route path="users" element={<UsersPage />} />
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
