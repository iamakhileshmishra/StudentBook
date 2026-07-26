import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { IndianRupee, Edit3, Check, X, Loader2, BookOpen } from 'lucide-react'
import { feesApi, type ClassFeeStructureDto, type FeeStructureRowDto } from '@/api/fees'
import { formatCurrency, cn } from '@/lib/utils'

export function FeeStructurePage() {
  const queryClient = useQueryClient()
  const [editingCell, setEditingCell] = useState<{ classId: string; categoryId: string } | null>(null)
  const [editValue, setEditValue] = useState('')

  const { data: structures, isLoading } = useQuery({
    queryKey: ['fee-structure'],
    queryFn: feesApi.getAllStructures,
    staleTime: 1000 * 60 * 5,
  })

  const { mutate: updateAmount, isPending: isSaving } = useMutation({
    mutationFn: feesApi.updateAmount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-structure'] })
      setEditingCell(null)
    },
  })

  const startEdit = (classId: string, row: FeeStructureRowDto) => {
    setEditingCell({ classId, categoryId: row.feeCategoryId })
    setEditValue(row.defaultAmount.toString())
  }

  const cancelEdit = () => setEditingCell(null)

  const saveEdit = (classId: string, categoryId: string) => {
    const amount = parseFloat(editValue)
    if (isNaN(amount) || amount < 0) return
    updateAmount({ classId, feeCategoryId: categoryId, amount })
  }

  const isEditing = (classId: string, categoryId: string) =>
    editingCell?.classId === classId && editingCell?.categoryId === categoryId

  return (
    <div className="p-8 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Fee Structure</h1>
          <p className="text-slate-500 text-sm mt-1">Monthly fee amounts per class — Academic Year 2025-26</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-brand-50 rounded-xl border border-brand-100">
          <IndianRupee className="w-4 h-4 text-brand-600" />
          <span className="text-sm font-medium text-brand-700">Click any amount to edit</span>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-24">
          <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-2 gap-6">
          {structures?.map((cls) => (
            <ClassFeeCard
              key={cls.classId}
              cls={cls}
              isEditing={isEditing}
              editValue={editValue}
              setEditValue={setEditValue}
              onStartEdit={startEdit}
              onSave={saveEdit}
              onCancel={cancelEdit}
              isSaving={isSaving}
            />
          ))}
        </div>
      )}
    </div>
  )
}

interface CardProps {
  cls: ClassFeeStructureDto
  isEditing: (classId: string, catId: string) => boolean
  editValue: string
  setEditValue: (v: string) => void
  onStartEdit: (classId: string, row: FeeStructureRowDto) => void
  onSave: (classId: string, catId: string) => void
  onCancel: () => void
  isSaving: boolean
}

function ClassFeeCard({ cls, isEditing, editValue, setEditValue, onStartEdit, onSave, onCancel, isSaving }: CardProps) {
  return (
    <div className="card">
      {/* Card header */}
      <div className="card-header flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-brand-100 flex items-center justify-center">
            <BookOpen className="w-4 h-4 text-brand-600" />
          </div>
          <div>
            <h2 className="font-bold text-slate-800">{cls.className}</h2>
            <p className="text-xs text-slate-400">Monthly total</p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-lg font-bold text-brand-600">{formatCurrency(cls.totalMonthlyFee)}</p>
          <p className="text-xs text-slate-400">/month</p>
        </div>
      </div>

      {/* Fee rows */}
      <div className="divide-y divide-slate-50">
        {cls.rows.map((row) => {
          const editing = isEditing(cls.classId, row.feeCategoryId)
          return (
            <div
              key={row.feeCategoryId}
              className={cn(
                'flex items-center justify-between px-5 py-3 group transition-colors',
                editing ? 'bg-brand-50' : 'hover:bg-slate-50'
              )}
            >
              <span className="text-sm text-slate-600 font-medium">{row.categoryName}</span>

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
                        if (e.key === 'Enter') onSave(cls.classId, row.feeCategoryId)
                        if (e.key === 'Escape') onCancel()
                      }}
                      className="w-28 pl-7 pr-2 py-1.5 text-sm border-2 border-brand-400 rounded-lg outline-none focus:border-brand-600"
                    />
                  </div>
                  <button
                    onClick={() => onSave(cls.classId, row.feeCategoryId)}
                    disabled={isSaving}
                    className="w-7 h-7 rounded-lg bg-emerald-500 text-white flex items-center justify-center hover:bg-emerald-600 disabled:opacity-50"
                  >
                    {isSaving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                  </button>
                  <button
                    onClick={onCancel}
                    className="w-7 h-7 rounded-lg bg-slate-200 text-slate-600 flex items-center justify-center hover:bg-slate-300"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => onStartEdit(cls.classId, row)}
                  className="flex items-center gap-2 group/btn"
                >
                  <span className={cn(
                    'text-sm font-semibold',
                    row.defaultAmount > 0 ? 'text-slate-800' : 'text-slate-300'
                  )}>
                    {row.defaultAmount > 0 ? formatCurrency(row.defaultAmount) : '—'}
                  </span>
                  <Edit3 className="w-3.5 h-3.5 text-slate-300 group-hover/btn:text-brand-500 opacity-0 group-hover:opacity-100 transition-opacity" />
                </button>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
