import api from './axios'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  user: {
    id: string
    username: string
    fullName: string
    role: string
    schoolId: string
    schoolName: string
    mustChangePassword: boolean
  }
}

export interface RegisterSchoolRequest {
  ownerName: string
  schoolName: string
  shortName?: string
  email: string
  phone: string
  addressLine1: string
  city: string
  state: string
  pincode: string
  feeDueDate?: number
}

export interface RegisterSchoolResponse {
  schoolName: string
  email: string
}

export const authApi = {
  register: (data: RegisterSchoolRequest) =>
    api.post<RegisterSchoolResponse>('/auth/register', data).then((r) => r.data),

  login: (data: LoginRequest) =>
    api.post<LoginResponse>('/auth/login', data),

  logout: () =>
    api.post('/auth/logout'),

  refresh: () =>
    api.post<{ accessToken: string }>('/auth/refresh'),
}
