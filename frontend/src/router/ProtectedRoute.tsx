import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const mustChangePassword = useAuthStore((s) => s.user?.mustChangePassword)
  const { pathname } = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  // Accounts created with an emailed temporary password must set their own first
  if (mustChangePassword && pathname !== '/set-password') {
    return <Navigate to="/set-password" replace />
  }
  return <Outlet />
}
