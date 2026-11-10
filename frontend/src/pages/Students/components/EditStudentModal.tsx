import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { X, Loader2, User, Phone, IndianRupee, Trash2 } from 'lucide-react'
import { studentsApi, type StudentDetailDto } from '@/api/students'
import { cn } from '@/lib/utils'
import { RemoveStudentPrompt } from './RemoveStudentPrompt'

interface Props {
  student: StudentDetailDto
  onClose: () => void
}

const SECTIONS = [
  { id: 'personal', label: 'Personal', icon: User },
  { id: 'parent', label: 'Parent', icon: Phone },
  { id: 'fees', label: 'Fees', icon: IndianRupee },
]

export function EditStudentModal({ student, onClose }: Props) {
  const [section, setSection] = useState('personal')
  const [form, setForm] = useState({
    firstName: student.firstName,
    lastName: student.lastName,
    gender: student.gender || '',
    dateOfBirth: student.dateOfBirth || '',
    fatherName: student.fatherName || '',
    motherName: student.motherName || '',
    guardianName: student.guardianName || '',
    primaryContactPhone: student.primaryContactPhone,
    alternatePhone: student.alternatePhone || '',
    email: student.email || '',
    address: student.address || '',
    staffChild: student.staffChild,
    concessionAmount: student.concessionAmount,
    concessionNote: student.concessionNote || '',
  })
  
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [showRemovePrompt, setShowRemovePrompt] = useState(false)
  const queryClient = useQueryClient()

  const { mutate: updateStudent, isPending } = useMutation({
    mutationFn: (data: any) => studentsApi.updateStudent(student.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student', student.id] })
      onClose()
    },
    onError: (err: any) => {
      const data = err?.response?.data
      const msg = (data?.fieldErrors && Object.values(data.fieldErrors)[0]) || data?.message || 'Failed to update student'
      setErrors({ submit: msg })
    },
  })

  const set = (field: string, value: any) => {
    setForm((f) => ({ ...f, [field]: value }))
    setErrors((e) => ({ ...e, [field]: '' }))
  }

  const validate = (): boolean => {
    const errs: Record<string, string> = {}
    const today = new Date().toISOString().split('T')[0]
    
    if (!form.firstName.trim()) errs.firstName = 'Required'
    if (!form.lastName.trim()) errs.lastName = 'Required'
    
    if (!form.dateOfBirth) errs.dateOfBirth = 'Required'
    else if (form.dateOfBirth > today) errs.dateOfBirth = 'Cannot be in future'
    
    if (!form.fatherName?.trim()) errs.fatherName = 'Required'
    if (!form.motherName?.trim()) errs.motherName = 'Required'
    
    if (!form.primaryContactPhone.trim()) {
      errs.primaryContactPhone = 'Required'
    } else if (!/^\d{10}$/.test(form.primaryContactPhone)) {
      errs.primaryContactPhone = 'Must be 10 digits'
    }
    
    if (form.alternatePhone && !/^\d{10}$/.test(form.alternatePhone)) {
      errs.alternatePhone = 'Must be 10 digits'
    }

    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      errs.email = 'Enter a valid email address'
    }

    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) {
      if (errors.firstName || errors.lastName || errors.dateOfBirth) setSection('personal')
      else if (errors.primaryContactPhone || errors.alternatePhone || errors.email || errors.fatherName || errors.motherName) setSection('parent')
      return
    }

    const payload = {
      ...form,
      dateOfBirth: form.dateOfBirth || undefined,
      gender: form.gender || undefined,
      concessionAmount: Number(form.concessionAmount) || 0,
    }
    updateStudent(payload)
  }

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  if (showRemovePrompt) {
    return <RemoveStudentPrompt studentId={student.id} onClose={() => setShowRemovePrompt(false)} />
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 max-h-[90vh] flex flex-col animate-fade-in">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="text-lg font-bold text-slate-900">Edit Student Details</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Section tabs */}
        <div className="flex border-b border-slate-100 px-6 pt-2">
          {SECTIONS.map((s) => {
            const Icon = s.icon
            const hasError = (
              (s.id === 'personal' && (errors.firstName || errors.lastName || errors.dateOfBirth)) ||
              (s.id === 'parent' && (errors.primaryContactPhone || errors.alternatePhone || errors.email || errors.fatherName || errors.motherName))
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
          {section === 'personal' && (
            <div className="space-y-4 animate-fade-in">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">First Name <span className="text-red-500">*</span></label>
                  <input className={cn('input-field', errors.firstName && 'border-red-400')}
                    value={form.firstName} onChange={(e) => set('firstName', e.target.value)} />
                  {errors.firstName && <p className="text-red-500 text-xs mt-1">{errors.firstName}</p>}
                </div>
                <div>
                  <label className="label">Last Name <span className="text-red-500">*</span></label>
                  <input className={cn('input-field', errors.lastName && 'border-red-400')}
                    value={form.lastName} onChange={(e) => set('lastName', e.target.value)} />
                  {errors.lastName && <p className="text-red-500 text-xs mt-1">{errors.lastName}</p>}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Date of Birth <span className="text-red-500">*</span></label>
                  <input type="date" className={cn('input-field', errors.dateOfBirth && 'border-red-400')} value={form.dateOfBirth}
                    onChange={(e) => set('dateOfBirth', e.target.value)} />
                  {errors.dateOfBirth && <p className="text-red-500 text-xs mt-1">{errors.dateOfBirth}</p>}
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
            </div>
          )}

          {section === 'parent' && (
            <div className="space-y-4 animate-fade-in">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Father's Name <span className="text-red-500">*</span></label>
                  <input className={cn('input-field', errors.fatherName && 'border-red-400')} value={form.fatherName}
                    onChange={(e) => set('fatherName', e.target.value)} />
                  {errors.fatherName && <p className="text-red-500 text-xs mt-1">{errors.fatherName}</p>}
                </div>
                <div>
                  <label className="label">Mother's Name <span className="text-red-500">*</span></label>
                  <input className={cn('input-field', errors.motherName && 'border-red-400')} value={form.motherName}
                    onChange={(e) => set('motherName', e.target.value)} />
                  {errors.motherName && <p className="text-red-500 text-xs mt-1">{errors.motherName}</p>}
                </div>
              </div>
              <div>
                <label className="label">Primary Phone <span className="text-red-500">*</span></label>
                <input type="tel" className={cn('input-field', errors.primaryContactPhone && 'border-red-400')}
                  value={form.primaryContactPhone} onChange={(e) => {
                    const val = e.target.value.replace(/\D/g, '')
                    if (val.length <= 10) set('primaryContactPhone', val)
                  }} />
                {errors.primaryContactPhone && <p className="text-red-500 text-xs mt-1">{errors.primaryContactPhone}</p>}
              </div>
              <div>
                <label className="label">Alternate Phone</label>
                <input type="tel" className={cn('input-field', errors.alternatePhone && 'border-red-400')} value={form.alternatePhone}
                  onChange={(e) => {
                    const val = e.target.value.replace(/\D/g, '')
                    if (val.length <= 10) set('alternatePhone', val)
                  }} />
                {errors.alternatePhone && <p className="text-red-500 text-xs mt-1">{errors.alternatePhone}</p>}
              </div>
              <div>
                <label className="label">Parent Email</label>
                <input type="email" className={cn('input-field', errors.email && 'border-red-400')} value={form.email}
                  onChange={(e) => set('email', e.target.value.trim())} placeholder="Optional" />
                {errors.email
                  ? <p className="text-red-500 text-xs mt-1">{errors.email}</p>
                  : <p className="text-slate-400 text-xs mt-1">Fee receipts are emailed here after each payment</p>}
              </div>
              <div>
                <label className="label">Address</label>
                <textarea rows={3} className="input-field resize-none" value={form.address}
                  onChange={(e) => set('address', e.target.value)} />
              </div>
            </div>
          )}

          {section === 'fees' && (
            <div className="space-y-4 animate-fade-in">
              <div className="p-4 bg-purple-50 rounded-xl border border-purple-100">
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.staffChild}
                    onChange={(e) => set('staffChild', e.target.checked)}
                    className="w-4 h-4 accent-brand-600"
                  />
                  <div>
                    <p className="font-medium text-slate-800">Staff Child</p>
                    <p className="text-xs text-slate-500">Child of school staff member</p>
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
                />
              </div>

              <div>
                <label className="label">Concession Note</label>
                <textarea rows={2} className="input-field resize-none"
                  value={form.concessionNote}
                  onChange={(e) => set('concessionNote', e.target.value)} />
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between gap-3">
          <button 
            onClick={() => setShowRemovePrompt(true)}
            className="flex items-center gap-2 px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg text-sm font-semibold transition-colors"
          >
            <Trash2 className="w-4 h-4" /> Remove Student
          </button>
          
          <div className="flex items-center gap-3">
            {errors.submit && <p className="text-red-600 text-sm mr-2">{errors.submit}</p>}
            
            <button onClick={onClose} className="px-4 py-2 text-slate-600 hover:bg-slate-50 rounded-lg text-sm font-semibold transition-colors">
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={isPending}
              className="btn-primary"
            >
              {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Save Changes'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
