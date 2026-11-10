import type { LoginResponse } from './auth'
import api from './axios'

export interface LateFeeRule {
  startDaysLate: number
  endDaysLate: number
  amount: number
}

export interface AcademicYearDto {
  id: string
  name: string
  startDate: string
  endDate: string
  isCurrent: boolean
}

export type PromotionDecision = 'PROMOTED' | 'RETAINED' | 'LEFT' | 'GRADUATED';

export interface StudentPromotionDto {
  studentId: string
  decision: PromotionDecision
  nextClassId?: string
  nextSectionId?: string
}

export interface PromotionRequest {
  newAcademicYearName: string
  newStartDate: string
  newEndDate: string
  promotions: StudentPromotionDto[]
}

export interface SectionData {
  id: string
  name: string
}

export interface ClassData {
  id: string
  name: string
  numericOrder: number
  sections: SectionData[]
}

export interface PromotionDataResponse {
  currentStudents: any[] // StudentSummaryDto with pendingDues
  classes: ClassData[]
}

export interface SchoolProfileDto {
  name: string
  shortName?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  state?: string
  pincode?: string
  phone?: string
  email?: string
  website?: string
  logoUrl?: string
  feeDueDate?: number
  lateFeeRules?: LateFeeRule[]
  /** Email the parent a receipt link after each fee payment */
  notifyParentEmail?: boolean
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  /** Emailed code; not needed on first login (replacing a temporary password) */
  otp?: string
}

export interface PasswordChangeCodeResponse {
  sentTo: string
  resendAfterSeconds: number
}

export const settingsApi = {
  getSchoolProfile: () => 
    api.get<SchoolProfileDto>('/settings/school').then(res => res.data),
    
  updateSchoolProfile: (data: SchoolProfileDto) =>
    api.put<SchoolProfileDto>('/settings/school', data).then(res => res.data),

  requestPasswordChangeCode: (data: ChangePasswordRequest) =>
    api.post<PasswordChangeCodeResponse>('/settings/change-password/request-otp', data).then(res => res.data),

  /** Returns fresh tokens: other sessions are signed out, this one continues */
  changePassword: (data: ChangePasswordRequest) =>
    api.post<LoginResponse>('/settings/change-password', data).then(res => res.data),

  getCurrentAcademicYear: () =>
    api.get<AcademicYearDto>('/settings/academic-years/current').then(res => res.data),

  updateCurrentAcademicYear: (data: AcademicYearDto) =>
    api.put<AcademicYearDto>('/settings/academic-years/current', data).then(res => res.data),

  getPromotionData: () =>
    api.get<PromotionDataResponse>('/settings/academic-years/promotion-data').then(res => res.data),

  promoteStudents: (data: PromotionRequest) =>
    api.post('/settings/academic-years/promote', data).then(res => res.data),

  downloadBackup: async (academicYearId?: string) => {
    const response = await api.get('/backup/download', {
      params: academicYearId ? { academicYearId } : undefined,
      responseType: 'blob'
    });
    const disposition = response.headers['content-disposition'];
    let filename = 'Backup.zip';
    if (disposition && disposition.indexOf('attachment') !== -1) {
      const matches = /filename="([^"]*)"/.exec(disposition);
      if (matches != null && matches[1]) {
        filename = matches[1];
      }
    }
    return { blob: response.data as Blob, filename };
  },
}
