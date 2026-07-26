import { useQuery } from '@tanstack/react-query'
import {
  IndianRupee, TrendingUp, AlertCircle, CheckCircle2
} from 'lucide-react'
import { useState } from 'react'
import api from '@/api/axios'
import { paymentsApi, FullReceiptDto } from '@/api/payments'
import { formatCurrency, getMonthName } from '@/lib/utils'
import { useAuthStore } from '@/store/authStore'
import { DashboardStudentListModal } from './components/DashboardStudentListModal'
import { ReceiptModal } from '../FeeCollection/components/ReceiptModal'

interface DashboardSummary {
  todayCollection: number
  thisMonthCollection: number
  totalStudents: number
  studentsPaid: number
  studentsPartiallyPaid: number
  studentsNotPaid: number
}

interface ClassWiseStat {
  classId: string
  className: string
  totalStudents: number
  collected: number
  pending: number
}

interface RecentTransaction {
  id: string
  receiptNumber: string
  studentName: string
  admissionNumber: string
  className: string
  amountPaid: number
  paymentMethod: string
  confirmedAt: string
}

const now = new Date()
const currentMonth = now.getMonth() + 1
const currentYear = now.getFullYear()

export function DashboardPage() {
  const user = useAuthStore((s) => s.user)

  // Modals state
  const [selectedClass, setSelectedClass] = useState<{ id: string, name: string } | null>(null)
  const [selectedReceipt, setSelectedReceipt] = useState<FullReceiptDto | null>(null)
  const [isReceiptLoading, setIsReceiptLoading] = useState(false)

  const handleReceiptClick = async (receiptNumber: string) => {
    try {
      setIsReceiptLoading(true)
      const data = await paymentsApi.getReceipt(receiptNumber)
      setSelectedReceipt(data)
    } catch (e) {
      console.error(e)
    } finally {
      setIsReceiptLoading(false)
    }
  }

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['dashboard-summary', currentMonth, currentYear],
    queryFn: () =>
      api.get<DashboardSummary>(`/dashboard/summary?month=${currentMonth}&year=${currentYear}`)
         .then((r) => r.data),
  })

  const { data: classWise, isLoading: classLoading } = useQuery({
    queryKey: ['dashboard-class-wise', currentMonth, currentYear],
    queryFn: () =>
      api.get<ClassWiseStat[]>(`/dashboard/class-wise?month=${currentMonth}&year=${currentYear}`)
         .then((r) => r.data),
  })

  const { data: recent } = useQuery({
    queryKey: ['dashboard-recent'],
    queryFn: () =>
      api.get<RecentTransaction[]>('/dashboard/recent?limit=8').then((r) => r.data),
    refetchInterval: 30000, // refresh every 30s
  })

  const paidPercent = summary
    ? Math.min(100, Math.round((summary.studentsPaid / Math.max(summary.totalStudents, 1)) * 100))
    : 0

  return (
    <div className="p-8 animate-fade-in">
      {/* Page header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900">
          Good {getGreeting()}, {user?.fullName?.split(' ')[0]} 👋
        </h1>
        <p className="text-slate-500 mt-1">
          {getMonthName(currentMonth)} {currentYear} — Fee Collection Overview
        </p>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-8">
        <StatCard
          label="Today's Collection"
          value={summaryLoading ? '—' : formatCurrency(summary?.todayCollection ?? 0)}
          icon={<IndianRupee className="w-5 h-5" />}
          color="blue"
          loading={summaryLoading}
        />
        <StatCard
          label={`${getMonthName(currentMonth)} Collection`}
          value={summaryLoading ? '—' : formatCurrency(summary?.thisMonthCollection ?? 0)}
          icon={<TrendingUp className="w-5 h-5" />}
          color="green"
          loading={summaryLoading}
        />
        <StatCard
          label="Students Paid"
          value={summaryLoading ? '—' : `${summary?.studentsPaid ?? 0} / ${summary?.totalStudents ?? 0}`}
          sub={`${paidPercent}% paid this month`}
          icon={<CheckCircle2 className="w-5 h-5" />}
          color="emerald"
          loading={summaryLoading}
        />
        <StatCard
          label="Yet to Pay"
          value={summaryLoading ? '—' : `${summary?.studentsNotPaid ?? 0}`}
          sub={summary ? `${summary.studentsPartiallyPaid} partial payments` : ''}
          icon={<AlertCircle className="w-5 h-5" />}
          color="amber"
          loading={summaryLoading}
        />
      </div>

      {/* Progress bar */}
      {summary && (
        <div className="card p-5 mb-8">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-semibold text-slate-700">
              Monthly Collection Progress — {getMonthName(currentMonth)} {currentYear}
            </p>
            <span className="text-sm font-bold text-brand-600">{paidPercent}%</span>
          </div>
          <div className="w-full bg-slate-100 rounded-full h-3">
            <div
              className="bg-gradient-to-r from-brand-500 to-brand-600 h-3 rounded-full transition-all duration-700"
              style={{ width: `${paidPercent}%` }}
            />
          </div>
          <div className="flex justify-between text-xs text-slate-500 mt-2">
            <span>{summary.studentsPaid} paid</span>
            <span>{summary.studentsPartiallyPaid} partial</span>
            <span>{summary.studentsNotPaid} not paid</span>
          </div>
        </div>
      )}

      {/* Class-wise + Recent transactions */}
      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        {/* Class-wise table */}
        <div className="xl:col-span-3 card">
          <div className="card-header flex items-center justify-between">
            <h2 className="font-semibold text-slate-800">Class-Wise Collection</h2>
            <span className="badge-blue">{getMonthName(currentMonth)}</span>
          </div>
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Class</th>
                  <th>Students</th>
                  <th>Collected</th>
                  <th>Pending</th>
                  <th>Progress</th>
                </tr>
              </thead>
              <tbody>
                {classLoading && (
                  <tr><td colSpan={5} className="text-center py-8 text-slate-400">Loading...</td></tr>
                )}
                {classWise?.map((row) => {
                  const total = row.collected + row.pending
                  const pct = total > 0 ? Math.round((row.collected / total) * 100) : 0
                  return (
                    <tr key={row.classId}>
                      <td>
                        <button 
                          onClick={() => setSelectedClass({ id: row.classId, name: row.className })}
                          className="font-medium text-brand-600 hover:text-brand-700 hover:underline text-left"
                        >
                          {row.className}
                        </button>
                      </td>
                      <td>
                        <button 
                          onClick={() => setSelectedClass({ id: row.classId, name: row.className })}
                          className="text-slate-600 hover:text-brand-600 hover:underline"
                        >
                          {row.totalStudents}
                        </button>
                      </td>
                      <td className="text-emerald-600 font-medium">{formatCurrency(row.collected)}</td>
                      <td className={row.pending > 0 ? 'text-amber-600 font-medium' : 'text-slate-400'}>
                        {formatCurrency(row.pending)}
                      </td>
                      <td>
                        <div className="flex items-center gap-2">
                          <div className="flex-1 bg-slate-100 rounded-full h-1.5">
                            <div
                              className="bg-brand-500 h-1.5 rounded-full"
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                          <span className="text-xs text-slate-500 w-7 text-right">{pct}%</span>
                        </div>
                      </td>
                    </tr>
                  )
                })}
                {!classLoading && (!classWise || classWise.length === 0) && (
                  <tr>
                    <td colSpan={5} className="text-center py-8 text-slate-400">
                      No data for this month
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recent transactions */}
        <div className="xl:col-span-2 card">
          <div className="card-header">
            <h2 className="font-semibold text-slate-800">Recent Payments</h2>
          </div>
          <div className="divide-y divide-slate-100">
            {recent?.slice(0, 8).map((txn) => (
              <div key={txn.id} className="px-5 py-3 flex items-center gap-3 hover:bg-slate-50">
                <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center flex-shrink-0">
                  <IndianRupee className="w-4 h-4 text-emerald-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-800 truncate">{txn.studentName}</p>
                  <p className="text-xs text-slate-500">
                    {txn.className} · 
                    <button 
                      onClick={() => handleReceiptClick(txn.receiptNumber)}
                      className="hover:text-brand-600 hover:underline ml-1"
                    >
                      #{txn.receiptNumber}
                    </button>
                  </p>
                </div>
                <div className="text-right flex-shrink-0">
                  <p className="text-sm font-semibold text-emerald-600">{formatCurrency(txn.amountPaid)}</p>
                  <p className="text-xs text-slate-400">{txn.paymentMethod}</p>
                </div>
              </div>
            ))}
            {(!recent || recent.length === 0) && (
              <div className="px-5 py-10 text-center text-slate-400 text-sm">
                No payments yet today
              </div>
            )}
          </div>
        </div>
      </div>

      <DashboardStudentListModal
        isOpen={!!selectedClass}
        onClose={() => setSelectedClass(null)}
        classId={selectedClass?.id || ''}
        className={selectedClass?.name || ''}
        month={currentMonth}
        year={currentYear}
        monthName={getMonthName(currentMonth)}
      />

      <ReceiptModal
        isOpen={!!selectedReceipt}
        onClose={() => setSelectedReceipt(null)}
        receipt={selectedReceipt?.receipt || null}
        student={selectedReceipt?.student || null}
        dues={selectedReceipt?.dues || null}
        lateFee={selectedReceipt?.lateFee || 0}
        concession={selectedReceipt?.concession || 0}
        grandTotal={selectedReceipt?.grandTotal || 0}
      />
    </div>
  )
}

function StatCard({
  label, value, sub, icon, color, loading,
}: {
  label: string; value: string; sub?: string;
  icon: React.ReactNode; color: string; loading?: boolean
}) {
  const colorMap: Record<string, string> = {
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-emerald-50 text-emerald-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
  }
  return (
    <div className="stat-card">
      <div className="flex items-start justify-between">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${colorMap[color]}`}>
          {icon}
        </div>
      </div>
      <div>
        {loading ? (
          <div className="h-8 w-32 bg-slate-100 rounded animate-pulse" />
        ) : (
          <p className="text-2xl font-bold text-slate-900">{value}</p>
        )}
        <p className="text-sm text-slate-500 mt-1">{label}</p>
        {sub && <p className="text-xs text-slate-400 mt-0.5">{sub}</p>}
      </div>
    </div>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}
