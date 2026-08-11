import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { IndianRupee, Edit3, Check, X, Loader2, BookOpen, Layers } from 'lucide-react'
import { feesApi, type ClassFeeStructureDto, type FeeStructureRowDto } from '@/api/fees'
import { formatCurrency, cn } from '@/lib/utils'

export function FeeStructurePage() {
  const queryClient = useQueryClient()
  const [selectedClass, setSelectedClass] = useState<ClassFeeStructureDto | null>(null)

  const { data: structures, isLoading } = useQuery({
    queryKey: ['fee-structure'],
    queryFn: feesApi.getAllStructures,
    staleTime: 1000 * 60 * 5,
  })

  // When a mutation completes, we might want to update the selectedClass to reflect new totals.
  // Using an effect or finding the updated class from the cache works.
  const currentSelectedClass = structures?.find(c => c.classId === selectedClass?.classId) || null

  return (
    <div className="p-8 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Fee Structure</h1>
          <p className="text-slate-500 text-sm mt-1">Monthly fee amounts per class — Academic Year 2025-26</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-24">
          <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {structures?.map((cls) => (
            <button
              key={cls.classId}
              onClick={() => setSelectedClass(cls)}
              className="group flex flex-col items-start bg-white p-5 rounded-2xl shadow-sm border border-slate-200 hover:border-brand-300 hover:shadow-md transition-all text-left"
            >
              <div className="flex items-center gap-3 w-full mb-3">
                <div className="w-10 h-10 rounded-xl bg-brand-50 flex items-center justify-center flex-shrink-0 group-hover:bg-brand-100 transition-colors">
                  <Layers className="w-5 h-5 text-brand-600" />
                </div>
                <div className="flex-1">
                  <h3 className="font-bold text-slate-800 text-lg">{cls.className}</h3>
                </div>
              </div>
              <div className="mt-auto">
                <p className="text-sm text-slate-500 font-medium mb-0.5">Monthly Total</p>
                <div className="flex items-baseline gap-1">
                  <span className="text-2xl font-bold text-brand-700">{formatCurrency(cls.totalMonthlyFee)}</span>
                  <span className="text-xs font-medium text-slate-400">/mo</span>
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      {selectedClass && currentSelectedClass && (
        <FeeStructureModal
          cls={currentSelectedClass}
          onClose={() => setSelectedClass(null)}
          queryClient={queryClient}
        />
      )}
    </div>
  )
}

function FeeStructureModal({ cls, onClose, queryClient }: { cls: ClassFeeStructureDto, onClose: () => void, queryClient: any }) {
  const [editingCell, setEditingCell] = useState<string | null>(null)
  const [editValue, setEditValue] = useState('')

  const { mutate: updateAmount, isPending: isSaving } = useMutation({
    mutationFn: feesApi.updateAmount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-structure'] })
      setEditingCell(null)
    },
  })

  const startEdit = (row: FeeStructureRowDto) => {
    setEditingCell(row.feeCategoryId)
    setEditValue(row.defaultAmount.toString())
  }

  const cancelEdit = () => setEditingCell(null)

  const saveEdit = (categoryId: string) => {
    const amount = parseFloat(editValue)
    if (isNaN(amount) || amount < 0) return
    updateAmount({ classId: cls.classId, feeCategoryId: categoryId, amount })
  }

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-brand-100 flex items-center justify-center">
              <BookOpen className="w-5 h-5 text-brand-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800">{cls.className} Fee Structure</h2>
              <p className="text-sm text-brand-600 font-semibold">{formatCurrency(cls.totalMonthlyFee)} / month</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-2">
          <div className="divide-y divide-slate-50">
            {cls.rows.map((row) => {
              const editing = editingCell === row.feeCategoryId
              return (
                <div
                  key={row.feeCategoryId}
                  className={cn(
                    'flex items-center justify-between px-4 py-3.5 group rounded-xl transition-colors',
                    editing ? 'bg-brand-50' : 'hover:bg-slate-50'
                  )}
                >
                  <span className="text-sm text-slate-700 font-medium">{row.categoryName}</span>

                  {editing ? (
                    <div className="flex items-center gap-2">
                      <div className="relative">
                        <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400 text-sm">₹</span>
                        <input
                          autoFocus
                          type="number"
                          min="0"
                          step="50"
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') saveEdit(row.feeCategoryId)
                            if (e.key === 'Escape') cancelEdit()
                          }}
                          className="w-28 pl-7 pr-2 py-1.5 text-sm border-2 border-brand-400 rounded-lg outline-none focus:border-brand-600 bg-white"
                        />
                      </div>
                      <button
                        onClick={() => saveEdit(row.feeCategoryId)}
                        disabled={isSaving}
                        className="w-8 h-8 rounded-lg bg-brand-600 text-white flex items-center justify-center hover:bg-brand-700 disabled:opacity-50 transition-colors shadow-sm"
                      >
                        {isSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                      </button>
                      <button
                        onClick={cancelEdit}
                        className="w-8 h-8 rounded-lg bg-slate-200 text-slate-600 flex items-center justify-center hover:bg-slate-300 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => startEdit(row)}
                      className="flex items-center gap-3 group/btn px-2 py-1 -mr-2 rounded-lg hover:bg-white hover:shadow-sm transition-all"
                    >
                      <span className={cn(
                        'text-sm font-bold',
                        row.defaultAmount > 0 ? 'text-slate-800' : 'text-slate-300'
                      )}>
                        {row.defaultAmount > 0 ? formatCurrency(row.defaultAmount) : '—'}
                      </span>
                      <Edit3 className="w-4 h-4 text-slate-300 group-hover/btn:text-brand-500 transition-colors" />
                    </button>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}
