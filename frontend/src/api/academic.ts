import api from './axios'

export interface ClassDto {
  id: string
  name: string
  numericOrder: number
  sections: SectionDto[]
}

export interface SectionDto {
  id: string
  name: string
}

export interface AcademicYearDto {
  id: string
  name: string
  startDate: string
  endDate: string
  current: boolean
}

export interface CreateClassRequest {
  name: string
  numericOrder?: number
}

export interface CreateSectionRequest {
  name: string
}

export const academicApi = {
  getClasses: () =>
    api.get<ClassDto[]>('/structure/classes').then((r) => r.data),

  getCurrentYear: () =>
    api.get<AcademicYearDto>('/structure/academic-years/current').then((r) => r.data),

  getAcademicYears: () =>
    api.get<AcademicYearDto[]>('/structure/academic-years').then((r) => r.data),

  createClass: (data: CreateClassRequest) =>
    api.post<ClassDto>('/structure/classes', data).then((r) => r.data),

  updateClass: ({ id, data }: { id: string; data: CreateClassRequest }) =>
    api.put<ClassDto>(`/structure/classes/${id}`, data).then((r) => r.data),

  deleteClass: (id: string) =>
    api.delete(`/structure/classes/${id}`).then((r) => r.data),

  createSection: ({ classId, data }: { classId: string; data: CreateSectionRequest }) =>
    api.post<SectionDto>(`/structure/classes/${classId}/sections`, data).then((r) => r.data),

  updateSection: ({ id, data }: { id: string; data: CreateSectionRequest }) =>
    api.put<SectionDto>(`/structure/sections/${id}`, data).then((r) => r.data),

  deleteSection: (id: string) =>
    api.delete(`/structure/sections/${id}`).then((r) => r.data),
}
