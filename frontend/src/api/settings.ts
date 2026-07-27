import api from './axios'

export interface LateFeeRule {
  startDaysLate: number
  endDaysLate: number
  amount: number
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
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export const settingsApi = {
  getSchoolProfile: () => 
    api.get<SchoolProfileDto>('/settings/school').then(res => res.data),
    
  updateSchoolProfile: (data: SchoolProfileDto) =>
    api.put<SchoolProfileDto>('/settings/school', data).then(res => res.data),

  changePassword: (data: ChangePasswordRequest) =>
    api.post('/settings/change-password', data).then(res => res.data),
}
