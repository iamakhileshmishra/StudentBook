import { useState, FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { GraduationCap, Loader2, MailCheck } from 'lucide-react'
import { authApi, RegisterSchoolRequest, RegisterSchoolResponse } from '@/api/auth'

type FormState = Omit<RegisterSchoolRequest, 'feeDueDate'> & { feeDueDate: string }

const EMPTY: FormState = {
  ownerName: '',
  schoolName: '',
  shortName: '',
  email: '',
  phone: '',
  addressLine1: '',
  city: '',
  state: '',
  pincode: '',
  feeDueDate: '10',
}

interface ApiError {
  response?: { data?: { message?: string; fieldErrors?: Record<string, string> } }
}

export function RegisterPage() {
  const [form, setForm] = useState<FormState>(EMPTY)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<RegisterSchoolResponse | null>(null)

  const set = (key: keyof FormState) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((f) => ({ ...f, [key]: e.target.value }))
    setFieldErrors((fe) => ({ ...fe, [key]: '' }))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setFieldErrors({})
    setLoading(true)
    try {
      const res = await authApi.register({
        ...form,
        shortName: form.shortName?.trim() || undefined,
        feeDueDate: form.feeDueDate ? Number(form.feeDueDate) : undefined,
      })
      setResult(res)
    } catch (err: unknown) {
      const data = (err as ApiError)?.response?.data
      setFieldErrors(data?.fieldErrors ?? {})
      setError(data?.message ?? 'Could not create your account. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-lg bg-brand-600 flex items-center justify-center">
              <GraduationCap className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-lg text-slate-900">School ERP</span>
          </Link>
          <Link to="/login" className="text-sm font-medium text-brand-600 hover:text-brand-700">
            Already registered? Login
          </Link>
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 sm:px-6 py-10">
        {result ? (
          <div className="card p-8 text-center animate-fade-in">
            <div className="w-14 h-14 mx-auto rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <MailCheck className="w-7 h-7" />
            </div>
            <h1 className="mt-5 text-2xl font-bold text-slate-900">Check your email</h1>
            <p className="mt-3 text-slate-600 leading-relaxed">
              Your account for <strong>{result.schoolName}</strong> has been created. We've sent your
              User ID and a temporary password to <strong>{result.email}</strong>.
            </p>
            <p className="mt-2 text-sm text-slate-500">
              You'll be asked to set a new password the first time you sign in.
            </p>
            <Link to="/login" className="btn-primary inline-flex mt-8 px-6">Go to Login</Link>
          </div>
        ) : (
          <>
            <div className="mb-8">
              <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">Create your school account</h1>
              <p className="mt-2 text-slate-600">
                Enter your school's details. You can change them later in Settings → School Profile.
                We'll email your login details to the address you provide.
              </p>
            </div>

            <form onSubmit={handleSubmit} className="card p-6 sm:p-8 space-y-8" noValidate>
              <Fieldset title="About you">
                <Field id="ownerName" label="Your full name" required value={form.ownerName} onChange={set('ownerName')} error={fieldErrors.ownerName} autoComplete="name" />
              </Fieldset>

              <Fieldset title="School details">
                <Field id="schoolName" label="School name" required value={form.schoolName} onChange={set('schoolName')} error={fieldErrors.schoolName} className="sm:col-span-2" />
                <Field
                  id="shortName" label="Short name (receipt prefix)" value={form.shortName ?? ''} onChange={set('shortName')}
                  error={fieldErrors.shortName} maxLength={10}
                  hint="Optional — defaults to your school's initials. Also used to build your User ID."
                />
                <Field id="phone" label="Phone number" required type="tel" value={form.phone} onChange={set('phone')} error={fieldErrors.phone} autoComplete="tel" />
                <Field
                  id="email" label="Email address" required type="email" value={form.email} onChange={set('email')}
                  error={fieldErrors.email} autoComplete="email" className="sm:col-span-2"
                  hint="Your User ID and temporary password will be sent here."
                />
              </Fieldset>

              <Fieldset title="Address">
                <Field id="addressLine1" label="Address" required value={form.addressLine1} onChange={set('addressLine1')} error={fieldErrors.addressLine1} className="sm:col-span-2" autoComplete="street-address" />
                <Field id="city" label="City" required value={form.city} onChange={set('city')} error={fieldErrors.city} autoComplete="address-level2" />
                <Field id="state" label="State" required value={form.state} onChange={set('state')} error={fieldErrors.state} autoComplete="address-level1" />
                <Field id="pincode" label="Pincode" required inputMode="numeric" maxLength={6} value={form.pincode} onChange={set('pincode')} error={fieldErrors.pincode} autoComplete="postal-code" />
              </Fieldset>

              <Fieldset title="Fee settings">
                <Field
                  id="feeDueDate" label="Monthly fee due date" type="number" min={1} max={28}
                  value={form.feeDueDate} onChange={set('feeDueDate')} error={fieldErrors.feeDueDate}
                  hint="Day of the month (1–28). Late fee rules can be added later in Settings."
                />
              </Fieldset>

              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3">
                  <p className="text-red-700 text-sm">{error}</p>
                </div>
              )}

              <div className="flex flex-col-reverse sm:flex-row sm:items-center sm:justify-between gap-4 pt-2">
                <p className="text-sm text-slate-500">
                  Already have an account?{' '}
                  <Link to="/login" className="text-brand-600 hover:text-brand-700 font-medium">Login</Link>
                </p>
                <button type="submit" disabled={loading} className="btn-primary justify-center px-6 py-2.5">
                  {loading ? <><Loader2 className="w-4 h-4 animate-spin" /> Creating account...</> : 'Create Account'}
                </button>
              </div>
            </form>
          </>
        )}
      </main>
    </div>
  )
}

function Fieldset({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <fieldset>
      <legend className="text-sm font-semibold text-slate-900 uppercase tracking-wider mb-4">{title}</legend>
      <div className="grid sm:grid-cols-2 gap-5">{children}</div>
    </fieldset>
  )
}

interface FieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  id: string
  label: string
  error?: string
  hint?: string
}

function Field({ id, label, error, hint, className, required, ...input }: FieldProps) {
  return (
    <div className={className}>
      <label htmlFor={id} className="label">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      <input
        id={id}
        name={id}
        required={required}
        aria-invalid={!!error}
        className={`input-field ${error ? 'border-red-400' : ''}`}
        {...input}
      />
      {error ? (
        <p className="text-xs text-red-600 mt-1">{error}</p>
      ) : hint ? (
        <p className="text-xs text-slate-500 mt-1">{hint}</p>
      ) : null}
    </div>
  )
}
