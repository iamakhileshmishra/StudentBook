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

export const academicApi = {
  getClasses: () =>
    api.get<ClassDto[]>('/structure/classes').then((r) => r.data),

  getCurrentYear: () =>
    api.get<AcademicYearDto>('/structure/academic-years/current').then((r) => r.data),

  getAcademicYears: () =>
    api.get<AcademicYearDto[]>('/structure/academic-years').then((r) => r.data),
}
