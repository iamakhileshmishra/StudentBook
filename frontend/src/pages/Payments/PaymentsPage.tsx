import { useQuery } from '@tanstack/react-query'
import { Printer, Search, ArrowRightLeft } from 'lucide-react'
import { useState, useEffect } from 'react'
import { paymentsApi, FullReceiptDto } from '@/api/payments'
import { formatCurrency } from '@/lib/utils'
import { ReceiptModal } from '../FeeCollection/components/ReceiptModal'

// Custom hook for debouncing search input
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay)
    return () => clearTimeout(handler)
  }, [value, delay])
  return debouncedValue
}

export function PaymentsPage() {
  const [page, setPage] = useState(0)
  const size = 20
  
  const [searchTerm, setSearchTerm] = useState('')
  const debouncedSearch = useDebounce(searchTerm, 500)

  const [selectedReceipt, setSelectedReceipt] = useState<FullReceiptDto | null>(null)
  const [isReceiptLoading, setIsReceiptLoading] = useState(false)

  // Reset page to 0 when search changes
  useEffect(() => {
    setPage(0)
  }, [debouncedSearch])

  const { data, isLoading } = useQuery({
    queryKey: ['payments-history', page, size, debouncedSearch],
    queryFn: () => paymentsApi.getHistory(page, size, debouncedSearch)
  })

  const handlePrint = async (receiptNumber: string) => {
    try {
      setIsReceiptLoading(true)
      const rcpt = await paymentsApi.getReceipt(receiptNumber)
      setSelectedReceipt(rcpt)
    } catch (e) {
      console.error(e)
    } finally {
      setIsReceiptLoading(false)
    }
  }

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <div className="flex items-center gap-4 mb-8">
        <div className="w-12 h-12 bg-brand-100 rounded-2xl flex items-center justify-center text-brand-600">
          <ArrowRightLeft className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Payment History</h1>
          <p className="text-slate-500">View and print past fee receipts</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="p-4 border-b border-slate-200 flex items-center gap-4 bg-slate-50">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input 
              type="text" 
              placeholder="Search by receipt or student name..." 
              className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="text-xs text-slate-500 uppercase bg-slate-50/50 border-b border-slate-200">
              <tr>
                <th className="px-6 py-4 font-semibold">Receipt No</th>
                <th className="px-6 py-4 font-semibold">Student Details</th>
                <th className="px-6 py-4 font-semibold">Date</th>
                <th className="px-6 py-4 font-semibold">Amount</th>
                <th className="px-6 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={5} className="px-6 py-10 text-center text-slate-400">Loading history...</td>
                </tr>
              ) : data?.content.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-10 text-center text-slate-400">No payment history found.</td>
                </tr>
              ) : (
                data?.content.map(tx => (
                  <tr key={tx.id} className="hover:bg-slate-50/80 transition-colors group">
                    <td className="px-6 py-4">
                      <button 
                        onClick={() => handlePrint(tx.receiptNumber)}
                        className="font-medium text-brand-600 hover:text-brand-700 hover:underline"
                      >
                        #{tx.receiptNumber}
                      </button>
                    </td>
                    <td className="px-6 py-4">
                      <div className="font-medium text-slate-800">{tx.studentName}</div>
                      <div className="text-xs text-slate-500">{tx.className} • Adm: {tx.admissionNumber}</div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-slate-800">{new Date(tx.confirmedAt).toLocaleDateString()}</div>
                      <div className="text-xs text-slate-500">{new Date(tx.confirmedAt).toLocaleTimeString()}</div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="font-semibold text-emerald-600">{formatCurrency(tx.amountPaid)}</div>
                      <div className="text-xs text-slate-500">{tx.paymentMethod}</div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handlePrint(tx.receiptNumber)}
                        disabled={isReceiptLoading}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-600 bg-white border border-slate-200 rounded hover:bg-slate-50 hover:text-slate-900 transition-colors disabled:opacity-50"
                      >
                        <Printer className="w-3.5 h-3.5" />
                        Print
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination controls */}
        {data && data.totalPages > 1 && (
          <div className="p-4 border-t border-slate-200 flex items-center justify-between bg-slate-50">
            <span className="text-sm text-slate-500">
              Showing page {data.number + 1} of {data.totalPages}
            </span>
            <div className="flex gap-2">
              <button 
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm font-medium text-slate-600 bg-white border border-slate-200 rounded disabled:opacity-50"
              >
                Previous
              </button>
              <button 
                onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))}
                disabled={page === data.totalPages - 1}
                className="px-3 py-1.5 text-sm font-medium text-slate-600 bg-white border border-slate-200 rounded disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

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
