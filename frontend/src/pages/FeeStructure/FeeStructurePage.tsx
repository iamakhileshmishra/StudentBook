import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Edit3, Check, X, Loader2, BookOpen, Layers, Settings2, Plus } from 'lucide-react'
import { feesApi, type ClassFeeStructureDto, type FeeStructureRowDto } from '@/api/fees'
import { formatCurrency, cn } from '@/lib/utils'
import { settingsApi } from '@/api/settings'

export function FeeStructurePage() {
  const queryClient = useQueryClient()
  const [selectedClass, setSelectedClass] = useState<ClassFeeStructureDto | null>(null)
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false)

  // The year label used to be hard-coded as 2025-26 (BUG-20)
  const { data: currentYear } = useQuery({
    queryKey: ['currentAcademicYear'],
    queryFn: settingsApi.getCurrentAcademicYear,
  })

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
          <p className="text-slate-500 text-sm mt-1">Monthly fee amounts per class{currentYear ? ` — Academic Year ${currentYear.name}` : ''}</p>
        </div>
        <button
          onClick={() => setIsCategoryModalOpen(true)}
          className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-700 font-medium rounded-xl hover:bg-slate-50 hover:border-slate-300 transition-colors shadow-sm"
        >
          <Settings2 className="w-4 h-4" />
          <span>Manage Fee Categories</span>
        </button>
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

      {isCategoryModalOpen && (
        <FeeCategoryModal
          onClose={() => setIsCategoryModalOpen(false)}
          queryClient={queryClient}
        />
      )}
    </div>
  )
}

function FeeCategoryModal({ onClose, queryClient }: { onClose: () => void, queryClient: any }) {
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editName, setEditName] = useState('')
  const [isAdding, setIsAdding] = useState(false)
  const [newName, setNewName] = useState('')

  const { data: categories, isLoading } = useQuery({
    queryKey: ['fee-categories'],
    queryFn: feesApi.getCategories,
  })

  const { mutate: createCategory, isPending: isCreating } = useMutation({
    mutationFn: feesApi.createCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-categories'] })
      queryClient.invalidateQueries({ queryKey: ['fee-structure'] })
      setIsAdding(false)
      setNewName('')
    },
  })

  const { mutate: updateCategory, isPending: isUpdating } = useMutation({
    mutationFn: feesApi.updateCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-categories'] })
      queryClient.invalidateQueries({ queryKey: ['fee-structure'] })
      setEditingId(null)
    },
  })

  const handleCreate = () => {
    if (!newName.trim()) return
    createCategory({ name: newName.trim() })
  }

  const handleUpdate = (id: string) => {
    if (!editName.trim()) return
    updateCategory({ id, data: { name: editName.trim() } })
  }

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md flex flex-col max-h-[90vh] animate-in fade-in zoom-in-95 duration-200">
        <div className="flex-shrink-0 flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center">
              <Settings2 className="w-5 h-5 text-indigo-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800">Fee Categories</h2>
              <p className="text-sm text-slate-500 font-medium">Manage columns across all classes</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-2 overflow-y-auto min-h-0">
          {isLoading ? (
            <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-indigo-500" /></div>
          ) : (
            <div className="divide-y divide-slate-50">
              {categories?.map((cat) => (
                <div key={cat.id} className="flex items-center justify-between px-4 py-3.5 group rounded-xl hover:bg-slate-50 transition-colors">
                  {editingId === cat.id ? (
                    <div className="flex items-center gap-2 w-full">
                      <input
                        autoFocus
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleUpdate(cat.id)
                          if (e.key === 'Escape') setEditingId(null)
                        }}
                        className="flex-1 px-3 py-1.5 text-sm border-2 border-indigo-400 rounded-lg outline-none focus:border-indigo-600"
                      />
                      <button onClick={() => handleUpdate(cat.id)} disabled={isUpdating} className="p-1.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50">
                        {isUpdating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                      </button>
                      <button onClick={() => setEditingId(null)} className="p-1.5 bg-slate-200 text-slate-600 rounded-lg hover:bg-slate-300">
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <>
                      <span className="text-sm text-slate-700 font-medium">{cat.name}</span>
                      <button
                        onClick={() => {
                          setEditingId(cat.id)
                          setEditName(cat.name)
                        }}
                        className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                    </>
                  )}
                </div>
              ))}

              <div className="px-2 py-3 mt-2">
                {isAdding ? (
                  <div className="flex items-center gap-2 bg-indigo-50 p-3 rounded-xl border border-indigo-100">
                    <input
                      autoFocus
                      type="text"
                      placeholder="Category name (e.g. Music Fee)"
                      value={newName}
                      onChange={(e) => setNewName(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') handleCreate()
                        if (e.key === 'Escape') setIsAdding(false)
                      }}
                      className="flex-1 px-3 py-1.5 text-sm border-2 border-indigo-300 rounded-lg outline-none focus:border-indigo-500 bg-white"
                    />
                    <button onClick={handleCreate} disabled={isCreating} className="p-1.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50">
                      {isCreating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                    </button>
                    <button onClick={() => setIsAdding(false)} className="p-1.5 bg-white text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50">
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => setIsAdding(true)}
                    className="w-full flex items-center justify-center gap-2 py-2.5 border-2 border-dashed border-slate-200 rounded-xl text-slate-500 hover:border-indigo-300 hover:text-indigo-600 hover:bg-indigo-50 transition-all font-medium text-sm"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Add New Category</span>
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
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
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md flex flex-col max-h-[90vh] animate-in fade-in zoom-in-95 duration-200">
        <div className="flex-shrink-0 flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-blue-100 text-blue-600 flex items-center justify-center">
              <BookOpen className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-800">{cls.className} Fee Structure</h2>
              <p className="text-sm font-semibold text-blue-600 mt-0.5">{formatCurrency(cls.totalMonthlyFee)} / month</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-3 overflow-y-auto min-h-0">
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
