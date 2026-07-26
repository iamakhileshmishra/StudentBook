import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Phone, GraduationCap, IndianRupee, Calendar, Loader2, Edit2 } from 'lucide-react'
import { studentsApi } from '@/api/students'
import { formatCurrency, formatDate } from '@/lib/utils'
import { EditStudentModal } from './components/EditStudentModal'

export function StudentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [showEditModal, setShowEditModal] = useState(false)

  const { data: student, isLoading } = useQuery({
    queryKey: ['student', id],
    queryFn: () => studentsApi.getById(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
      </div>
    )
  }

  if (!student) {
    return (
      <div className="p-8 text-center">
        <p className="text-slate-500">Student not found.</p>
        <Link to="/students" className="btn-primary mt-4 inline-flex">← Back to Students</Link>
      </div>
    )
  }

  return (
    <div className="p-8 animate-fade-in relative">
      {/* Breadcrumb and Edit Action */}
      <div className="flex justify-between items-center mb-6">
        <Link to="/students" className="flex items-center gap-2 text-sm text-slate-500 hover:text-slate-700 w-fit">
          <ArrowLeft className="w-4 h-4" /> Back to Students
        </Link>
        <button 
          onClick={() => setShowEditModal(true)}
          className="btn-secondary flex items-center gap-2"
        >
          <Edit2 className="w-4 h-4" />
          Edit Student
        </button>
      </div>

      {/* Profile header */}
      <div className="card p-6 mb-6">
        <div className="flex items-start gap-5">
          <div className="w-16 h-16 rounded-2xl bg-brand-100 flex items-center justify-center flex-shrink-0">
            <span className="text-brand-700 font-bold text-2xl">
              {student.firstName.charAt(0).toUpperCase()}
            </span>
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-2xl font-bold text-slate-900">{student.fullName}</h1>
              <span className={
                student.status === 'ACTIVE' ? 'badge-green' : 
                student.status === 'LEFT' ? 'badge-red' : 'badge-gray'
              }>
                {student.status}
              </span>
              {student.staffChild && <span className="badge-gray">Staff Child</span>}
            </div>
            <div className="flex items-center gap-4 mt-2 text-sm text-slate-500 flex-wrap">
              <span className="font-mono font-semibold text-brand-600">#{student.admissionNumber}</span>
              {student.className && (
                <span className="flex items-center gap-1">
                  <GraduationCap className="w-3.5 h-3.5" />
                  {student.className} — {student.sectionName} ({student.academicYearName})
                </span>
              )}
              {student.dateOfBirth && (
                <span className="flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5" />
                  DOB: {formatDate(student.dateOfBirth)}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Parent / Contact info */}
        <div className="card">
          <div className="card-header flex items-center gap-2">
            <Phone className="w-4 h-4 text-slate-500" />
            <h2 className="font-semibold text-slate-800">Parent & Contact</h2>
          </div>
          <div className="card-body space-y-3">
            <InfoRow label="Father's Name" value={student.fatherName} />
            <InfoRow label="Mother's Name" value={student.motherName} />
            {student.guardianName && <InfoRow label="Guardian" value={student.guardianName} />}
            <InfoRow label="Primary Phone" value={student.primaryContactPhone} mono />
            {student.alternatePhone && <InfoRow label="Alternate Phone" value={student.alternatePhone} mono />}
            {student.email && <InfoRow label="Email" value={student.email} />}
            {student.address && <InfoRow label="Address" value={student.address} />}
          </div>
        </div>

        {/* Fee info */}
        <div className="card">
          <div className="card-header flex items-center gap-2">
            <IndianRupee className="w-4 h-4 text-slate-500" />
            <h2 className="font-semibold text-slate-800">Fee Information</h2>
          </div>
          <div className="card-body space-y-3">
            <InfoRow label="Monthly Concession"
              value={student.concessionAmount > 0 ? formatCurrency(student.concessionAmount) : 'None'} />
            {student.concessionNote && <InfoRow label="Concession Note" value={student.concessionNote} />}
            <InfoRow label="Staff Child" value={student.staffChild ? 'Yes' : 'No'} />
            {student.enrollmentDate && (
              <InfoRow label="Enrollment Date" value={formatDate(student.enrollmentDate)} />
            )}
            <InfoRow label="Member Since" value={formatDate(student.createdAt)} />
          </div>
        </div>
      </div>

      {showEditModal && (
        <EditStudentModal 
          student={student} 
          onClose={() => setShowEditModal(false)} 
        />
      )}
    </div>
  )
}

function InfoRow({ label, value, mono }: { label: string; value?: string | null; mono?: boolean }) {
  if (!value) return null
  return (
    <div className="flex justify-between items-start gap-4">
      <span className="text-sm text-slate-500 flex-shrink-0">{label}</span>
      <span className={`text-sm font-medium text-slate-800 text-right ${mono ? 'font-mono' : ''}`}>{value}</span>
    </div>
  )
}
