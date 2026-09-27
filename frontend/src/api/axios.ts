import axios from 'axios'
import { useAuthStore } from '@/store/authStore'

const api = axios.create({
  baseURL: '/api',
  withCredentials: true, // Required for httpOnly refresh token cookie
  headers: {
    'Content-Type': 'application/json',
  },
})

/** Public auth endpoints: never send a (possibly expired) access token to these. */
const isAuthEndpoint = (url?: string) => !!url && url.startsWith('/auth/')

// ── Request interceptor: attach access token ──────────────────
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  // Sending an expired token to /auth/refresh used to make the refresh itself
  // fail with 401, logging everyone out after 15 minutes (test&bug.md BUG-01)
  if (token && !isAuthEndpoint(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor: silent refresh on 401 ───────────────
let isRefreshing = false
let failedQueue: Array<{ resolve: (value: unknown) => void; reject: (error: unknown) => void }> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((p) => {
    if (error) {
      p.reject(error)
    } else {
      p.resolve(token)
    }
  })
  failedQueue = []
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Temporary password not yet replaced: the API allows nothing else (SEC-10)
    if (error.response?.status === 403 && error.response?.data?.error === 'PASSWORD_CHANGE_REQUIRED') {
      if (window.location.pathname !== '/set-password') {
        window.location.href = '/set-password'
      }
      return Promise.reject(error)
    }

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      if (isAuthEndpoint(originalRequest.url)) {
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const response = await api.post<{ accessToken: string }>('/auth/refresh')
        const newToken = response.data.accessToken
        useAuthStore.getState().setAccessToken(newToken)
        processQueue(null, newToken)
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        useAuthStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default api
