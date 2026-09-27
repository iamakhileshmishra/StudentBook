import { useState, FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { GraduationCap, KeyRound, Loader2 } from 'lucide-react'
import { settingsApi } from '@/api/settings'
import { useAuthStore } from '@/store/authStore'

/** First-login screen for accounts created with an emailed temporary password. */
export function SetPasswordPage() {
  const user = useAuthStore((s) => s.user)
  const clearMustChangePassword = useAuthStore((s) => s.clearMustChangePassword)
  const navigate = useNavigate()

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (!user?.mustChangePassword) {
    return <Navigate to="/dashboard" replace />
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match')
      return
    }
    setLoading(true)
    try {
      await settingsApi.changePassword({ currentPassword, newPassword })
      clearMustChangePassword()
      navigate('/dashboard', { replace: true })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message ?? 'Could not update password. Please try again.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 sm:p-8 bg-slate-50">
      <div className="w-full max-w-md card p-6 sm:p-8 animate-fade-in">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-xl bg-brand-600 flex items-center justify-center">
            <GraduationCap className="w-5 h-5 text-white" />
          </div>
          <span className="text-slate-900 font-bold text-lg">School ERP</span>
        </div>

        <div className="flex items-start gap-3 mb-6">
          <KeyRound className="w-5 h-5 text-brand-600 mt-1 flex-shrink-0" />
          <div>
            <h1 className="text-xl font-bold text-slate-900">Set your password</h1>
            <p className="text-sm text-slate-500 mt-1">
              Welcome, {user.fullName}. You signed in with a temporary password — choose a new one to continue.
            </p>
          </div>
        </div>

        <div className="mb-6 px-4 py-3 rounded-lg bg-slate-50 border border-slate-200">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Your User ID</p>
          <p className="font-mono text-slate-900 mt-0.5">{user.username}</p>
          <p className="text-xs text-slate-500 mt-1">This is permanent — keep it for signing in.</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="current-password" className="label">Temporary password</label>
            <input
              id="current-password" type="password" autoComplete="current-password" autoFocus
              className="input-field" value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)} disabled={loading}
            />
          </div>
          <div>
            <label htmlFor="new-password" className="label">New password</label>
            <input
              id="new-password" type="password" autoComplete="new-password"
              className="input-field" value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)} disabled={loading}
            />
            <p className="text-xs text-slate-500 mt-1">At least 8 characters.</p>
          </div>
          <div>
            <label htmlFor="confirm-password" className="label">Confirm new password</label>
            <input
              id="confirm-password" type="password" autoComplete="new-password"
              className="input-field" value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)} disabled={loading}
            />
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3">
              <p className="text-red-700 text-sm">{error}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={loading || !currentPassword || !newPassword || !confirmPassword}
            className="btn-primary w-full justify-center py-2.5"
          >
            {loading ? <><Loader2 className="w-4 h-4 animate-spin" /> Saving...</> : 'Set Password & Continue'}
          </button>
        </form>
      </div>
    </div>
  )
}
