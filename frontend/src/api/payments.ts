import api from './axios'

export interface FeeItemDto {
  feeCategoryId: string
  feeCategoryName: string
  amount: number
}

export interface DuesResponse {
  subtotal: number
  expectedLateFee: number
  expectedConcession: number
  lineItems: FeeItemDto[]
  alreadyPaid?: boolean
  paidTransactionId?: string
  paidReceiptNumber?: string
}

export interface CollectFeeRequest {
  studentId: string
  month: number
  year: number
  lateFee: number
  concessionAmount: number
  paymentMethod: 'CASH' | 'UPI' | 'CHEQUE' | 'BANK_TRANSFER'
  remarks?: string
}

export interface PaymentReceiptDto {
  id: string
  receiptNumber: string
  studentId: string
  collectionMonth: number
  collectionYear: number
  amountPaid: number
  paymentMethod: string
  paymentDate: string
}

export interface RecentTransactionDto {
  id: string
  receiptNumber: string
  studentName: string
  admissionNumber: string
  className: string
  amountPaid: number
  paymentMethod: string
  confirmedAt: string
}

export interface Page<T> {
  content: T[]
  totalPages: number
  totalElements: number
  size: number
  number: number
}

export interface FullReceiptDto {
  receipt: PaymentReceiptDto
  student: import('./students').StudentSummaryDto
  dues: DuesResponse
  lateFee: number
  concession: number
  grandTotal: number
}

export const paymentsApi = {
  getDues: (studentId: string, month: number, year: number) =>
    api.get<DuesResponse>(`/fees/collection/dues`, { params: { studentId, month, year } }).then(r => r.data),

  collectFee: (data: CollectFeeRequest) =>
    api.post<PaymentReceiptDto>('/fees/collection', data).then(r => r.data),

  getHistory: (page: number = 0, size: number = 50, search: string = '') =>
    api.get<Page<RecentTransactionDto>>(`/payments/history`, { params: { page, size, search } }).then(r => r.data),

  getReceipt: (receiptNumber: string) =>
    api.get<FullReceiptDto>(`/payments/receipt/${receiptNumber}`).then(r => r.data),
}
