import api from './axios'

export interface StudentSummaryDto {
  id: string
  admissionNumber: string
  firstName: string
  lastName: string
  fullName: string
  fatherName: string
  primaryContactPhone: string
  status: string
  staffChild: boolean
  concessionAmount: number
  className: string
  sectionName?: string | null
  classId: string
  enrollmentId: string
}

export interface StudentPageResponse {
  content: StudentSummaryDto[]
  totalElements: number
  activeCount: number
  totalPages: number
  currentPage: number
  pageSize: number
}

export interface StudentDetailDto {
  id: string
  admissionNumber: string
  firstName: string
  lastName: string
  fullName: string
  dateOfBirth?: string
  gender?: string
  fatherName?: string
  motherName?: string
  guardianName?: string
  primaryContactPhone: string
  alternatePhone?: string
  email?: string
  address?: string
  staffChild: boolean
  concessionAmount: number
  concessionNote?: string
  status: string
  enrollmentId?: string
  classId?: string
  className?: string
  sectionId?: string
  sectionName?: string
  academicYearName?: string
  enrollmentDate?: string
  previousDues?: number
  createdAt: string
}

export interface StudentCreateRequest {
  firstName: string
  lastName: string
  dateOfBirth?: string
  gender?: string
  fatherName?: string
  motherName?: string
  guardianName?: string
  primaryContactPhone: string
  alternatePhone?: string
  email?: string
  address?: string
  staffChild: boolean
  concessionAmount: number
  concessionNote?: string
  classId: string
  /** Omitted when the class has no sections */
  sectionId?: string
  enrollmentDate?: string
  admissionNumber?: string
}

export const studentsApi = {
  search: (params: { q?: string; classId?: string; page?: number; size?: number }) =>
    api.get<StudentPageResponse>('/students', { params }).then((r) => r.data),

  getById: (id: string) =>
    api.get<StudentDetailDto>(`/students/${id}`).then((r) => r.data),

  create: (data: StudentCreateRequest) =>
    api.post<StudentDetailDto>('/students', data).then(r => r.data),
    
  updateStudent: (id: string, data: any) =>
    api.put<StudentDetailDto>(`/students/${id}`, data).then(r => r.data),
    
  removeStudent: (id: string, password: string) =>
    api.post<void>(`/students/${id}/remove`, { password }).then(r => r.data),
}
