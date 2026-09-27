import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { Loader2, LogOut } from 'lucide-react'

interface Props {
  onConfirm: () => Promise<void>
  onClose: () => void
}

export function LogoutConfirmModal({ onConfirm, onClose }: Props) {
  const [isPending, setIsPending] = useState(false)

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape' && !isPending) onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose, isPending])

  const handleConfirm = async () => {
    setIsPending(true)
    await onConfirm()
  }

  // Portal to <body> so the sidebar's stacking context (fixed, z-30) can't trap the overlay
  return createPortal(
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={() => { if (!isPending) onClose() }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="logout-title"
        className="bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 animate-fade-in flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Body */}
        <div className="p-6 flex items-start gap-4">
          <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center text-red-600 flex-shrink-0">
            <LogOut className="w-5 h-5" />
          </div>
          <div>
            <h2 id="logout-title" className="text-lg font-bold text-slate-900">Log out?</h2>
            <p className="text-sm text-slate-600 mt-1">
              You will need to sign in again to access the dashboard.
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-slate-50 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            disabled={isPending}
            autoFocus
            className="px-4 py-2 text-slate-600 hover:bg-slate-200 rounded-lg text-sm font-semibold transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={isPending}
            className="px-6 py-2 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-lg text-sm font-bold shadow-sm transition-colors flex items-center gap-2"
          >
            {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Log out'}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
