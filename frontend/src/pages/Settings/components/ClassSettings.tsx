import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, Layers, AlertTriangle, X } from 'lucide-react'
import { academicApi } from '@/api/academic'

export function ClassSettings() {
  const queryClient = useQueryClient()
  const { data: classes, isLoading } = useQuery({
    queryKey: ['classes'],
    queryFn: academicApi.getClasses
  })

  const [editingClass, setEditingClass] = useState<string | null>(null)
  const [editingSection, setEditingSection] = useState<string | null>(null)
  const [tempName, setTempName] = useState('')
  const [tempOrder, setTempOrder] = useState<number | ''>('')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  
  // Modal states
  const [isAddClassOpen, setIsAddClassOpen] = useState(false)
  const [addClassName, setAddClassName] = useState('')
  const [addClassOrder, setAddClassOrder] = useState<number | ''>('')

  const [isAddSectionOpen, setIsAddSectionOpen] = useState(false)
  const [addSectionTarget, setAddSectionTarget] = useState<{id: string, name: string} | null>(null)
  const [addSectionName, setAddSectionName] = useState('')

  const createClassMutation = useMutation({
    mutationFn: academicApi.createClass,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] })
      queryClient.invalidateQueries({ queryKey: ['fee-structure'] })
      setIsAddClassOpen(false)
      setAddClassName('')
      setAddClassOrder('')
      setErrorMsg(null)
      alert("Class added successfully! Please make sure to adjust the fee structure for this class accordingly in the Fee Structure section.")
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || 'Failed to create class.')
    }
  })

  const updateClassMutation = useMutation({
    mutationFn: academicApi.updateClass,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] })
      queryClient.invalidateQueries({ queryKey: ['fee-structure'] })
      setEditingClass(null)
      setErrorMsg(null)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || 'Failed to update class.')
    }
  })

  const deleteClassMutation = useMutation({
    mutationFn: academicApi.deleteClass,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] })
      queryClient.invalidateQueries({ queryKey: ['fee-structure'] })
    }
  })

  const createSectionMutation = useMutation({
    mutationFn: academicApi.createSection,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] })
      setIsAddSectionOpen(false)
      setAddSectionName('')
      setAddSectionTarget(null)
      setErrorMsg(null)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || 'Failed to create section.')
    }
  })

  const updateSectionMutation = useMutation({
    mutationFn: academicApi.updateSection,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] })
      setEditingSection(null)
      setErrorMsg(null)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || 'Failed to update section.')
    }
  })

  const deleteSectionMutation = useMutation({
    mutationFn: academicApi.deleteSection,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['classes'] })
  })

  const handleAddClass = () => {
    if (!addClassName) return
    createClassMutation.mutate({ 
      name: addClassName, 
      numericOrder: addClassOrder === '' ? undefined : addClassOrder 
    })
  }

  const handleDeleteClass = (id: string, name: string) => {
    if (confirm(`Are you sure you want to delete ${name}? This will hide it from new enrollments. Historical data will remain intact.`)) {
      deleteClassMutation.mutate(id)
    }
  }

  const handleAddSection = () => {
    if (!addSectionName || !addSectionTarget) return
    createSectionMutation.mutate({ 
      classId: addSectionTarget.id, 
      data: { name: addSectionName } 
    })
  }

  const handleDeleteSection = (id: string, name: string) => {
    if (confirm(`Are you sure you want to delete Section ${name}?`)) {
      deleteSectionMutation.mutate(id)
    }
  }

  if (isLoading) return <div className="text-slate-500">Loading classes...</div>

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-lg font-bold text-slate-800">Classes & Sections</h2>
          <p className="text-sm text-slate-500">Manage school hierarchy, classes, and sections</p>
        </div>
        <button
          onClick={() => {
            setIsAddClassOpen(true)
            setErrorMsg(null)
          }}
          className="flex items-center gap-2 px-4 py-2 bg-brand-50 text-brand-700 hover:bg-brand-100 font-medium rounded-lg transition-colors"
        >
          <Plus className="w-4 h-4" />
          Add Class
        </button>
      </div>

      {errorMsg && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex gap-3 items-start">
          <AlertTriangle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="text-sm font-medium text-red-800">Action Failed</h3>
            <p className="text-sm text-red-600 mt-1">{errorMsg}</p>
          </div>
          <button onClick={() => setErrorMsg(null)} className="text-red-400 hover:text-red-600">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      <div className="space-y-4">
        {classes?.map((cls) => (
          <div key={cls.id} className="border border-slate-200 rounded-xl overflow-hidden">
            {/* Class Header */}
            <div className="bg-slate-50 px-4 py-3 flex items-center justify-between border-b border-slate-200">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-600 font-medium text-sm">
                  {cls.numericOrder}
                </div>
                {editingClass === cls.id ? (
                  <div className="flex items-center gap-2">
                    <input 
                      type="text" value={tempName} onChange={e => setTempName(e.target.value)}
                      className="px-2 py-1 text-sm border border-slate-300 rounded focus:outline-none focus:border-brand-500"
                    />
                    <input 
                      type="number" value={tempOrder} onChange={e => setTempOrder(e.target.value === '' ? '' : parseInt(e.target.value) || 0)}
                      className="w-16 px-2 py-1 text-sm border border-slate-300 rounded focus:outline-none focus:border-brand-500"
                      placeholder="Order"
                    />
                    <button 
                      onClick={() => updateClassMutation.mutate({ id: cls.id, data: { name: tempName, numericOrder: tempOrder === '' ? undefined : tempOrder } })}
                      className="text-xs bg-brand-600 text-white px-2 py-1 rounded"
                    >
                      Save
                    </button>
                    <button onClick={() => { setEditingClass(null); setErrorMsg(null) }} className="text-xs text-slate-500 px-2 py-1">Cancel</button>
                  </div>
                ) : (
                  <span className="font-semibold text-slate-800">{cls.name}</span>
                )}
              </div>
              <div className="flex items-center gap-2">
                <button 
                  onClick={() => {
                    setAddSectionTarget({ id: cls.id, name: cls.name })
                    setIsAddSectionOpen(true)
                    setErrorMsg(null)
                  }}
                  className="p-1.5 text-slate-400 hover:text-brand-600 hover:bg-brand-50 rounded"
                  title="Add Section"
                >
                  <Plus className="w-4 h-4" />
                </button>
                <button 
                  onClick={() => { setEditingClass(cls.id); setTempName(cls.name); setTempOrder(cls.numericOrder); setErrorMsg(null) }}
                  className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded"
                  title="Edit Class"
                >
                  <Edit2 className="w-4 h-4" />
                </button>
                <button 
                  onClick={() => handleDeleteClass(cls.id, cls.name)}
                  className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded"
                  title="Delete Class"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Sections */}
            <div className="p-4 bg-white">
              <div className="flex flex-wrap gap-2">
                {cls.sections.length === 0 ? (
                  <p className="text-sm text-slate-400 italic">No sections added yet.</p>
                ) : (
                  cls.sections.map(sec => (
                    <div key={sec.id} className="inline-flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5">
                      {editingSection === sec.id ? (
                        <div className="flex items-center gap-1">
                          <input 
                            type="text" value={tempName} onChange={e => setTempName(e.target.value)}
                            className="w-20 px-1.5 py-0.5 text-sm border border-slate-300 rounded focus:outline-none"
                            autoFocus
                          />
                          <button 
                            onClick={() => updateSectionMutation.mutate({ id: sec.id, data: { name: tempName } })}
                            className="text-[10px] bg-brand-600 text-white px-1.5 py-0.5 rounded"
                          >
                            Save
                          </button>
                        </div>
                      ) : (
                        <>
                          <Layers className="w-3.5 h-3.5 text-slate-400" />
                          <span className="text-sm font-medium text-slate-700">{sec.name}</span>
                          <div className="flex items-center ml-2 border-l border-slate-200 pl-2">
                            <button 
                              onClick={() => { setEditingSection(sec.id); setTempName(sec.name) }}
                              className="text-slate-400 hover:text-blue-600 px-1"
                            >
                              <Edit2 className="w-3 h-3" />
                            </button>
                            <button 
                              onClick={() => handleDeleteSection(sec.id, sec.name)}
                              className="text-slate-400 hover:text-red-600 px-1"
                            >
                              <Trash2 className="w-3 h-3" />
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        ))}
        {classes?.length === 0 && (
          <div className="text-center py-8">
            <AlertTriangle className="w-8 h-8 text-amber-400 mx-auto mb-2" />
            <h3 className="text-sm font-medium text-slate-800">No classes found</h3>
            <p className="text-sm text-slate-500 mt-1">Add your first class to get started.</p>
          </div>
        )}
      </div>

      {/* Add Class Modal */}
      {isAddClassOpen && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="font-bold text-slate-800">Add New Class</h3>
              <button onClick={() => setIsAddClassOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Class Name</label>
                <input 
                  type="text" value={addClassName} onChange={e => setAddClassName(e.target.value)}
                  placeholder="e.g. Std I, Nursery"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
                  autoFocus
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Display Order (Optional)</label>
                <input 
                  type="number" value={addClassOrder} onChange={e => setAddClassOrder(e.target.value === '' ? '' : parseInt(e.target.value) || 0)}
                  placeholder="Leave blank to auto-append"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
                />
                <p className="text-xs text-slate-500 mt-1">Leave blank to automatically add it at the end.</p>
              </div>
            </div>
            <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-3">
              <button 
                onClick={() => setIsAddClassOpen(false)}
                className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800"
              >
                Cancel
              </button>
              <button 
                onClick={handleAddClass}
                disabled={!addClassName || createClassMutation.isPending}
                className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
              >
                {createClassMutation.isPending ? 'Saving...' : 'Add Class'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Section Modal */}
      {isAddSectionOpen && addSectionTarget && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="font-bold text-slate-800">Add Section to {addSectionTarget.name}</h3>
              <button onClick={() => setIsAddSectionOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-6">
              <label className="block text-sm font-medium text-slate-700 mb-1">Section Name</label>
              <input 
                type="text" value={addSectionName} onChange={e => setAddSectionName(e.target.value)}
                placeholder="e.g. A, B, Science"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
                autoFocus
              />
            </div>
            <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-3">
              <button 
                onClick={() => setIsAddSectionOpen(false)}
                className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800"
              >
                Cancel
              </button>
              <button 
                onClick={handleAddSection}
                disabled={!addSectionName || createSectionMutation.isPending}
                className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
              >
                {createSectionMutation.isPending ? 'Saving...' : 'Add Section'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
