import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ProtectedRoute } from '@/router/ProtectedRoute'
import { AppLayout } from '@/components/layout/AppLayout'
import { LoginPage } from '@/pages/Login/LoginPage'
import { DashboardPage } from '@/pages/Dashboard/DashboardPage'
import { StudentsPage } from '@/pages/Students/StudentsPage'
import { StudentDetailPage } from '@/pages/Students/StudentDetailPage'

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
          <Route path="/login" element={<LoginPage />} />

          {/* Protected */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/students" element={<StudentsPage />} />
              <Route path="/students/:id" element={<StudentDetailPage />} />
              <Route path="/collect-fee" element={<PlaceholderPage title="Collect Fee" desc="Fee collection module coming in the next milestone." />} />
              <Route path="/payments" element={<PlaceholderPage title="Payment History" desc="Payment records and receipts coming soon." />} />
              <Route path="/settings" element={<PlaceholderPage title="Settings" desc="School settings and configuration coming soon." />} />
            </Route>
          </Route>

          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
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
