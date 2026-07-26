import React from 'react';
import { PaymentReceiptDto, DuesResponse } from '../../../api/payments';
import { StudentSummaryDto } from '../../../api/students';
import { Printer, X, CheckCircle2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { settingsApi } from '@/api/settings';

interface ReceiptModalProps {
  receipt: PaymentReceiptDto | null;
  student: StudentSummaryDto | null;
  dues: DuesResponse | null;
  lateFee: number;
  concession: number;
  grandTotal: number;
  isOpen: boolean;
  onClose: () => void;
}

const numberToWordsHelper = (num: number): string => {
  const a = ['', 'One ', 'Two ', 'Three ', 'Four ', 'Five ', 'Six ', 'Seven ', 'Eight ', 'Nine ', 'Ten ', 'Eleven ', 'Twelve ', 'Thirteen ', 'Fourteen ', 'Fifteen ', 'Sixteen ', 'Seventeen ', 'Eighteen ', 'Nineteen '];
  const b = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];
  
  if (num === 0) return '';
  if (num < 0) return 'Minus ' + numberToWordsHelper(Math.abs(num));

  let str = '';
  if (Math.floor(num / 10000000) > 0) {
    str += numberToWordsHelper(Math.floor(num / 10000000)) + 'Crore ';
    num %= 10000000;
  }
  if (Math.floor(num / 100000) > 0) {
    str += numberToWordsHelper(Math.floor(num / 100000)) + 'Lakh ';
    num %= 100000;
  }
  if (Math.floor(num / 1000) > 0) {
    str += numberToWordsHelper(Math.floor(num / 1000)) + 'Thousand ';
    num %= 1000;
  }
  if (Math.floor(num / 100) > 0) {
    str += numberToWordsHelper(Math.floor(num / 100)) + 'Hundred ';
    num %= 100;
  }
  if (num > 0) {
    if (str !== '') str += 'and ';
    if (num < 20) str += a[num];
    else {
      str += b[Math.floor(num / 10)];
      if (num % 10 > 0) str += '-' + a[num % 10];
      else str += ' ';
    }
  }
  return str;
};

const numberToWords = (num: number): string => {
  if (num === 0) return 'Zero Only';
  return numberToWordsHelper(num).trim() + ' Only';
};

export const ReceiptModal: React.FC<ReceiptModalProps> = ({ 
  receipt, student, dues, lateFee, concession, grandTotal, isOpen, onClose 
}) => {
  const { data: profile } = useQuery({
    queryKey: ['school-profile'],
    queryFn: settingsApi.getSchoolProfile,
    enabled: isOpen
  });

  if (!isOpen || !receipt || !student || !dues) return null;

  const handlePrint = () => {
    const printElement = document.getElementById('printable-receipt');
    if (!printElement) return;

    // Create a hidden iframe, but give it physical size so Chrome renders its layout tree!
    const iframe = document.createElement('iframe');
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '1000px';
    iframe.style.height = '1000px';
    iframe.style.border = '0';
    iframe.style.opacity = '0';
    iframe.style.pointerEvents = 'none';
    iframe.style.zIndex = '-1000';
    document.body.appendChild(iframe);

    const pri = iframe.contentWindow;
    if (!pri) return;

    // Gather all stylesheets to retain Tailwind styling
    const styles = document.querySelectorAll('style, link[rel="stylesheet"]');
    const stylesHtml = Array.from(styles).map(s => s.outerHTML).join('');

    pri.document.open();
    pri.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Receipt</title>
          ${stylesHtml}
          <style>
            @page { size: A4 landscape; margin: 0; }
            body { 
              margin: 0; 
              padding: 0; 
              background: white; 
              -webkit-print-color-adjust: exact; 
              print-color-adjust: exact; 
            }
            .print-container {
              width: 297mm;
              height: 210mm;
              overflow: hidden;
            }
          </style>
        </head>
        <body>
          <div class="print-container">
            ${printElement.outerHTML}
          </div>
          <script>
            // Wait for styles to apply before triggering print
            setTimeout(() => {
              window.focus();
              window.print();
            }, 500);
          </script>
        </body>
      </html>
    `);
    pri.document.close();

    // Cleanup iframe after print dialog is handled
    setTimeout(() => {
      document.body.removeChild(iframe);
    }, 60000);
  };

  const receiptMonthName = new Date(receipt.collectionYear, receipt.collectionMonth - 1).toLocaleString('default', { month: 'long', year: 'numeric' });

  const ReceiptContent = ({ type }: { type: 'Parent' | 'Office' }) => (
    <div className={`relative h-full flex flex-col p-5 text-slate-800 ${type === 'Office' ? 'border-l border-dashed border-slate-300' : ''}`}>
      {type === 'Office' && (
        <div className="absolute inset-0 flex items-center justify-center opacity-[0.04] pointer-events-none transform -rotate-45 font-bold text-6xl whitespace-nowrap">
          OFFICE COPY
        </div>
      )}
      
      {/* Header and Type */}
      <div className="text-center mb-3 pb-3 border-b-2 border-slate-800">
        <div className="uppercase font-bold tracking-widest text-slate-500 text-[10px] mb-1">{type} COPY</div>
        <h1 className="text-xl font-bold uppercase tracking-wider">{profile?.name || 'School Name'}</h1>
        <p className="text-xs">{profile?.addressLine1 || ''}{profile?.city ? `, ${profile.city}` : ''}{profile?.state ? `, ${profile.state}` : ''}{profile?.pincode ? ` - ${profile.pincode}` : ''}</p>
        <p className="text-xs">Phone: {profile?.phone || ''} | Email: {profile?.email || ''}</p>
      </div>

      {/* Receipt Info - Left Aligned */}
      <div className="flex justify-between items-start mb-3 text-xs">
        <div className="space-y-1">
          <div><span className="font-semibold">Receipt No:</span> {receipt.receiptNumber}</div>
          <div><span className="font-semibold">Fee Period:</span> <span className="bg-indigo-50 px-1.5 py-0.5 rounded font-medium text-indigo-700">{receiptMonthName}</span></div>
        </div>
        <div className="text-right">
          <div><span className="font-semibold">Date:</span> {new Date(receipt.paymentDate).toLocaleDateString('en-IN')}</div>
        </div>
      </div>

      {/* Student Details */}
      <div className="bg-slate-50 border border-slate-200 rounded p-2 mb-3 text-xs flex flex-col gap-1">
        <div className="flex items-start">
          <span className="text-slate-500 w-24 shrink-0">Student Name:</span> 
          <span className="font-semibold text-slate-800">{student.firstName} {student.lastName}</span>
        </div>
        <div className="flex items-start">
          <span className="text-slate-500 w-24 shrink-0">Class & Section:</span> 
          <span className="font-semibold text-slate-800">{student.className} - {student.sectionName}</span>
        </div>
        <div className="flex items-start">
          <span className="text-slate-500 w-24 shrink-0">Admission No:</span> 
          <span className="font-semibold text-slate-800">{student.admissionNumber}</span>
        </div>
        <div className="flex items-start">
          <span className="text-slate-500 w-24 shrink-0">Father's Name:</span> 
          <span className="font-semibold text-slate-800">{student.fatherName}</span>
        </div>
      </div>

      {/* Fee Breakdown Table */}
      <div className="flex-grow-0">
        <table className="w-full text-xs border-collapse mb-2">
          <thead>
            <tr className="border-b-2 border-slate-800">
              <th className="text-left py-1.5">Fee Head</th>
              <th className="text-right py-1.5">Amount (₹)</th>
            </tr>
          </thead>
          <tbody>
            {dues.lineItems?.map((item, idx) => (
              <tr key={idx} className="border-b border-slate-200">
                <td className="py-1.5">{item.feeCategoryName}</td>
                <td className="text-right py-1.5">{item.amount.toFixed(2)}</td>
              </tr>
            ))}
            
            {lateFee > 0 && (
              <tr className="border-b border-slate-200">
                <td className="py-1.5">Late Fee</td>
                <td className="text-right py-1.5">{lateFee.toFixed(2)}</td>
              </tr>
            )}
            
            {concession > 0 && (
              <tr className="border-b border-slate-200 text-emerald-600">
                <td className="py-1.5">Concession (-)</td>
                <td className="text-right py-1.5">-{concession.toFixed(2)}</td>
              </tr>
            )}
          </tbody>
          <tfoot>
            <tr className="font-bold text-sm border-t-2 border-slate-800">
              <td className="py-1.5 text-right">Grand Total:</td>
              <td className="text-right py-1.5">₹{grandTotal.toFixed(2)}</td>
            </tr>
          </tfoot>
        </table>
        
        {/* Payment Summary */}
        <div className="pt-2">
          <div className="text-xs mb-1.5">
            <span className="font-semibold text-slate-600">Amount in Words:</span> Rupees {numberToWords(grandTotal)}
          </div>
          <div className="text-xs">
            <span className="font-semibold text-slate-600">Payment Method:</span> {receipt.paymentMethod.replace('_', ' ')}
          </div>
        </div>
      </div>

      {/* Empty Space for Signature/Stamps */}
      <div className="flex-grow"></div>

      {/* Footer Signature Details */}
      <div className="mt-auto pt-4 border-t border-dashed border-slate-200">
        <div className="flex justify-between items-end">
          <div className="text-[10px] text-slate-400 italic">
            * This is a computer-generated receipt.
          </div>
          <div className="text-center">
            <div className="w-32 border-b border-slate-800 mb-1"></div>
            <div className="text-xs font-semibold">Authorized Signatory</div>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm print:absolute print:inset-0 print:p-0 print:bg-white print:block">
      {/* On-Screen Modal Container */}
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-5xl max-h-[90vh] overflow-y-auto print:shadow-none print:max-w-none print:max-h-none print:bg-white print:overflow-visible flex flex-col">
        
        {/* Header - hide on print */}
        <div className="flex items-center justify-between p-4 border-b border-slate-100 print:hidden sticky top-0 bg-white z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
              <CheckCircle2 className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800">Payment Successful!</h2>
              <p className="text-sm text-slate-500">Receipt generated</p>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 px-4 py-2 text-sm font-bold text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 transition-colors"
            >
              <Printer className="w-4 h-4" />
              Print Receipt
            </button>
            <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-50 transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Print Layout Area */}
        <div className="bg-slate-100 p-8 print:p-0 print:bg-white flex justify-center overflow-auto print:overflow-visible print:block">
          {/* A4 Landscape Page Container (297mm x 210mm) */}
          <div 
            id="printable-receipt"
            className="bg-white shadow-lg print:shadow-none grid grid-cols-2" 
            style={{ width: '297mm', height: '210mm', overflow: 'hidden' }}
          >
            <ReceiptContent type="Parent" />
            <ReceiptContent type="Office" />
          </div>
        </div>
      </div>
    </div>
  );
};
