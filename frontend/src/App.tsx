import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ProtectedRoute } from '@/router/ProtectedRoute'
import { AppLayout } from '@/components/layout/AppLayout'
import { LandingPage } from '@/pages/Landing/LandingPage'
import { LoginPage } from '@/pages/Login/LoginPage'
import { SetPasswordPage } from '@/pages/Login/SetPasswordPage'
import { ForgotPasswordPage } from '@/pages/Login/ForgotPasswordPage'
import { RegisterPage } from '@/pages/Register/RegisterPage'
import { DashboardPage } from '@/pages/Dashboard/DashboardPage'
import { StudentsPage } from '@/pages/Students/StudentsPage'
import { StudentDetailPage } from '@/pages/Students/StudentDetailPage'
import { FeeStructurePage } from '@/pages/FeeStructure/FeeStructurePage'
import { FeeCollectionPage } from '@/pages/FeeCollection/FeeCollectionPage'
import { PaymentsPage } from '@/pages/Payments/PaymentsPage'
import { SettingsPage } from '@/pages/Settings/SettingsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 1000 * 30,
      refetchOnWindowFocus: false,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />

          {/* Protected */}
          <Route element={<ProtectedRoute />}>
            <Route path="/set-password" element={<SetPasswordPage />} />
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/students" element={<StudentsPage />} />
              <Route path="/students/:id" element={<StudentDetailPage />} />
              <Route path="/fee-structure" element={<FeeStructurePage />} />
              <Route path="/collect-fee" element={<FeeCollectionPage />} />
              <Route path="/payments" element={<PaymentsPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

function PlaceholderPage({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="p-8 animate-fade-in">
      <div className="card p-16 text-center">
        <h1 className="text-2xl font-bold text-slate-800 mb-2">{title}</h1>
        <p className="text-slate-500">{desc}</p>
      </div>
    </div>
  )
}

export default App
