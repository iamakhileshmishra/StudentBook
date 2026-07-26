import { NavLink, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  Users,
  LayoutGrid,
  IndianRupee,
  Receipt,
  Settings,
  LogOut,
  GraduationCap,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import api from '@/api/axios'

const NAV_ITEMS = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/students',  icon: Users,           label: 'Students'  },
  { to: '/fee-structure', icon: LayoutGrid,  label: 'Fee Structure' },
  { to: '/collect-fee', icon: IndianRupee,   label: 'Collect Fee' },
  { to: '/payments',  icon: Receipt,         label: 'Payments'  },
  { to: '/settings',  icon: Settings,        label: 'Settings'  },
]

export function Sidebar() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try { await api.post('/auth/logout') } catch { /* ignore */ }
    logout()
    navigate('/login')
  }

  return (
    <aside className="sidebar">
      {/* Logo / School Name */}
      <div className="px-4 py-5 border-b border-slate-700/50">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-brand-600 flex items-center justify-center flex-shrink-0">
            <GraduationCap className="w-5 h-5 text-white" />
          </div>
          <div className="min-w-0">
            <p className="text-white font-semibold text-sm leading-tight truncate">
              {user?.schoolName ?? 'School ERP'}
            </p>
            <p className="text-slate-400 text-xs mt-0.5">Fee Management</p>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-4 overflow-y-auto">
        <p className="px-6 text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">
          Main Menu
        </p>
        <ul className="space-y-0.5">
          {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
            <li key={to}>
              <NavLink
                to={to}
                className={({ isActive }) =>
                  `sidebar-nav-item ${isActive ? 'active' : ''}`
                }
              >
                <Icon className="w-4 h-4 flex-shrink-0" />
                <span>{label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {/* User info + Logout */}
      <div className="border-t border-slate-700/50 p-3">
        <div className="flex items-center gap-3 px-2 py-2 rounded-lg">
          <div className="w-8 h-8 rounded-full bg-brand-600 flex items-center justify-center flex-shrink-0">
            <span className="text-white text-xs font-bold">
              {user?.fullName?.charAt(0).toUpperCase() ?? 'A'}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-white text-sm font-medium truncate">{user?.fullName}</p>
            <p className="text-slate-400 text-xs capitalize">
              {user?.role?.replace('_', ' ').toLowerCase()}
            </p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="sidebar-nav-item w-full mt-1 text-red-400 hover:text-red-300 hover:bg-red-500/10"
        >
          <LogOut className="w-4 h-4" />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  )
}
