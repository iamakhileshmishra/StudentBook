import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Search, UserPlus, ChevronLeft, ChevronRight, Users, GraduationCap, Loader2 } from 'lucide-react'
import { studentsApi } from '@/api/students'
import { academicApi } from '@/api/academic'
import { formatCurrency } from '@/lib/utils'
import { AddStudentModal } from './AddStudentModal'

export function StudentsPage() {
  const [search, setSearch] = useState('')
  const [selectedClass, setSelectedClass] = useState('')
  const [page, setPage] = useState(0)
  const [showAddModal, setShowAddModal] = useState(false)

  const { data: classes } = useQuery({
    queryKey: ['classes'],
    queryFn: academicApi.getClasses,
    staleTime: Infinity,
  })

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['students', search, selectedClass, page],
    queryFn: () =>
      studentsApi.search({
        q: search || undefined,
        classId: selectedClass || undefined,
        page,
        size: 20,
      }),
    placeholderData: (prev) => prev,
  })

  const handleSearch = (value: string) => {
    setSearch(value)
    setPage(0)
  }

  const handleClassFilter = (classId: string) => {
    setSelectedClass(classId)
    setPage(0)
  }

  return (
    <div className="p-8 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Students</h1>
          <p className="text-slate-500 text-sm mt-1">
            {data ? `${data.activeCount} students enrolled` : 'Loading...'}
          </p>
        </div>
        <button
          id="add-student-btn"
          onClick={() => setShowAddModal(true)}
          className="btn-primary"
        >
          <UserPlus className="w-4 h-4" />
          Add Student
        </button>
      </div>

      {/* Filters */}
      <div className="card mb-6">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4">
            {/* Search */}
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                id="student-search"
                type="text"
                placeholder="Search by name or admission number..."
                value={search}
                onChange={(e) => handleSearch(e.target.value)}
                className="input-field pl-10"
              />
            </div>

            {/* Class filter */}
            <div className="sm:w-56">
              <select
                id="class-filter"
                value={selectedClass}
                onChange={(e) => handleClassFilter(e.target.value)}
                className="input-field"
              >
                <option value="">All Classes</option>
                {classes?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="card">
        {isLoading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
          </div>
        ) : !data?.content.length ? (
          <div className="text-center py-20">
            <Users className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-500 font-medium">No students found</p>
            <p className="text-slate-400 text-sm mt-1">
              {search || selectedClass ? 'Try adjusting your filters' : 'Add your first student to get started'}
            </p>
            {!search && !selectedClass && (
              <button onClick={() => setShowAddModal(true)} className="btn-primary mt-4 mx-auto">
                <UserPlus className="w-4 h-4" /> Add First Student
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Adm. No.</th>
                    <th>Student Name</th>
                    <th>Class</th>
                    <th>Father's Name</th>
                    <th>Phone</th>
                    <th>Concession</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody className={isFetching ? 'opacity-60' : ''}>
                  {data.content.map((student) => (
                    <tr key={student.id}>
                      <td>
                        <span className="font-mono text-sm font-semibold text-brand-600">
                          #{student.admissionNumber}
                        </span>
                      </td>
                      <td>
                        <div>
                          <p className="font-semibold text-slate-800">{student.fullName}</p>
                          {student.staffChild && (
                            <span className="text-xs text-purple-600 font-medium">Staff Child</span>
                          )}
                        </div>
                      </td>
                      <td>
                        {student.className ? (
                          <span className="badge-blue">
                            <GraduationCap className="w-3 h-3" />
                            {student.className} {student.sectionName ?? ''}
                          </span>
                        ) : (
                          <span className="text-slate-400 text-sm">—</span>
                        )}
                      </td>
                      <td className="text-slate-600">{student.fatherName || '—'}</td>
                      <td className="text-slate-600 font-mono text-sm">{student.primaryContactPhone}</td>
                      <td>
                        {student.concessionAmount > 0 ? (
                          <span className="badge-yellow">{formatCurrency(student.concessionAmount)}/mo</span>
                        ) : (
                          <span className="text-slate-400 text-sm">None</span>
                        )}
                      </td>
                      <td>
                        <span className={student.status === 'ACTIVE' ? 'badge-green' : 'badge-red'}>
                          {student.status}
                        </span>
                      </td>
                      <td>
                        <Link
                          to={`/students/${student.id}`}
                          className="text-sm text-brand-600 hover:text-brand-700 font-medium"
                        >
                          View →
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {data.totalPages > 1 && (
              <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between">
                <p className="text-sm text-slate-500">
                  Showing {page * 20 + 1}–{Math.min((page + 1) * 20, data.totalElements)} of{' '}
                  {data.totalElements} students
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage((p) => p - 1)}
                    disabled={page === 0}
                    className="btn-secondary px-3 py-1.5 text-sm disabled:opacity-40"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <span className="flex items-center px-3 text-sm text-slate-600">
                    Page {page + 1} of {data.totalPages}
                  </span>
                  <button
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= data.totalPages - 1}
                    className="btn-secondary px-3 py-1.5 text-sm disabled:opacity-40"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {showAddModal && (
        <AddStudentModal onClose={() => setShowAddModal(false)} />
      )}
    </div>
  )
}
