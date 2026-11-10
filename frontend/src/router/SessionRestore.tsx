import { useEffect, useState, type ReactNode } from 'react'
import { Loader2 } from 'lucide-react'
import api from '@/api/axios'
import type { LoginResponse } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'

/**
 * The access token lives only in memory, so a page reload used to log the user
 * out. On startup, try the httpOnly refresh cookie to restore the session before
 * any route decides the user is signed out.
 */
export function SessionRestore({ children }: { children: ReactNode }) {
  // Parent receipt links (/r/…) never need a staff session
  const [ready, setReady] = useState(() =>
    useAuthStore.getState().isAuthenticated || window.location.pathname.startsWith('/r/'))

  useEffect(() => {
    if (ready) return
    let cancelled = false
    api.post<LoginResponse>('/auth/refresh')
      .then(({ data }) => {
        if (!cancelled) useAuthStore.getState().setAuth(data.accessToken, data.user)
      })
      .catch(() => { /* no session — stay signed out */ })
      .finally(() => { if (!cancelled) setReady(true) })
    return () => { cancelled = true }
  }, [ready])

  if (!ready) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <Loader2 className="w-6 h-6 text-brand-600 animate-spin" aria-label="Loading" />
      </div>
    )
  }
  return <>{children}</>
}
