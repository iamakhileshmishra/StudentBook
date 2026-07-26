import { useState, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { X, Loader2, User, Phone, GraduationCap, IndianRupee, CheckCircle2 } from 'lucide-react'
import { studentsApi, type StudentCreateRequest } from '@/api/students'
import { academicApi } from '@/api/academic'
import { cn } from '@/lib/utils'

interface Props {
  onClose: () => void
}

const SECTIONS = [
  { id: 'personal', label: 'Personal', icon: User },
  { id: 'parent', label: 'Parent', icon: Phone },
  { id: 'class', label: 'Class', icon: GraduationCap },
  { id: 'fees', label: 'Fees', icon: IndianRupee },
]

const DEFAULT_FORM: StudentCreateRequest = {
  firstName: '', lastName: '', gender: '', dateOfBirth: '',
  fatherName: '', motherName: '', guardianName: '',
  primaryContactPhone: '', alternatePhone: '', email: '', address: '',
  staffChild: false, concessionAmount: 0, concessionNote: '',
  classId: '', sectionId: '', enrollmentDate: '', admissionNumber: '',
}

export function AddStudentModal({ onClose }: Props) {
  const [section, setSection] = useState('personal')
  const [form, setForm] = useState<StudentCreateRequest>(DEFAULT_FORM)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [success, setSuccess] = useState(false)
  const queryClient = useQueryClient()

  const { data: classes, isLoading: isClassesLoading, isError: isClassesError, refetch: refetchClasses } = useQuery({
    queryKey: ['classes'],
    queryFn: academicApi.getClasses,
    staleTime: Infinity,
  })

  const selectedClass = classes?.find((c) => c.id === form.classId)

  const { mutate: createStudent, isPending } = useMutation({
    mutationFn: studentsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] })
      setSuccess(true)
      setTimeout(onClose, 1500)
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message ?? 'Failed to add student'
      setErrors({ submit: msg })
    },
  })

  const set = (field: keyof StudentCreateRequest, value: unknown) => {
    setForm((f) => ({ ...f, [field]: value }))
    setErrors((e) => ({ ...e, [field]: '' }))
    // Reset section when class changes
    if (field === 'classId') setForm((f) => ({ ...f, classId: value as string, sectionId: '' }))
  }

  const validate = (): boolean => {
    const errs: Record<string, string> = {}
    if (!form.firstName.trim()) errs.firstName = 'Required'
    if (!form.lastName.trim()) errs.lastName = 'Required'
    if (!form.primaryContactPhone.trim()) errs.primaryContactPhone = 'Required'
    if (!form.classId) errs.classId = 'Required'
    if (!form.sectionId) errs.sectionId = 'Required'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) {
      // Switch to section with first error
      if (errors.firstName || errors.lastName) setSection('personal')
      else if (errors.primaryContactPhone) setSection('parent')
      else if (errors.classId || errors.sectionId) setSection('class')
      return
    }

    const payload: StudentCreateRequest = {
      ...form,
      dateOfBirth: form.dateOfBirth || undefined,
      gender: form.gender || undefined,
      admissionNumber: form.admissionNumber || undefined,
      enrollmentDate: form.enrollmentDate || undefined,
      concessionAmount: Number(form.concessionAmount) || 0,
    }
    createStudent(payload)
  }

  // Trap escape key
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 max-h-[90vh] flex flex-col animate-fade-in">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="text-lg font-bold text-slate-900">Add New Student</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Success state */}
        {success ? (
          <div className="flex-1 flex flex-col items-center justify-center py-16">
            <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center mb-4">
              <CheckCircle2 className="w-8 h-8 text-emerald-600" />
            </div>
            <p className="text-xl font-bold text-slate-800">Student Added!</p>
            <p className="text-slate-500 mt-1">Admission number assigned automatically</p>
          </div>
        ) : (
          <>
            {/* Section tabs */}
            <div className="flex border-b border-slate-100 px-6 pt-2">
              {SECTIONS.map((s) => {
                const Icon = s.icon
                const hasError = (
                  (s.id === 'personal' && (errors.firstName || errors.lastName)) ||
                  (s.id === 'parent' && errors.primaryContactPhone) ||
                  (s.id === 'class' && (errors.classId || errors.sectionId))
                )
                return (
                  <button
                    key={s.id}
                    onClick={() => setSection(s.id)}
                    className={cn(
                      'flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 -mb-px transition-colors',
                      section === s.id
                        ? 'border-brand-600 text-brand-600'
                        : 'border-transparent text-slate-500 hover:text-slate-700'
                    )}
                  >
                    <Icon className="w-4 h-4" />
                    {s.label}
                    {hasError && <span className="w-1.5 h-1.5 rounded-full bg-red-500" />}
                  </button>
                )
              })}
            </div>

            {/* Form body */}
            <div className="flex-1 overflow-y-auto px-6 py-5">
              {/* ── Personal ── */}
              {section === 'personal' && (
                <div className="space-y-4 animate-fade-in">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="label">First Name <span className="text-red-500">*</span></label>
                      <input id="student-first-name" className={cn('input-field', errors.firstName && 'border-red-400')}
                        value={form.firstName} onChange={(e) => set('firstName', e.target.value)} placeholder="Ram" />
                      {errors.firstName && <p className="text-red-500 text-xs mt-1">{errors.firstName}</p>}
                    </div>
                    <div>
                      <label className="label">Last Name <span className="text-red-500">*</span></label>
                      <input id="student-last-name" className={cn('input-field', errors.lastName && 'border-red-400')}
                        value={form.lastName} onChange={(e) => set('lastName', e.target.value)} placeholder="Kumar" />
                      {errors.lastName && <p className="text-red-500 text-xs mt-1">{errors.lastName}</p>}
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="label">Date of Birth</label>
                      <input type="date" className="input-field" value={form.dateOfBirth}
                        onChange={(e) => set('dateOfBirth', e.target.value)} />
                    </div>
                    <div>
                      <label className="label">Gender</label>
                      <select className="input-field" value={form.gender} onChange={(e) => set('gender', e.target.value)}>
                        <option value="">Select gender</option>
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                        <option value="OTHER">Other</option>
                      </select>
                    </div>
                  </div>
                  <div>
                    <label className="label">Admission Number <span className="text-slate-400 text-xs">(leave blank for auto-assign)</span></label>
                    <input className="input-field" placeholder="Auto" value={form.admissionNumber}
                      onChange={(e) => set('admissionNumber', e.target.value)} />
                  </div>
                </div>
              )}

              {/* ── Parent ── */}
              {section === 'parent' && (
                <div className="space-y-4 animate-fade-in">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="label">Father's Name</label>
                      <input className="input-field" value={form.fatherName ?? ''}
                        onChange={(e) => set('fatherName', e.target.value)} placeholder="Suresh Kumar" />
                    </div>
                    <div>
                      <label className="label">Mother's Name</label>
                      <input className="input-field" value={form.motherName ?? ''}
                        onChange={(e) => set('motherName', e.target.value)} placeholder="Priya Kumar" />
                    </div>
                  </div>
                  <div>
                    <label className="label">Primary Phone <span className="text-red-500">*</span></label>
                    <input id="student-phone" type="tel" className={cn('input-field', errors.primaryContactPhone && 'border-red-400')}
                      value={form.primaryContactPhone} onChange={(e) => set('primaryContactPhone', e.target.value)}
                      placeholder="9876543210" />
                    {errors.primaryContactPhone && <p className="text-red-500 text-xs mt-1">{errors.primaryContactPhone}</p>}
                  </div>
                  <div>
                    <label className="label">Alternate Phone</label>
                    <input type="tel" className="input-field" value={form.alternatePhone ?? ''}
                      onChange={(e) => set('alternatePhone', e.target.value)} placeholder="Optional" />
                  </div>
                  <div>
                    <label className="label">Address</label>
                    <textarea rows={3} className="input-field resize-none" value={form.address ?? ''}
                      onChange={(e) => set('address', e.target.value)}
                      placeholder="House No., Street, Village/City..." />
                  </div>
                </div>
              )}

              {/* ── Class ── */}
              {section === 'class' && (
                <div className="space-y-4 animate-fade-in">
                  <div>
                    <label className="label">Class <span className="text-red-500">*</span></label>
                    {isClassesLoading ? (
                      <div className="flex items-center justify-center py-4 text-slate-400">
                        <Loader2 className="w-5 h-5 animate-spin mr-2" /> Loading classes...
                      </div>
                    ) : isClassesError ? (
                      <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm flex items-center justify-between border border-red-100">
                        <span>Failed to load classes.</span>
                        <button type="button" onClick={() => refetchClasses()} className="font-bold hover:underline">Retry</button>
                      </div>
                    ) : (
                      <div className="grid grid-cols-4 gap-2">
                        {classes?.map((c) => (
                          <button
                            key={c.id}
                            type="button"
                            onClick={() => set('classId', c.id)}
                            className={cn(
                              'py-3 px-2 rounded-lg border-2 text-sm font-medium transition-all',
                              form.classId === c.id
                                ? 'border-brand-500 bg-brand-50 text-brand-700'
                                : 'border-slate-200 text-slate-600 hover:border-slate-300'
                            )}
                          >
                            {c.name}
                          </button>
                        ))}
                      </div>
                    )}
                    {errors.classId && <p className="text-red-500 text-xs mt-1">{errors.classId}</p>}
                  </div>

                  {selectedClass && (
                    <div>
                      <label className="label">Section <span className="text-red-500">*</span></label>
                      <div className="flex gap-2 flex-wrap">
                        {selectedClass.sections.map((s) => (
                          <button
                            key={s.id}
                            type="button"
                            onClick={() => set('sectionId', s.id)}
                            className={cn(
                              'w-14 py-2 rounded-lg border-2 text-sm font-bold transition-all',
                              form.sectionId === s.id
                                ? 'border-brand-500 bg-brand-50 text-brand-700'
                                : 'border-slate-200 text-slate-600 hover:border-slate-300'
                            )}
                          >
                            {s.name}
                          </button>
                        ))}
                      </div>
                      {errors.sectionId && <p className="text-red-500 text-xs mt-1">{errors.sectionId}</p>}
                    </div>
                  )}

                  <div>
                    <label className="label">Enrollment Date</label>
                    <input type="date" className="input-field" value={form.enrollmentDate ?? ''}
                      onChange={(e) => set('enrollmentDate', e.target.value)} />
                  </div>
                </div>
              )}

              {/* ── Fees ── */}
              {section === 'fees' && (
                <div className="space-y-4 animate-fade-in">
                  <div className="p-4 bg-purple-50 rounded-xl border border-purple-100">
                    <label className="flex items-center gap-3 cursor-pointer">
                      <input
                        type="checkbox"
                        id="staff-child-checkbox"
                        checked={form.staffChild}
                        onChange={(e) => set('staffChild', e.target.checked)}
                        className="w-4 h-4 accent-brand-600"
                      />
                      <div>
                        <p className="font-medium text-slate-800">Staff Child</p>
                        <p className="text-xs text-slate-500">Child of school staff member — may be eligible for concession</p>
                      </div>
                    </label>
                  </div>

                  <div>
                    <label className="label">Monthly Concession Amount (₹)</label>
                    <input
                      type="number"
                      min="0"
                      step="50"
                      className="input-field"
                      value={form.concessionAmount}
                      onChange={(e) => set('concessionAmount', parseFloat(e.target.value) || 0)}
                      placeholder="0"
                    />
                    <p className="text-xs text-slate-400 mt-1">This amount will be deducted from monthly fee total</p>
                  </div>

                  <div>
                    <label className="label">Concession Note</label>
                    <textarea rows={2} className="input-field resize-none"
                      value={form.concessionNote ?? ''}
                      onChange={(e) => set('concessionNote', e.target.value)}
                      placeholder="e.g. Staff child concession, Scholarship, etc." />
                  </div>
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between gap-3">
              {errors.submit && (
                <p className="text-red-600 text-sm">{errors.submit}</p>
              )}
              <div className="flex gap-3 ml-auto">
                <div className="flex gap-1">
                  {SECTIONS.map((s) => (
                    <button key={s.id} onClick={() => setSection(s.id)}
                      className={cn('w-2 h-2 rounded-full transition-colors',
                        section === s.id ? 'bg-brand-600' : 'bg-slate-200')}
                    />
                  ))}
                </div>
                {section !== 'fees' ? (
                  <button
                    onClick={() => {
                      const idx = SECTIONS.findIndex((s) => s.id === section)
                      setSection(SECTIONS[idx + 1].id)
                    }}
                    className="btn-primary"
                  >
                    Next →
                  </button>
                ) : (
                  <button
                    id="submit-student-btn"
                    onClick={handleSubmit}
                    disabled={isPending}
                    className="btn-primary"
                  >
                    {isPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Saving...</> : 'Add Student'}
                  </button>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
