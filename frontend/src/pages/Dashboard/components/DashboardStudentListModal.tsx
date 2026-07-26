import React from 'react'
import { useQuery } from '@tanstack/react-query'
import { X } from 'lucide-react'
import api from '@/api/axios'
import { formatCurrency } from '@/lib/utils'

interface DashboardStudentDto {
  studentId: string
  name: string
  admissionNumber: string
  paymentStatus: 'PAID' | 'PARTIAL' | 'UNPAID'
  amountPaid: number
}

interface Props {
  isOpen: boolean
  onClose: () => void
  classId: string
  className: string
  month: number
  year: number
  monthName: string
}

export function DashboardStudentListModal({ isOpen, onClose, classId, className, month, year, monthName }: Props) {
  const { data: students, isLoading } = useQuery({
    queryKey: ['dashboard-class-students', classId, month, year],
    queryFn: () => 
      api.get<DashboardStudentDto[]>(`/dashboard/class-students?classId=${classId}&month=${month}&year=${year}`)
         .then(r => r.data),
    enabled: isOpen && !!classId
  })

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[85vh] overflow-hidden flex flex-col">
        <div className="flex items-center justify-between p-5 border-b border-slate-100">
          <div>
            <h2 className="text-xl font-bold text-slate-800">{className} Students</h2>
            <p className="text-sm text-slate-500">Fee Status for {monthName} {year}</p>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-50 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-auto flex-1 p-5">
          {isLoading ? (
            <div className="py-10 text-center text-slate-400">Loading students...</div>
          ) : (
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Student Name</th>
                  <th>Adm. No</th>
                  <th>Status</th>
                  <th className="text-right">Amount Paid</th>
                </tr>
              </thead>
              <tbody>
                {students?.map(s => (
                  <tr key={s.studentId}>
                    <td className="font-medium text-slate-800">{s.name}</td>
                    <td className="text-slate-500">{s.admissionNumber}</td>
                    <td>
                      {s.paymentStatus === 'PAID' && <span className="badge-green">Paid</span>}
                      {s.paymentStatus === 'PARTIAL' && <span className="badge-amber">Partial</span>}
                      {s.paymentStatus === 'UNPAID' && <span className="badge-red">Unpaid</span>}
                    </td>
                    <td className="text-right font-medium text-slate-700">
                      {s.amountPaid > 0 ? formatCurrency(s.amountPaid) : '—'}
                    </td>
                  </tr>
                ))}
                {students?.length === 0 && (
                  <tr>
                    <td colSpan={4} className="py-8 text-center text-slate-400">No students enrolled in this class.</td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
