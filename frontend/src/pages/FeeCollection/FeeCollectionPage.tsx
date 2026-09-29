import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Search, User, Calendar, CreditCard, Loader2, CheckCircle2 } from 'lucide-react';
import { studentsApi, StudentSummaryDto } from '../../api/students';
import { paymentsApi, DuesResponse, CollectFeeRequest, PaymentReceiptDto } from '../../api/payments';
import { settingsApi } from '../../api/settings';
import { ReceiptModal } from './components/ReceiptModal';

export const FeeCollectionPage = () => {
  // Panel 1: Student Search
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStudent, setSelectedStudent] = useState<StudentSummaryDto | null>(null);

  const { data: studentsData, isFetching: isSearching } = useQuery({
    queryKey: ['students-search', searchQuery],
    queryFn: () => studentsApi.search({ q: searchQuery, size: 5 }),
    enabled: searchQuery.length > 2,
  });

  // Academic Year State
  const { data: currentYear } = useQuery({
    queryKey: ['currentAcademicYear'],
    queryFn: settingsApi.getCurrentAcademicYear
  });

  const startYear = currentYear ? new Date(currentYear.startDate).getFullYear() : new Date().getFullYear();
  const startMonth = currentYear ? new Date(currentYear.startDate).getMonth() + 1 : 1;

  // Panel 2: Period Selection
  const [month, setMonth] = useState<number>(new Date().getMonth() + 1);
  const [year, setYear] = useState<number>(new Date().getFullYear());

  // Panel 3: Cart & Payment
  const [dues, setDues] = useState<DuesResponse | null>(null);
  const [lateFee, setLateFee] = useState<number>(0);
  const [concession, setConcession] = useState<number>(0);
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'UPI' | 'CHEQUE' | 'BANK_TRANSFER'>('CASH');
  
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isReceiptModalOpen, setIsReceiptModalOpen] = useState(false);
  
  // Receipt Modal State
  const [receipt, setReceipt] = useState<PaymentReceiptDto | null>(null);

  // Already Paid State
  const [isAlreadyPaidOpen, setIsAlreadyPaidOpen] = useState(false);
  const [paidReceiptNumber, setPaidReceiptNumber] = useState<string | null>(null);

  const duesMutation = useMutation({
    mutationFn: () => paymentsApi.getDues(selectedStudent!.id, month, year),
    onSuccess: (data) => {
      if (data.alreadyPaid) {
        setPaidReceiptNumber(data.paidReceiptNumber || null);
        setIsAlreadyPaidOpen(true);
        setDues(null);
      } else {
        setDues(data);
        setLateFee(data.expectedLateFee || 0);
        setConcession(data.expectedConcession || (selectedStudent?.concessionAmount || 0));
      }
    },
  });

  const collectMutation = useMutation({
    mutationFn: (data: CollectFeeRequest) => paymentsApi.collectFee(data),
    onSuccess: (data) => {
      setReceipt(data);
      setIsReceiptModalOpen(true);
    },
    // Failures used to be silent; show the server's reason (validation, already paid, etc.)
    onError: (err: any) => {
      const data = err?.response?.data;
      const fieldError = data?.fieldErrors && Object.values(data.fieldErrors)[0];
      alert(fieldError || data?.message || 'Could not collect the fee. Please try again.');
    },
  });

  const handleViewReceipt = async () => {
    if (!paidReceiptNumber) return;
    try {
      const data = await paymentsApi.getReceipt(paidReceiptNumber);
      setSelectedStudent(data.student);
      setReceipt(data.receipt);
      setDues(data.dues);
      setLateFee(data.lateFee);
      setConcession(data.concession);
      setIsAlreadyPaidOpen(false);
      setIsReceiptModalOpen(true);
    } catch (error) {
      console.error("Failed to load receipt", error);
      alert("Failed to load receipt details.");
    }
  };

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
  };

  const selectStudent = (student: StudentSummaryDto) => {
    setSelectedStudent(student);
    setSearchQuery('');
    setDues(null);
  };

  const handleCalculateDues = () => {
    if (selectedStudent) {
      duesMutation.mutate();
    }
  };

  const handleConfirmCollect = () => {
    if (selectedStudent && dues) {
      collectMutation.mutate({
        studentId: selectedStudent.id,
        month,
        year,
        lateFee,
        concessionAmount: concession,
        paymentMethod,
      });
      setIsConfirmOpen(false);
    }
  };

  const subtotal = dues?.lineItems?.reduce((acc, item) => acc + item.amount, 0) || 0;
  const grandTotal = subtotal + lateFee - concession;
  
  const showConcession = (selectedStudent?.concessionAmount || 0) > 0 || (dues?.expectedConcession || 0) > 0;

  return (
    <div className="p-8 max-w-7xl mx-auto animate-fade-in">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-800 tracking-tight">Fee Collection</h1>
        <p className="text-slate-500 mt-1">Search student, calculate dues, and process payments.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left Column: Student & Period */}
        <div className="lg:col-span-7 space-y-8">
          
          {/* Panel 1: Student Search */}
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="p-6 border-b border-slate-100 bg-slate-50/50">
              <h2 className="text-lg font-semibold text-slate-800 flex items-center gap-2">
                <User className="w-5 h-5 text-indigo-500" />
                1. Select Student
              </h2>
            </div>
            <div className="p-6">
              <div className="relative mb-6">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search className="h-5 w-5 text-slate-400" />
                </div>
                <input
                  type="text"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-300 rounded-xl leading-5 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm transition-all"
                  placeholder="Search by name, admission number..."
                  value={searchQuery}
                  onChange={handleSearch}
                />
                {isSearching && (
                  <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                    <Loader2 className="h-5 w-5 text-slate-400 animate-spin" />
                  </div>
                )}
              </div>

              {/* Search Results Dropdown-like list */}
              {searchQuery.length > 2 && studentsData?.content && studentsData.content.length > 0 && !selectedStudent && (
                <div className="bg-white border border-slate-200 rounded-xl shadow-lg max-h-60 overflow-y-auto mb-6">
                  {studentsData.content.map(student => (
                    <button
                      key={student.id}
                      disabled={student.status !== 'ACTIVE'}
                      className={`w-full text-left px-4 py-3 border-b border-slate-100 last:border-0 flex justify-between items-center transition-colors ${
                        student.status === 'ACTIVE' 
                          ? 'hover:bg-slate-50 cursor-pointer' 
                          : 'opacity-60 cursor-not-allowed bg-slate-50'
                      }`}
                      onClick={() => selectStudent(student)}
                    >
                      <div>
                        <div className="font-medium text-slate-800 flex items-center gap-2">
                          {student.fullName}
                          {student.status !== 'ACTIVE' && (
                            <span className="text-[10px] bg-red-100 text-red-700 px-1.5 py-0.5 rounded font-bold uppercase tracking-wider">
                              {student.status}
                            </span>
                          )}
                        </div>
                        <div className="text-sm text-slate-500">Adm: {student.admissionNumber} • {student.className}{student.sectionName ? ` - ${student.sectionName}` : ''}</div>
                      </div>
                      {student.status === 'ACTIVE' && <CheckCircle2 className="w-5 h-5 text-slate-300" />}
                    </button>
                  ))}
                </div>
              )}

              {/* Selected Student Profile */}
              {selectedStudent && (
                <div className="bg-gradient-to-br from-indigo-50 to-blue-50 rounded-xl p-5 border border-indigo-100 flex items-start justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-14 h-14 bg-indigo-600 rounded-full flex items-center justify-center text-white text-xl font-bold shadow-sm uppercase">
                      {selectedStudent.firstName[0]}
                    </div>
                    <div>
                      <h3 className="text-xl font-bold text-slate-800">{selectedStudent.fullName}</h3>
                      <p className="text-slate-600 font-medium">{selectedStudent.className}{selectedStudent.sectionName ? ` • Section ${selectedStudent.sectionName}` : ''}</p>
                      <p className="text-sm text-slate-500 mt-1">Adm: {selectedStudent.admissionNumber} | Father: {selectedStudent.fatherName}</p>
                    </div>
                  </div>
                  <button 
                    onClick={() => { setSelectedStudent(null); setDues(null); }}
                    className="text-sm text-indigo-600 hover:text-indigo-800 font-medium px-3 py-1 bg-white rounded-lg border border-indigo-200 shadow-sm transition-colors"
                  >
                    Change
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Panel 2: Period */}
          <div className={`bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden transition-opacity ${!selectedStudent ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
            <div className="p-6 border-b border-slate-100 bg-slate-50/50">
              <h2 className="text-lg font-semibold text-slate-800 flex items-center gap-2">
                <Calendar className="w-5 h-5 text-indigo-500" />
                2. Select Fee Period
              </h2>
            </div>
            <div className="p-6 flex items-end gap-6">
              <div className="flex-1">
                <label className="block text-sm font-medium text-slate-700 mb-2">Month</label>
                <select 
                  className="block w-full pl-3 pr-10 py-3 text-base border-slate-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-xl border bg-white"
                  value={month}
                  onChange={(e) => setMonth(Number(e.target.value))}
                >
                  {Array.from({ length: 12 }, (_, i) => i + 1).map(m => {
                    const isDisabled = year < startYear || (year === startYear && m < startMonth);
                    return (
                      <option key={m} value={m} disabled={isDisabled}>
                        {new Date(2000, m - 1).toLocaleString('default', { month: 'long' })}
                        {isDisabled ? ' (Prior to Start Date)' : ''}
                      </option>
                    );
                  })}
                </select>
              </div>
              <div className="flex-1">
                <label className="block text-sm font-medium text-slate-700 mb-2">Year</label>
                <select 
                  className="block w-full pl-3 pr-10 py-3 text-base border-slate-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-xl border bg-white"
                  value={year}
                  onChange={(e) => {
                    const newYear = Number(e.target.value);
                    setYear(newYear);
                    if (newYear === startYear && month < startMonth) {
                      setMonth(startMonth);
                    }
                  }}
                >
                  {[new Date().getFullYear() - 1, new Date().getFullYear(), new Date().getFullYear() + 1].map(y => (
                    <option key={y} value={y} disabled={y < startYear}>{y}</option>
                  ))}
                </select>
              </div>
              <button
                onClick={handleCalculateDues}
                disabled={duesMutation.isPending}
                className="px-6 py-3 bg-slate-800 text-white font-medium rounded-xl hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 shadow-sm transition-all disabled:opacity-70 flex items-center gap-2"
              >
                {duesMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Calculate Dues
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: Fee Cart */}
        <div className="lg:col-span-5">
          <div className={`bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden sticky top-8 transition-opacity ${!dues ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
            <div className="p-6 bg-gradient-to-r from-slate-800 to-slate-900">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <CreditCard className="w-6 h-6 text-indigo-400" />
                3. Payment Summary
              </h2>
            </div>
            
            <div className="p-6">
              {/* Line Items */}
              <div className="space-y-4 mb-6">
                <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider">Fee Heads</h3>
                {dues?.lineItems?.length === 0 ? (
                  <div className="py-4 text-center text-sm text-slate-500">No fee heads configured</div>
                ) : (
                  dues?.lineItems?.map((item, idx) => (
                    <div key={idx} className="flex justify-between items-center py-2 text-sm">
                      <span className="text-slate-600 font-medium">{item.feeCategoryName}</span>
                      <span className="text-slate-800 font-semibold font-mono">₹{item.amount.toFixed(2)}</span>
                    </div>
                  ))
                )}
                
                <div className="border-t border-slate-200 pt-4 mt-4 flex justify-between items-center text-slate-800 font-semibold">
                  <span>Subtotal</span>
                  <span>₹{subtotal.toFixed(2)}</span>
                </div>
              </div>

              {/* Adjustments */}
              <div className="space-y-4 mb-8 bg-slate-50 p-4 rounded-xl border border-slate-100">
                <div className="flex items-center justify-between">
                  <label className="text-sm font-medium text-slate-700">Late Fee (+)</label>
                  <div className="relative w-32">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <span className="text-slate-500 sm:text-sm">₹</span>
                    </div>
                    <input
                      type="number"
                      min="0"
                      className="block w-full pl-7 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm bg-white"
                      value={lateFee}
                      onChange={(e) => setLateFee(Math.max(0, Number(e.target.value) || 0))}
                    />
                  </div>
                </div>

                {showConcession && (
                  <div className="flex items-center justify-between">
                    <label className="text-sm font-medium text-slate-700 text-emerald-600">Concession (-)</label>
                    <div className="relative w-32">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <span className="text-emerald-500 sm:text-sm">₹</span>
                      </div>
                      <input
                        type="number"
                        min="0"
                        className="block w-full pl-7 pr-3 py-2 border border-emerald-300 rounded-lg focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm bg-emerald-50/50 text-emerald-700 font-medium"
                        value={concession}
                        onChange={(e) => setConcession(Math.max(0, Number(e.target.value) || 0))}
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Grand Total */}
              <div className="flex justify-between items-end mb-8 bg-indigo-50 p-5 rounded-xl border border-indigo-100">
                <div>
                  <p className="text-sm font-medium text-indigo-600 mb-1">Total Payable</p>
                  <p className="text-4xl font-bold text-indigo-900">₹{grandTotal.toFixed(2)}</p>
                </div>
              </div>

              {/* Payment Method */}
              <div className="mb-8">
                <label className="block text-sm font-medium text-slate-700 mb-3">Payment Method</label>
                <div className="grid grid-cols-2 gap-3">
                  {['CASH', 'UPI', 'CHEQUE', 'BANK_TRANSFER'].map((method) => (
                    <button
                      key={method}
                      onClick={() => setPaymentMethod(method as any)}
                      className={`py-3 px-4 rounded-xl border text-sm font-medium transition-all ${
                        paymentMethod === method 
                          ? 'bg-indigo-600 border-indigo-600 text-white shadow-md' 
                          : 'bg-white border-slate-200 text-slate-600 hover:border-indigo-300 hover:bg-slate-50'
                      }`}
                    >
                      {method.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>

              {/* Collect Button */}
              <button
                onClick={() => setIsConfirmOpen(true)}
                disabled={collectMutation.isPending || grandTotal < 0 || !dues}
                className="w-full py-4 px-6 border border-transparent rounded-xl shadow-sm text-lg font-bold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all disabled:opacity-70 flex items-center justify-center gap-2"
              >
                Collect Payment
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Modal */}
      {isConfirmOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" onClick={() => setIsConfirmOpen(false)}></div>
            <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
            <div className="inline-block align-bottom bg-white rounded-xl text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
              <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="sm:flex sm:items-start">
                  <div className="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
                    <h3 className="text-xl leading-6 font-bold text-gray-900" id="modal-title">
                      Confirm Payment
                    </h3>
                    <div className="mt-4 bg-slate-50 p-4 rounded-lg border border-slate-200">
                      <p className="text-sm text-gray-500 mb-2">Are you sure payment is received for the following details?</p>
                      <div className="grid grid-cols-2 gap-4 text-sm mt-3">
                        <div className="text-slate-500">Student:</div>
                        <div className="font-semibold text-slate-800 text-right">{selectedStudent?.firstName} {selectedStudent?.lastName}</div>
                        
                        <div className="text-slate-500">Period:</div>
                        <div className="font-semibold text-slate-800 text-right">{month}/{year}</div>
                        
                        <div className="text-slate-500">Method:</div>
                        <div className="font-semibold text-slate-800 text-right">{paymentMethod.replace('_', ' ')}</div>
                      </div>
                      <div className="mt-4 pt-4 border-t border-slate-200 flex justify-between items-center">
                        <div className="text-indigo-600 font-bold">Grand Total:</div>
                        <div className="text-2xl font-bold text-indigo-700">₹{grandTotal.toFixed(2)}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse gap-2">
                <button
                  type="button"
                  onClick={handleConfirmCollect}
                  disabled={collectMutation.isPending}
                  className="w-full inline-flex justify-center rounded-lg border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:w-auto sm:text-sm items-center gap-2"
                >
                  {collectMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Yes, Proceed
                </button>
                <button
                  type="button"
                  onClick={() => setIsConfirmOpen(false)}
                  className="mt-3 w-full inline-flex justify-center rounded-lg border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:w-auto sm:text-sm"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Already Paid Modal */}
      {isAlreadyPaidOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" onClick={() => setIsAlreadyPaidOpen(false)}></div>
            <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
            <div className="inline-block align-bottom bg-white rounded-xl text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
              <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="sm:flex sm:items-start">
                  <div className="mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100 sm:mx-0 sm:h-10 sm:w-10">
                    <CheckCircle2 className="h-6 w-6 text-emerald-600" />
                  </div>
                  <div className="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
                    <h3 className="text-xl leading-6 font-bold text-gray-900" id="modal-title">
                      Payment Already Collected
                    </h3>
                    <div className="mt-4 bg-emerald-50 p-4 rounded-lg border border-emerald-100 text-emerald-800 text-sm">
                      <p>The fee for {new Date(2000, month - 1).toLocaleString('default', { month: 'long' })} {year} has already been paid for {selectedStudent?.firstName}.</p>
                      {paidReceiptNumber && (
                        <p className="mt-2 font-semibold flex items-center gap-2">
                          Receipt Number: 
                          <button 
                            onClick={handleViewReceipt}
                            className="font-mono bg-emerald-100 text-emerald-800 px-2 py-1 rounded hover:bg-emerald-200 hover:text-emerald-900 transition-colors underline decoration-emerald-400 underline-offset-2"
                          >
                            {paidReceiptNumber}
                          </button>
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              </div>
              <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse gap-2">
                <button
                  type="button"
                  onClick={() => setIsAlreadyPaidOpen(false)}
                  className="w-full inline-flex justify-center rounded-lg border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 sm:w-auto sm:text-sm items-center gap-2"
                >
                  Okay
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <ReceiptModal 
        isOpen={isReceiptModalOpen} 
        onClose={() => setIsReceiptModalOpen(false)} 
        receipt={receipt} 
        student={selectedStudent}
        dues={dues}
        lateFee={lateFee}
        concession={concession}
        grandTotal={grandTotal}
      />
    </div>
  );
};
