import { create } from 'zustand'

interface UserInfo {
  id: string
  username: string
  fullName: string
  role: string
  schoolId: string
  schoolName: string
  mustChangePassword: boolean
}

interface AuthState {
  accessToken: string | null
  user: UserInfo | null
  isAuthenticated: boolean
  setAuth: (token: string, user: UserInfo) => void
  setAccessToken: (token: string) => void
  clearMustChangePassword: () => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,

  setAuth: (token, user) =>
    set({ accessToken: token, user, isAuthenticated: true }),

  setAccessToken: (token) =>
    set({ accessToken: token }),

  clearMustChangePassword: () =>
    set((s) => ({ user: s.user ? { ...s.user, mustChangePassword: false } : null })),

  logout: () =>
    set({ accessToken: null, user: null, isAuthenticated: false }),
}))
