import React, { useState, useEffect } from 'react';
import { X, Calendar, AlertCircle, ChevronRight, CheckCircle2, Loader2, IndianRupee } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { settingsApi, PromotionDataResponse, PromotionRequest, StudentPromotionDto, PromotionDecision, ClassData, SectionData } from '@/api/settings';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

type WizardStep = 'CONFIG' | 'MAPPING' | 'CONFIRM';

export function AcademicYearPromotionModal({ isOpen, onClose }: Props) {
  const queryClient = useQueryClient();
  const [step, setStep] = useState<WizardStep>('CONFIG');

  const { data: promoData, isLoading: isLoadingData } = useQuery({
    queryKey: ['promotionData'],
    queryFn: settingsApi.getPromotionData,
    enabled: isOpen
  });

  const [newYearName, setNewYearName] = useState('');
  const [newStartDate, setNewStartDate] = useState('');
  const [newEndDate, setNewEndDate] = useState('');

  // Map of studentId -> StudentPromotionDto
  const [promotions, setPromotions] = useState<Record<string, StudentPromotionDto>>({});
  
  const [selectedClassId, setSelectedClassId] = useState<string>('');

  const mutation = useMutation({
    mutationFn: settingsApi.promoteStudents,
    onSuccess: () => {
      queryClient.invalidateQueries();
      alert('Academic year closed and students promoted successfully!');
      onClose();
    },
    onError: (err: any) => {
      const data = err?.response?.data;
      alert((data?.fieldErrors && Object.values(data.fieldErrors)[0]) || data?.message || 'Failed to process promotion. Please try again.');
    }
  });

  if (!isOpen) return null;

  const highestClassOrder = promoData?.classes.reduce((max, c) => Math.max(max, c.numericOrder), 0) || 0;

  const handleNextConfig = () => {
    if (!newYearName || !newStartDate || !newEndDate) {
      alert('Please fill in all academic year details.');
      return;
    }
    
    // Initialize promotions with defaults if not set
    if (Object.keys(promotions).length === 0 && promoData) {
      const initialPromotions: Record<string, StudentPromotionDto> = {};
      
      promoData.currentStudents.forEach(student => {
        // Find current class
        const currentClass = promoData.classes.find(c => c.name === student.className);
        
        let nextClassId: string | undefined = undefined;
        let nextSectionId: string | undefined = undefined;
        let decision: PromotionDecision = 'PROMOTED';
        
        if (currentClass) {
          // If they are in the highest class, default to GRADUATED
          if (currentClass.numericOrder === highestClassOrder) {
            decision = 'GRADUATED';
          } else {
            // Find next class based on numericOrder
            const nextClass = promoData.classes.find(c => c.numericOrder === currentClass.numericOrder + 1);
            if (nextClass) {
              nextClassId = nextClass.id;
              // Default to first section of next class
              if (nextClass.sections.length > 0) {
                nextSectionId = nextClass.sections[0].id;
              }
            }
          }
        }
        
        initialPromotions[student.id] = {
          studentId: student.id,
          decision,
          nextClassId,
          nextSectionId
        };
      });
      setPromotions(initialPromotions);
      if (promoData.classes.length > 0) {
        setSelectedClassId(promoData.classes[0].id);
      }
    }
    
    setStep('MAPPING');
  };

  const handleConfirm = () => {
    if (confirm('Are you absolutely sure? This will close the current academic year, generate arrears, and process all students. This cannot be undone.')) {
      mutation.mutate({
        newAcademicYearName: newYearName,
        newStartDate,
        newEndDate,
        promotions: Object.values(promotions)
      });
    }
  };

  const selectedClass = promoData?.classes.find(c => c.id === selectedClassId);
  const studentsInSelectedClass = promoData?.currentStudents.filter(s => s.className === selectedClass?.name) || [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-5xl max-h-[90vh] flex flex-col overflow-hidden animate-slide-up">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-red-100 text-red-600 flex items-center justify-center">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800">Close Academic Year & Promote Students</h2>
              <p className="text-sm text-slate-500">Step {step === 'CONFIG' ? '1' : step === 'MAPPING' ? '2' : '3'} of 3</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {isLoadingData ? (
            <div className="flex flex-col items-center justify-center py-20 text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin mb-4 text-indigo-600" />
              <p>Loading school data & computing arrears...</p>
            </div>
          ) : (
            <>
              {step === 'CONFIG' && (
                <div className="max-w-xl mx-auto space-y-6">
                  <div className="p-4 bg-indigo-50 border border-indigo-100 rounded-xl text-indigo-800 text-sm flex gap-3">
                    <AlertCircle className="w-5 h-5 shrink-0" />
                    <div>
                      <p className="font-bold mb-1">Before you proceed:</p>
                      <ul className="list-disc pl-4 space-y-1">
                        <li>This action will mark the current Academic Year as closed.</li>
                        <li>All unpaid fees for the current year will be carried forward as <strong>Previous Year Arrears</strong> into the new year.</li>
                        <li>Students will be moved to their new classes or marked as Graduated/Left.</li>
                      </ul>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <h3 className="font-bold text-slate-800">New Academic Year Configuration</h3>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Name (e.g. 2026-2027)</label>
                      <input 
                        type="text" 
                        value={newYearName}
                        onChange={e => setNewYearName(e.target.value)}
                        placeholder="2026-2027"
                        className="w-full px-4 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                      />
                    </div>
                    <div className="flex gap-4">
                      <div className="flex-1">
                        <label className="block text-sm font-medium text-slate-700 mb-1">Start Date</label>
                        <input 
                          type="date" 
                          value={newStartDate}
                          onChange={e => setNewStartDate(e.target.value)}
                          className="w-full px-4 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                        />
                      </div>
                      <div className="flex-1">
                        <label className="block text-sm font-medium text-slate-700 mb-1">End Date</label>
                        <input 
                          type="date" 
                          value={newEndDate}
                          onChange={e => setNewEndDate(e.target.value)}
                          className="w-full px-4 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {step === 'MAPPING' && (
                <div className="space-y-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-xl font-bold text-slate-800">Class Mapping</h3>
                      <p className="text-sm text-slate-500">Review pending dues and select the status for each student.</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <label className="text-sm font-medium text-slate-700">Select Class:</label>
                      <select 
                        value={selectedClassId}
                        onChange={(e) => setSelectedClassId(e.target.value)}
                        className="border-slate-300 rounded-lg text-sm bg-slate-50 py-2 pl-3 pr-8 min-w-[150px] shadow-sm"
                      >
                        {promoData?.classes.map(c => (
                          <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  {!selectedClass ? null : studentsInSelectedClass.length === 0 ? (
                    <div className="p-8 text-center bg-slate-50 rounded-xl border border-slate-100 text-slate-500">
                      No active students found in this class.
                    </div>
                  ) : (
                    <div className="border border-slate-200 rounded-xl overflow-hidden">
                      <table className="w-full text-left border-collapse">
                        <thead>
                          <tr className="bg-slate-50 border-b border-slate-200">
                            <th className="px-4 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Student</th>
                            <th className="px-4 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Pending Dues</th>
                            <th className="px-4 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Status</th>
                            <th className="px-4 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Next Class</th>
                            <th className="px-4 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Next Section</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 bg-white">
                          {studentsInSelectedClass.map(student => {
                            const p = promotions[student.id];
                            if (!p) return null;
                            
                            const nextClassOpts = promoData?.classes || [];
                            const selectedNextClass = nextClassOpts.find(c => c.id === p.nextClassId);
                            const isHighestClass = selectedClass.numericOrder === highestClassOrder;
                            const hasPendingDues = student.pendingDues > 0;
                            
                            return (
                              <tr key={student.id} className="hover:bg-slate-50 transition-colors">
                                <td className="px-4 py-3">
                                  <div className="font-medium text-slate-800">{student.fullName}</div>
                                  <div className="text-xs text-slate-500">{student.admissionNumber}{student.sectionName ? ` | ${student.sectionName}` : ''}</div>
                                </td>
                                <td className="px-4 py-3">
                                  <div className={`inline-flex items-center gap-1 font-medium ${hasPendingDues ? 'text-red-600' : 'text-slate-500'}`}>
                                    <IndianRupee className="w-3 h-3" />
                                    {student.pendingDues || '0.00'}
                                  </div>
                                </td>
                                <td className="px-4 py-3">
                                  <select 
                                    value={p.decision}
                                    onChange={e => {
                                      const newDecision = e.target.value as PromotionDecision;
                                      const newP = { ...p, decision: newDecision };
                                      if (newDecision === 'RETAINED') {
                                        newP.nextClassId = selectedClass.id;
                                        newP.nextSectionId = selectedClass.sections[0]?.id || '';
                                      } else if (newDecision === 'PROMOTED') {
                                        const nextClass = promoData?.classes.find(c => c.numericOrder === selectedClass.numericOrder + 1);
                                        newP.nextClassId = nextClass?.id || '';
                                        newP.nextSectionId = nextClass?.sections[0]?.id || '';
                                      }
                                      setPromotions({...promotions, [student.id]: newP});
                                    }}
                                    className="border-slate-300 rounded-lg text-sm bg-white py-1.5 shadow-sm"
                                  >
                                    <option value="PROMOTED">Promoted</option>
                                    <option value="RETAINED">Retained (Failed)</option>
                                    <option value="LEFT">Left School</option>
                                    {isHighestClass && <option value="GRADUATED">Graduated</option>}
                                  </select>
                                </td>
                                <td className="px-4 py-3">
                                  {p.decision === 'PROMOTED' || p.decision === 'RETAINED' ? (
                                    <select 
                                      value={p.nextClassId || ''}
                                      onChange={e => {
                                        const cid = e.target.value;
                                        const c = nextClassOpts.find(x => x.id === cid);
                                        setPromotions({...promotions, [student.id]: {...p, nextClassId: cid, nextSectionId: c?.sections[0]?.id || ''}});
                                      }}
                                      className="border-slate-300 rounded-lg text-sm bg-white py-1.5 w-full shadow-sm"
                                    >
                                      <option value="">Select Class</option>
                                      {nextClassOpts.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                                    </select>
                                  ) : (
                                    <span className="text-slate-400 text-sm italic">N/A</span>
                                  )}
                                </td>
                                <td className="px-4 py-3">
                                  {(p.decision === 'PROMOTED' || p.decision === 'RETAINED') && selectedNextClass ? (
                                    <select 
                                      value={p.nextSectionId || ''}
                                      onChange={e => setPromotions({...promotions, [student.id]: {...p, nextSectionId: e.target.value}})}
                                      className="border-slate-300 rounded-lg text-sm bg-white py-1.5 w-full shadow-sm"
                                    >
                                      {selectedNextClass.sections.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                                    </select>
                                  ) : (
                                    <span className="text-slate-400 text-sm italic">N/A</span>
                                  )}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}

              {step === 'CONFIRM' && (
                <div className="max-w-2xl mx-auto space-y-8 text-center">
                  <div className="w-20 h-20 bg-green-100 text-green-600 rounded-full flex items-center justify-center mx-auto">
                    <CheckCircle2 className="w-10 h-10" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-slate-800 mb-2">Ready to Promote</h2>
                    <p className="text-slate-600">You are about to close the current year and migrate all students to <strong>{newYearName}</strong>.</p>
                  </div>
                  
                  <div className="grid grid-cols-4 gap-4 text-left">
                    <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                      <div className="text-xs font-bold text-slate-400 uppercase mb-1">Promoted</div>
                      <div className="text-2xl font-bold text-slate-800">{Object.values(promotions).filter(p => p.decision === 'PROMOTED').length}</div>
                    </div>
                    <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                      <div className="text-xs font-bold text-slate-400 uppercase mb-1">Retained</div>
                      <div className="text-2xl font-bold text-slate-800">{Object.values(promotions).filter(p => p.decision === 'RETAINED').length}</div>
                    </div>
                    <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                      <div className="text-xs font-bold text-slate-400 uppercase mb-1">Left School</div>
                      <div className="text-2xl font-bold text-slate-800">{Object.values(promotions).filter(p => p.decision === 'LEFT').length}</div>
                    </div>
                    <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                      <div className="text-xs font-bold text-slate-400 uppercase mb-1">Graduated</div>
                      <div className="text-2xl font-bold text-slate-800">{Object.values(promotions).filter(p => p.decision === 'GRADUATED').length}</div>
                    </div>
                  </div>

                  <p className="text-sm text-slate-500 bg-slate-50 p-4 rounded-lg border border-slate-100 text-left">
                    <strong>Note:</strong> Students marked as Promoted or Retained will have their pending dues automatically injected as "Previous Year Arrears" in the new academic year's ledger.
                  </p>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-100 bg-slate-50 flex justify-between items-center">
          <div>
            {step !== 'CONFIG' && (
              <button 
                disabled={mutation.isPending}
                onClick={() => {
                  if (step === 'CONFIRM') setStep('MAPPING');
                  else if (step === 'MAPPING') setStep('CONFIG');
                }}
                className="px-4 py-2 text-slate-600 font-medium hover:bg-slate-200 rounded-lg transition-colors"
              >
                Back
              </button>
            )}
          </div>
          <div>
            {step === 'CONFIG' && (
              <button 
                onClick={handleNextConfig}
                className="px-6 py-2 bg-indigo-600 text-white font-medium rounded-xl hover:bg-indigo-700 transition-colors flex items-center gap-2"
              >
                Next Step <ChevronRight className="w-4 h-4" />
              </button>
            )}
            
            {step === 'MAPPING' && (
              <button 
                onClick={() => setStep('CONFIRM')}
                className="px-6 py-2 bg-indigo-600 text-white font-medium rounded-xl hover:bg-indigo-700 transition-colors flex items-center gap-2"
              >
                Review Summary <ChevronRight className="w-4 h-4" />
              </button>
            )}

            {step === 'CONFIRM' && (
              <button 
                onClick={handleConfirm}
                disabled={mutation.isPending}
                className="px-6 py-2 bg-red-600 text-white font-medium rounded-xl hover:bg-red-700 transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                {mutation.isPending ? (
                  <><Loader2 className="w-4 h-4 animate-spin" /> Processing...</>
                ) : (
                  <>Confirm & Execute Promotion</>
                )}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
