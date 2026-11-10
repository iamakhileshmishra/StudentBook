import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, Loader2 } from 'lucide-react'
import { studentsApi } from '@/api/students'

interface Props {
  studentId: string
  onClose: () => void
}

export function RemoveStudentPrompt({ studentId, onClose }: Props) {
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { mutate: removeStudent, isPending } = useMutation({
    mutationFn: () => studentsApi.removeStudent(studentId, password),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] })
      navigate('/students')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message ?? 'Failed to remove student'
      setError(msg)
    }
  })

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 animate-fade-in flex flex-col overflow-hidden">
        
        {/* Header */}
        <div className="bg-red-50 px-6 py-4 border-b border-red-100 flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center text-red-600">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-red-700">Remove Student</h2>
            <p className="text-xs text-red-600/80">This action requires admin approval</p>
          </div>
        </div>

        {/* Body */}
        <div className="p-6">
          <p className="text-sm text-slate-600 mb-6">
            Are you sure you want to remove this student? They will be marked as <strong>LEFT</strong> and removed from active rosters, but their fee history will be preserved.
          </p>

          <label className="block text-sm font-semibold text-slate-800 mb-2">Admin Password</label>
          <input 
            type="password"
            autoFocus
            className="w-full px-4 py-2 border-2 border-slate-200 rounded-lg focus:outline-none focus:border-red-400"
            placeholder="Enter your login password to confirm"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              setError('')
            }}
          />
          {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-slate-50 flex items-center justify-end gap-3">
          <button 
            onClick={onClose}
            className="px-4 py-2 text-slate-600 hover:bg-slate-200 rounded-lg text-sm font-semibold transition-colors"
          >
            Cancel
          </button>
          <button 
            onClick={() => {
              if (!password) setError('Password is required')
              else removeStudent()
            }}
            disabled={isPending || !password}
            className="px-6 py-2 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-lg text-sm font-bold shadow-sm transition-colors flex items-center gap-2"
          >
            {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Confirm Removal'}
          </button>
        </div>
      </div>
    </div>
  )
}
