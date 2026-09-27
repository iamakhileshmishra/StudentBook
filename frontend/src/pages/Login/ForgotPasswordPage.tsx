import { useEffect, useState, FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, GraduationCap, KeyRound, Loader2, Mail, ShieldCheck } from 'lucide-react'
import { authApi } from '@/api/auth'

type Step = 'email' | 'otp' | 'password' | 'done'

const RESEND_COOLDOWN_SECONDS = 60

function errorMessage(err: unknown, fallback: string) {
  const data = (err as { response?: { data?: { message?: string; fieldErrors?: Record<string, string> } } })
    ?.response?.data
  const firstFieldError = data?.fieldErrors && Object.values(data.fieldErrors)[0]
  return firstFieldError || data?.message || fallback
}

export function ForgotPasswordPage() {
  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(false)
  const [cooldown, setCooldown] = useState(0)

  useEffect(() => {
    if (cooldown <= 0) return
    const t = setTimeout(() => setCooldown((c) => c - 1), 1000)
    return () => clearTimeout(t)
  }, [cooldown])

  const run = async (action: () => Promise<void>, fallback: string) => {
    setError('')
    setNotice('')
    setLoading(true)
    try {
      await action()
    } catch (err) {
      setError(errorMessage(err, fallback))
    } finally {
      setLoading(false)
    }
  }

  const sendCode = (e?: FormEvent) => {
    e?.preventDefault()
    return run(async () => {
      await authApi.forgotPassword(email.trim())
      setOtp('')
      setStep('otp')
      setCooldown(RESEND_COOLDOWN_SECONDS)
      if (step === 'otp') setNotice('A new code has been sent.')
    }, 'Could not send the code. Please try again.')
  }

  const verifyCode = (e: FormEvent) => {
    e.preventDefault()
    return run(async () => {
      await authApi.verifyResetCode(email.trim(), otp)
      setStep('password')
    }, 'Could not verify the code. Please try again.')
  }

  const resetPassword = (e: FormEvent) => {
    e.preventDefault()
    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match')
      return
    }
    return run(async () => {
      await authApi.resetPassword(email.trim(), otp, newPassword)
      setStep('done')
    }, 'Could not reset your password. Please try again.')
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 sm:p-8 bg-slate-50">
      <div className="w-full max-w-md card p-6 sm:p-8 animate-fade-in">
        <Link to="/" className="flex items-center gap-3 mb-6 w-fit">
          <div className="w-10 h-10 rounded-xl bg-brand-600 flex items-center justify-center">
            <GraduationCap className="w-5 h-5 text-white" />
          </div>
          <span className="text-slate-900 font-bold text-lg">School ERP</span>
        </Link>

        {step !== 'done' && <StepIndicator step={step} />}

        {step === 'email' && (
          <>
            <Heading icon={Mail} title="Forgot your password?">
              Enter the email address registered for your school. We'll send you a 6-digit verification code.
            </Heading>
            <form onSubmit={sendCode} className="space-y-4">
              <div>
                <label htmlFor="email" className="label">Registered email</label>
                <input
                  id="email" type="email" autoComplete="email" autoFocus required
                  className="input-field" value={email} placeholder="you@school.com"
                  onChange={(e) => setEmail(e.target.value)} disabled={loading}
                />
              </div>
              <ErrorBox error={error} />
              <button type="submit" disabled={loading || !email.trim()} className="btn-primary w-full justify-center py-2.5">
                {loading ? <><Loader2 className="w-4 h-4 animate-spin" /> Sending...</> : 'Send Code'}
              </button>
            </form>
          </>
        )}

        {step === 'otp' && (
          <>
            <Heading icon={ShieldCheck} title="Enter verification code">
              If an account is registered with <strong className="text-slate-700">{email.trim()}</strong>, we've
              sent a 6-digit code to it. The code expires in 10 minutes.
            </Heading>
            <form onSubmit={verifyCode} className="space-y-4">
              <div>
                <label htmlFor="otp" className="label">Verification code</label>
                <input
                  id="otp" inputMode="numeric" autoComplete="one-time-code" autoFocus maxLength={6}
                  className="input-field text-center text-2xl font-mono tracking-[0.5em]" value={otp}
                  placeholder="••••••"
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} disabled={loading}
                />
              </div>
              <ErrorBox error={error} />
              {notice && <p className="text-sm text-emerald-700">{notice}</p>}
              <button type="submit" disabled={loading || otp.length !== 6} className="btn-primary w-full justify-center py-2.5">
                {loading ? <><Loader2 className="w-4 h-4 animate-spin" /> Verifying...</> : 'Verify Code'}
              </button>
              <div className="flex items-center justify-between text-sm">
                <button
                  type="button" className="text-slate-500 hover:text-slate-700"
                  onClick={() => { setStep('email'); setError(''); setNotice('') }}
                >
                  Change email
                </button>
                <button
                  type="button" onClick={() => sendCode()} disabled={loading || cooldown > 0}
                  className="font-medium text-brand-600 hover:text-brand-700 disabled:text-slate-400 disabled:cursor-not-allowed"
                >
                  {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
                </button>
              </div>
            </form>
          </>
        )}

        {step === 'password' && (
          <>
            <Heading icon={KeyRound} title="Set a new password">
              Code verified. Choose a new password for your account.
            </Heading>
            <form onSubmit={resetPassword} className="space-y-4">
              <div>
                <label htmlFor="new-password" className="label">New password</label>
                <input
                  id="new-password" type="password" autoComplete="new-password" autoFocus
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
              <ErrorBox error={error} />
              <button
                type="submit" disabled={loading || !newPassword || !confirmPassword}
                className="btn-primary w-full justify-center py-2.5"
              >
                {loading ? <><Loader2 className="w-4 h-4 animate-spin" /> Saving...</> : 'Reset Password'}
              </button>
            </form>
          </>
        )}

        {step === 'done' && (
          <div className="text-center py-4">
            <div className="w-14 h-14 mx-auto rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-7 h-7" />
            </div>
            <h1 className="mt-5 text-xl font-bold text-slate-900">Password reset</h1>
            <p className="mt-2 text-sm text-slate-600">
              Your password has been changed. You can now sign in with your User ID and new password.
            </p>
            <Link to="/login" className="btn-primary inline-flex mt-6 px-6">Go to Login</Link>
          </div>
        )}

        {step !== 'done' && (
          <Link to="/login" className="mt-6 flex items-center justify-center gap-1.5 text-sm text-slate-500 hover:text-slate-700">
            <ArrowLeft className="w-4 h-4" /> Back to login
          </Link>
        )}
      </div>
    </div>
  )
}

function StepIndicator({ step }: { step: Step }) {
  const steps: Step[] = ['email', 'otp', 'password']
  const current = steps.indexOf(step)
  return (
    <div className="flex items-center gap-2 mb-6" aria-label={`Step ${current + 1} of 3`}>
      {steps.map((s, i) => (
        <div key={s} className={`h-1.5 flex-1 rounded-full ${i <= current ? 'bg-brand-600' : 'bg-slate-200'}`} />
      ))}
    </div>
  )
}

function Heading({ icon: Icon, title, children }: { icon: typeof Mail; title: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start gap-3 mb-6">
      <Icon className="w-5 h-5 text-brand-600 mt-1 flex-shrink-0" />
      <div>
        <h1 className="text-xl font-bold text-slate-900">{title}</h1>
        <p className="text-sm text-slate-500 mt-1">{children}</p>
      </div>
    </div>
  )
}

function ErrorBox({ error }: { error: string }) {
  if (!error) return null
  return (
    <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3">
      <p className="text-red-700 text-sm">{error}</p>
    </div>
  )
}
