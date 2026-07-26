import api from './axios'

export interface FeeCategoryDto {
  id: string
  name: string
  displayOrder: number
  active: boolean
}

export interface FeeStructureRowDto {
  structureId: string
  feeCategoryId: string
  categoryName: string
  displayOrder: number
  defaultAmount: number
}

export interface ClassFeeStructureDto {
  classId: string
  className: string
  rows: FeeStructureRowDto[]
  totalMonthlyFee: number
}

export interface UpdateFeeAmountRequest {
  classId: string
  feeCategoryId: string
  amount: number
}

export const feesApi = {
  getCategories: () =>
    api.get<FeeCategoryDto[]>('/fee-structure/categories').then((r) => r.data),

  getAllStructures: () =>
    api.get<ClassFeeStructureDto[]>('/fee-structure').then((r) => r.data),

  getClassStructure: (classId: string) =>
    api.get<ClassFeeStructureDto>(`/fee-structure/class/${classId}`).then((r) => r.data),

  updateAmount: (data: UpdateFeeAmountRequest) =>
    api.put<FeeStructureRowDto>('/fee-structure/update', data).then((r) => r.data),
}
