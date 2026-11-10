import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Loader2, Printer, FileX2 } from 'lucide-react'
import { paymentsApi } from '@/api/payments'
import { formatCurrency, getMonthName, numberToWords } from '@/lib/utils'

const PAYMENT_MODES: Record<string, string> = {
  CASH: 'Cash',
  UPI: 'UPI',
  CHEQUE: 'Cheque',
  BANK_TRANSFER: 'Bank transfer',
}

/** Receipt a parent opens from the "fee paid" email. No login; the link's token is the key. */
export function PublicReceiptPage() {
  const { token = '' } = useParams()
  const { data: r, isLoading, isError } = useQuery({
    queryKey: ['public-receipt', token],
    queryFn: () => paymentsApi.getPublicReceipt(token),
    retry: false,
  })

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <Loader2 className="w-6 h-6 animate-spin text-brand-600" />
      </div>
    )
  }

  if (isError || !r) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
        <div className="card p-8 max-w-sm text-center">
          <FileX2 className="w-10 h-10 text-slate-400 mx-auto mb-3" />
          <h1 className="text-lg font-bold text-slate-800 mb-1">Receipt not found</h1>
          <p className="text-sm text-slate-500">
            This link is invalid or incomplete. Please check the link in your email, or contact the school.
          </p>
        </div>
      </div>
    )
  }

  const s = r.school
  const address = [s.addressLine1, s.addressLine2, s.city, s.state].filter(Boolean).join(', ')
    + (s.pincode ? ` - ${s.pincode}` : '')
  const period = `${getMonthName(r.collectionMonth)} ${r.collectionYear}`
  const cancelled = r.status !== 'CONFIRMED'

  return (
    <div className="min-h-screen bg-slate-50 py-6 px-4 print:bg-white print:p-0">
      <div className="max-w-xl mx-auto">
        <div className="flex justify-end mb-3 print:hidden">
          <button
            onClick={() => window.print()}
            className="flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-brand-600 rounded-lg hover:bg-brand-700"
          >
            <Printer className="w-4 h-4" />
            Print / Save as PDF
          </button>
        </div>

        <div className="relative bg-white rounded-2xl shadow-sm border border-slate-200 p-5 sm:p-7 text-slate-800 print:shadow-none print:border-0 print:rounded-none">
          {cancelled && (
            <div className="mb-4 rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm font-semibold text-red-700 text-center">
              This receipt has been cancelled by the school
            </div>
          )}

          <div className="text-center pb-4 mb-4 border-b-2 border-slate-800">
            {s.logoUrl && <img src={s.logoUrl} alt="" className="h-12 mx-auto mb-2 object-contain" />}
            <h1 className="text-lg sm:text-xl font-bold uppercase tracking-wide">{s.name}</h1>
            {address && <p className="text-xs text-slate-600">{address}</p>}
            {(s.phone || s.email) && (
              <p className="text-xs text-slate-600">
                {[s.phone && `Phone: ${s.phone}`, s.email && `Email: ${s.email}`].filter(Boolean).join(' | ')}
              </p>
            )}
            <p className="mt-2 text-xs font-bold tracking-widest text-slate-500">FEE RECEIPT</p>
          </div>

          <div className="flex flex-wrap justify-between gap-2 text-sm mb-4">
            <div><span className="text-slate-500">Receipt No:</span> <span className="font-semibold">{r.receiptNumber}</span></div>
            <div><span className="text-slate-500">Date:</span> <span className="font-semibold">{new Date(r.paymentDate).toLocaleDateString('en-IN')}</span></div>
          </div>

          <dl className="bg-slate-50 border border-slate-200 rounded-lg p-3 mb-4 text-sm grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
            <dt className="text-slate-500">Student</dt>
            <dd className="font-semibold">{r.student.firstName} {r.student.lastName}</dd>
            <dt className="text-slate-500">Class</dt>
            <dd className="font-semibold">{r.student.className}{r.student.sectionName ? ` - ${r.student.sectionName}` : ''}</dd>
            {r.student.admissionNumber && <>
              <dt className="text-slate-500">Admission No</dt>
              <dd className="font-semibold">{r.student.admissionNumber}</dd>
            </>}
            {r.student.fatherName && <>
              <dt className="text-slate-500">Father's Name</dt>
              <dd className="font-semibold">{r.student.fatherName}</dd>
            </>}
            <dt className="text-slate-500">Fee Period</dt>
            <dd className="font-semibold">{period}</dd>
          </dl>

          <table className="w-full text-sm mb-3">
            <thead>
              <tr className="border-b-2 border-slate-800">
                <th className="text-left py-1.5">Fee Head</th>
                <th className="text-right py-1.5">Amount</th>
              </tr>
            </thead>
            <tbody>
              {r.lineItems.map((item, i) => (
                <tr key={i} className="border-b border-slate-200">
                  <td className="py-1.5">{item.name}</td>
                  <td className="py-1.5 text-right">{formatCurrency(item.amount)}</td>
                </tr>
              ))}
              {r.lateFee > 0 && (
                <tr className="border-b border-slate-200">
                  <td className="py-1.5">Late Fee</td>
                  <td className="py-1.5 text-right">{formatCurrency(r.lateFee)}</td>
                </tr>
              )}
              {r.concession > 0 && (
                <tr className="border-b border-slate-200 text-emerald-700">
                  <td className="py-1.5">Concession</td>
                  <td className="py-1.5 text-right">-{formatCurrency(r.concession)}</td>
                </tr>
              )}
            </tbody>
            <tfoot>
              <tr className="font-bold border-t-2 border-slate-800">
                <td className="py-2">Total Paid</td>
                <td className="py-2 text-right">{formatCurrency(r.grandTotal)}</td>
              </tr>
            </tfoot>
          </table>

          <p className="text-xs text-slate-600 mb-1">
            <span className="font-semibold">Amount in words:</span> Rupees {numberToWords(r.grandTotal)}
          </p>
          <p className="text-xs text-slate-600">
            <span className="font-semibold">Payment mode:</span> {PAYMENT_MODES[r.paymentMethod] ?? r.paymentMethod}
          </p>

          <p className="mt-6 pt-3 border-t border-dashed border-slate-200 text-[11px] text-slate-400 italic">
            This is a computer-generated receipt.
          </p>
        </div>
      </div>
    </div>
  )
}
