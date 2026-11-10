import axios from 'axios'
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
  /** Key of the public receipt link parents get: /r/{shareToken} */
  shareToken?: string
  studentId: string
  collectionMonth: number
  collectionYear: number
  amountPaid: number
  paymentMethod: string
  paymentDate: string
  confirmedAt?: string
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

export interface NotificationLogDto {
  channel: 'EMAIL' | 'WHATSAPP'
  recipient?: string
  status: 'SENT' | 'FAILED' | 'SKIPPED'
  detail?: string
  createdAt: string
}

/** Receipt as shown to a parent through the public link (no login). */
export interface PublicReceiptDto {
  school: {
    name: string
    addressLine1?: string
    addressLine2?: string
    city?: string
    state?: string
    pincode?: string
    phone?: string
    email?: string
    logoUrl?: string
  }
  receiptNumber: string
  status: string
  collectionMonth: number
  collectionYear: number
  paymentDate: string
  paymentMethod: string
  student: {
    firstName: string
    lastName?: string
    admissionNumber?: string
    fatherName?: string
    className?: string
    sectionName?: string
  }
  lineItems: { name: string; amount: number }[]
  lateFee: number
  concession: number
  grandTotal: number
}

/** Public receipt link for parents, e.g. https://site/r/abc… */
export const parentReceiptUrl = (shareToken: string) => `${window.location.origin}/r/${shareToken}`

export const paymentsApi = {
  getDues: (studentId: string, month: number, year: number) =>
    api.get<DuesResponse>(`/fees/collection/dues`, { params: { studentId, month, year } }).then(r => r.data),

  collectFee: (data: CollectFeeRequest) =>
    api.post<PaymentReceiptDto>('/fees/collection', data).then(r => r.data),

  getHistory: (page: number = 0, size: number = 50, search: string = '') =>
    api.get<Page<RecentTransactionDto>>(`/payments/history`, { params: { page, size, search } }).then(r => r.data),

  getReceipt: (receiptNumber: string) =>
    api.get<FullReceiptDto>(`/payments/receipt/${receiptNumber}`).then(r => r.data),

  getNotifications: (receiptNumber: string) =>
    api.get<NotificationLogDto[]>(`/payments/receipt/${encodeURIComponent(receiptNumber)}/notifications`).then(r => r.data),

  resendEmail: (receiptNumber: string) =>
    api.post<NotificationLogDto>(`/payments/receipt/${encodeURIComponent(receiptNumber)}/notifications/email`).then(r => r.data),

  // Plain axios: parents aren't signed in, so skip the token/refresh interceptors
  getPublicReceipt: (token: string) =>
    axios.get<PublicReceiptDto>(`/api/public/receipts/${encodeURIComponent(token)}`).then(r => r.data),
}
