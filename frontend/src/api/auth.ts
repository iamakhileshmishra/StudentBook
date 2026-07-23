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
  }
}

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<LoginResponse>('/auth/login', data),

  logout: () =>
    api.post('/auth/logout'),

  refresh: () =>
    api.post<{ accessToken: string }>('/auth/refresh'),
}
