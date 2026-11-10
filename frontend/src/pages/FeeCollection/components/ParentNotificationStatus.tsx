import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Mail, MailCheck, MailX, Loader2, Link2, Check } from 'lucide-react'
import { paymentsApi, parentReceiptUrl, NotificationLogDto } from '@/api/payments'

/** Stop polling for the background email after this long */
const POLL_FOR_MS = 20_000

/**
 * Shows whether the parent was emailed about this receipt, with Resend and Copy-link.
 * The email is sent in the background after payment, so right after collecting a fee
 * this polls briefly until the outcome is recorded.
 */
export function ParentNotificationStatus({ receiptNumber, shareToken, confirmedAt }: {
  receiptNumber: string
  shareToken?: string
  confirmedAt?: string
}) {
  const queryClient = useQueryClient()
  // Only a just-collected payment can still have its email on the way
  const [polling, setPolling] = useState(() =>
    // (a minute's slack for clock differences between this device and the server)
    !!confirmedAt && Date.now() - new Date(confirmedAt).getTime() < 60_000)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!polling) return
    const t = setTimeout(() => setPolling(false), POLL_FOR_MS)
    return () => clearTimeout(t)
  }, [polling])

  const { data: logs = [] } = useQuery({
    queryKey: ['receipt-notifications', receiptNumber],
    queryFn: () => paymentsApi.getNotifications(receiptNumber),
    refetchInterval: (q) => (polling && (q.state.data?.length ?? 0) === 0 ? 2000 : false),
  })

  const resend = useMutation({
    mutationFn: () => paymentsApi.resendEmail(receiptNumber),
    onMutate: () => setError(''),
    onSuccess: (log) => {
      queryClient.setQueryData<NotificationLogDto[]>(['receipt-notifications', receiptNumber],
        (old = []) => [log, ...old])
    },
    onError: (err: any) => setError(err?.response?.data?.message || 'Could not send the email. Please try again.'),
  })

  const copyLink = async () => {
    if (!shareToken) return
    try {
      await navigator.clipboard.writeText(parentReceiptUrl(shareToken))
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      window.prompt('Copy the receipt link:', parentReceiptUrl(shareToken))
    }
  }

  const latest = logs.find((l) => l.channel === 'EMAIL')
  const waiting = !latest && polling

  let icon = <Mail className="w-4 h-4 text-slate-400" />
  let text = 'No email sent to the parent yet'
  let tone = 'text-slate-600'
  if (waiting) {
    icon = <Loader2 className="w-4 h-4 animate-spin text-slate-400" />
    text = 'Emailing the parent…'
  } else if (latest?.status === 'SENT') {
    icon = <MailCheck className="w-4 h-4 text-green-600" />
    text = `Receipt emailed to ${latest.recipient}`
    tone = 'text-green-700'
  } else if (latest?.status === 'FAILED') {
    icon = <MailX className="w-4 h-4 text-red-600" />
    text = `Email to ${latest.recipient} failed`
    tone = 'text-red-700'
  } else if (latest?.status === 'SKIPPED') {
    text = latest.detail === 'No parent email on file'
      ? "Not emailed: add a parent email on the student's profile"
      : `Not emailed: ${latest.detail}`
  }

  // Always offer it: the log shows past attempts, but the server reads the student's
  // current parent email at send time, so an email added since the payment is used
  const canResend = !waiting

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 px-4 py-2.5 border-b border-slate-100 bg-slate-50 text-sm print:hidden">
      <div className={`flex items-center gap-2 ${tone}`}>
        {icon}
        <span>{text}</span>
      </div>
      <div className="flex items-center gap-2 ml-auto">
        {canResend && (
          <button
            onClick={() => resend.mutate()}
            disabled={resend.isPending}
            className="px-3 py-1.5 text-xs font-semibold text-brand-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100 disabled:opacity-50"
          >
            {resend.isPending ? 'Sending…' : latest?.status === 'SENT' ? 'Resend email' : 'Send email'}
          </button>
        )}
        {shareToken && (
          <button
            onClick={copyLink}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-green-600" /> : <Link2 className="w-3.5 h-3.5" />}
            {copied ? 'Copied' : 'Copy parent link'}
          </button>
        )}
      </div>
      {error && <p className="w-full text-xs text-red-600">{error}</p>}
    </div>
  )
}
