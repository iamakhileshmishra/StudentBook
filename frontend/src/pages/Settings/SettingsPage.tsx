import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Settings, Shield, Database, School, Save } from 'lucide-react'
import { settingsApi, SchoolProfileDto, ChangePasswordRequest } from '@/api/settings'

export function SettingsPage() {
  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'system'>('profile')

  return (
    <div className="p-8 max-w-5xl mx-auto animate-fade-in">
      <div className="flex items-center gap-4 mb-8">
        <div className="w-12 h-12 bg-slate-100  rounded-2xl flex items-center justify-center text-slate-600 ">
          <Settings className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-900 ">Settings</h1>
          <p className="text-slate-500 ">Manage school profile, security, and preferences</p>
        </div>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        {/* Sidebar Tabs */}
        <div className="w-full md:w-64 shrink-0 space-y-1">
          <TabButton 
            active={activeTab === 'profile'} 
            onClick={() => setActiveTab('profile')} 
            icon={<School className="w-4 h-4" />} 
            label="School Profile" 
          />
          <TabButton 
            active={activeTab === 'security'} 
            onClick={() => setActiveTab('security')} 
            icon={<Shield className="w-4 h-4" />} 
            label="Security" 
          />
          <TabButton 
            active={activeTab === 'system'} 
            onClick={() => setActiveTab('system')} 
            icon={<Database className="w-4 h-4" />} 
            label="System & Backup" 
          />
        </div>

        {/* Content Area */}
        <div className="flex-1 min-w-0">
          {activeTab === 'profile' && <ProfileSettings />}
          {activeTab === 'security' && <SecuritySettings />}
          {activeTab === 'system' && <SystemSettings />}
        </div>
      </div>
    </div>
  )
}

function TabButton({ active, onClick, icon, label }: { active: boolean, onClick: () => void, icon: React.ReactNode, label: string }) {
  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
        active 
          ? 'bg-brand-50 text-brand-700' 
          : 'text-slate-700 hover:bg-slate-50'
      }`}
    >
      {icon}
      {label}
    </button>
  )
}

function ProfileSettings() {
  const queryClient = useQueryClient()
  const { data: profile, isLoading } = useQuery({
    queryKey: ['school-profile'],
    queryFn: settingsApi.getSchoolProfile
  })

  const [formData, setFormData] = useState<SchoolProfileDto | null>(null)

  // Initialize form data when fetched
  if (profile && !formData) {
    setFormData(profile)
  }

  const mutation = useMutation({
    mutationFn: settingsApi.updateSchoolProfile,
    onSuccess: (data) => {
      queryClient.setQueryData(['school-profile'], data)
      alert('Profile updated successfully!')
    },
    onError: () => {
      alert('Failed to update profile')
    }
  })

  if (isLoading || !formData) return <div className="text-slate-500">Loading profile...</div>

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
  }

  return (
    <div className="bg-white  rounded-2xl shadow-sm border border-slate-200  p-6">
      <h2 className="text-lg font-bold text-slate-800  mb-6">School Profile</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-1.5 md:col-span-2">
          <label className="text-sm font-medium text-slate-700 ">School Name</label>
          <input 
            type="text" name="name" value={formData.name} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-700 ">Short Name (Receipt Prefix)</label>
          <input 
            type="text" name="shortName" value={formData.shortName || ''} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-700 ">Phone Number</label>
          <input 
            type="text" name="phone" value={formData.phone || ''} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
        <div className="space-y-1.5 md:col-span-2">
          <label className="text-sm font-medium text-slate-700 ">Address Line 1</label>
          <input 
            type="text" name="addressLine1" value={formData.addressLine1 || ''} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-700 ">City</label>
          <input 
            type="text" name="city" value={formData.city || ''} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-700 ">State</label>
          <input 
            type="text" name="state" value={formData.state || ''} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-700 ">Pincode</label>
          <input 
            type="text" name="pincode" value={formData.pincode || ''} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-700 ">Email Address</label>
          <input 
            type="email" name="email" value={formData.email || ''} onChange={handleChange}
            className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
          />
        </div>
      </div>

      <div className="mt-8 pt-8 border-t border-slate-200">
        <h3 className="text-lg font-bold text-slate-800 mb-4">Fee Settings</h3>
        <div className="space-y-6">
          <div className="space-y-1.5 max-w-sm">
            <label className="text-sm font-medium text-slate-700">Monthly Fee Due Date (e.g. 10 for the 10th)</label>
            <input 
              type="number" min="1" max="28" name="feeDueDate" value={formData.feeDueDate || 10} 
              onChange={(e) => setFormData({ ...formData, feeDueDate: parseInt(e.target.value) || 10 })}
              className="w-full px-3 py-2 bg-white border border-slate-300 rounded-lg focus:outline-none focus:border-brand-500"
            />
          </div>
          
          <div>
            <label className="text-sm font-medium text-slate-700 block mb-2">Late Fee Rules</label>
            <div className="space-y-3">
              {(formData.lateFeeRules || []).map((rule, idx) => (
                <div key={idx} className="flex items-center gap-3 bg-slate-50 p-3 rounded-lg border border-slate-200">
                  <span className="text-sm text-slate-600">Days</span>
                  <input type="number" min="1" max="31"
                    className={`w-20 px-2 py-1 border border-slate-300 rounded text-sm ${idx > 0 ? 'bg-slate-100 text-slate-500' : 'bg-white'}`}
                    value={rule.startDaysLate || ''} 
                    readOnly={idx > 0}
                    onChange={e => {
                      if (idx > 0) return;
                      const rules = [...(formData.lateFeeRules || [])];
                      let val = parseInt(e.target.value);
                      if (isNaN(val)) val = 0;
                      if (val > 31) val = 31;
                      rules[idx].startDaysLate = val;
                      for (let i = idx + 1; i < rules.length; i++) {
                        rules[i].startDaysLate = rules[i - 1].endDaysLate + 1;
                        if (rules[i].endDaysLate < rules[i].startDaysLate) {
                          rules[i].endDaysLate = rules[i].startDaysLate;
                        }
                      }
                      setFormData({ ...formData, lateFeeRules: rules });
                    }}
                    onBlur={() => {
                      if (idx > 0) return;
                      const rules = [...(formData.lateFeeRules || [])];
                      if (rules[idx].startDaysLate < 1) rules[idx].startDaysLate = 1;
                      if (rules[idx].endDaysLate < rules[idx].startDaysLate) {
                        rules[idx].endDaysLate = rules[idx].startDaysLate;
                      }
                      for (let i = idx + 1; i < rules.length; i++) {
                        rules[i].startDaysLate = rules[i - 1].endDaysLate + 1;
                        if (rules[i].endDaysLate < rules[i].startDaysLate) {
                          rules[i].endDaysLate = rules[i].startDaysLate;
                        }
                      }
                      setFormData({ ...formData, lateFeeRules: rules });
                    }} 
                  />
                  <span className="text-sm text-slate-600">to</span>
                  <input type="number" min={rule.startDaysLate} max="31" 
                    className="w-20 px-2 py-1 border border-slate-300 rounded text-sm bg-white" 
                    value={rule.endDaysLate || ''} 
                    onChange={e => {
                      const rules = [...(formData.lateFeeRules || [])];
                      let val = parseInt(e.target.value);
                      if (isNaN(val)) val = 0;
                      if (val > 31) val = 31;
                      rules[idx].endDaysLate = val;
                      for (let i = idx + 1; i < rules.length; i++) {
                        rules[i].startDaysLate = rules[i - 1].endDaysLate + 1;
                        if (rules[i].endDaysLate < rules[i].startDaysLate) {
                          rules[i].endDaysLate = rules[i].startDaysLate;
                        }
                      }
                      setFormData({ ...formData, lateFeeRules: rules });
                    }}
                    onBlur={() => {
                      const rules = [...(formData.lateFeeRules || [])];
                      if (rules[idx].endDaysLate < rules[idx].startDaysLate) {
                        rules[idx].endDaysLate = rules[idx].startDaysLate;
                      }
                      for (let i = idx + 1; i < rules.length; i++) {
                        rules[i].startDaysLate = rules[i - 1].endDaysLate + 1;
                        if (rules[i].endDaysLate < rules[i].startDaysLate) {
                          rules[i].endDaysLate = rules[i].startDaysLate;
                        }
                      }
                      setFormData({ ...formData, lateFeeRules: rules });
                    }} 
                  />
                  <span className="text-sm text-slate-600 ml-4">Fee Amount (₹)</span>
                  <input type="number" min="0" className="w-24 px-2 py-1 border border-slate-300 rounded text-sm bg-white" 
                    value={rule.amount} 
                    onChange={e => {
                      const rules = [...(formData.lateFeeRules || [])];
                      rules[idx].amount = Math.max(0, parseInt(e.target.value) || 0);
                      setFormData({ ...formData, lateFeeRules: rules });
                    }} 
                  />
                  <button 
                    onClick={() => {
                      const rules = [...(formData.lateFeeRules || [])];
                      rules.splice(idx, 1);
                      setFormData({ ...formData, lateFeeRules: rules });
                    }}
                    className="ml-auto text-red-500 hover:text-red-700 p-1 font-bold text-xl leading-none"
                    title="Remove Rule"
                  >
                    ×
                  </button>
                </div>
              ))}
              <button
                onClick={() => {
                  const currentRules = formData.lateFeeRules || [];
                  let nextStart = 1;
                  if (currentRules.length > 0) {
                    nextStart = currentRules[currentRules.length - 1].endDaysLate + 1;
                  }
                  if (nextStart > 31) nextStart = 31;
                  setFormData({ 
                    ...formData, 
                    lateFeeRules: [...currentRules, { startDaysLate: nextStart, endDaysLate: nextStart, amount: 50 }] 
                  });
                }}
                className="text-sm text-brand-600 hover:text-brand-700 font-medium"
              >
                + Add Late Fee Rule
              </button>
            </div>
            <p className="text-xs text-slate-500 mt-3">Late fees are calculated automatically during fee collection based on these rules. The highest tier will apply if days late exceeds all defined ranges.</p>
          </div>
        </div>
      </div>

      <div className="mt-8 flex justify-end">
        <button 
          onClick={() => mutation.mutate(formData)}
          disabled={mutation.isPending}
          className="flex items-center gap-2 px-5 py-2.5 bg-brand-600 hover:bg-brand-700 text-white font-medium rounded-lg transition-colors disabled:opacity-50"
        >
          <Save className="w-4 h-4" />
          {mutation.isPending ? 'Saving...' : 'Save Profile'}
        </button>
      </div>
    </div>
  )
}

function SecuritySettings() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const mutation = useMutation({
    mutationFn: settingsApi.changePassword,
    onSuccess: () => {
      alert('Password changed successfully!')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    },
    onError: () => {
      alert('Failed to change password. Please check your current password.')
    }
  })

  const handleUpdate = () => {
    if (newPassword !== confirmPassword) {
      alert("New passwords do not match!")
      return
    }
    mutation.mutate({ currentPassword, newPassword })
  }

  return (
    <div className="space-y-6">
      <div className="bg-white  rounded-2xl shadow-sm border border-slate-200  p-6">
        <h2 className="text-lg font-bold text-slate-800  mb-2">Change Password</h2>
        <p className="text-sm text-slate-500  mb-6">Ensure your account is using a long, random password to stay secure.</p>
        
        <div className="space-y-4 max-w-md">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-700 ">Current Password</label>
            <input 
              type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)}
              className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-700 ">New Password</label>
            <input 
              type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)}
              className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-700 ">Confirm New Password</label>
            <input 
              type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)}
              className="w-full px-3 py-2 bg-white  border border-slate-300  rounded-lg focus:outline-none focus:border-brand-500 "
            />
          </div>
          
          <button 
            onClick={handleUpdate}
            disabled={mutation.isPending || !currentPassword || !newPassword}
            className="mt-4 px-5 py-2.5 bg-brand-600 hover:bg-brand-700 text-white font-medium rounded-lg transition-colors disabled:opacity-50"
          >
            {mutation.isPending ? 'Updating...' : 'Update Password'}
          </button>
        </div>
      </div>

      <div className="bg-slate-50 rounded-2xl border border-slate-200 p-6">
        <h2 className="text-lg font-bold text-slate-800 mb-2">Password Recovery</h2>
        <p className="text-sm text-slate-600 mb-4">Set up an email address to receive password reset links if you forget your password.</p>
        <span className="inline-block px-3 py-1 bg-amber-100 text-amber-800 text-xs font-semibold rounded-full">
          Coming Soon
        </span>
      </div>
    </div>
  )
}


function SystemSettings() {
  return (
    <div className="space-y-6">
      <div className="bg-white  rounded-2xl shadow-sm border border-slate-200  p-6">
        <h2 className="text-lg font-bold text-slate-800  mb-2">Database Backup</h2>
        <p className="text-sm text-slate-500  mb-4">Request a manual backup of your school's data. Automated backups run nightly.</p>
        <button className="px-5 py-2.5 bg-slate-100  text-slate-700  hover:bg-slate-200 :bg-slate-700 font-medium rounded-lg transition-colors border border-slate-200 ">
          Request Manual Backup
        </button>
        <p className="text-xs text-amber-600  mt-3 font-medium">Note: Feature will be enabled upon production deployment.</p>
      </div>

      <div className="bg-white  rounded-2xl shadow-sm border border-slate-200  p-6">
        <h2 className="text-lg font-bold text-slate-800  mb-2">Academic Year Management</h2>
        <p className="text-sm text-slate-500  mb-4">Close the current academic year and promote students to the next class.</p>
        <button className="px-5 py-2.5 bg-red-50  text-red-600  font-medium rounded-lg transition-colors border border-red-200 ">
          Close Academic Year
        </button>
        <p className="text-xs text-slate-400 mt-3">Warning: This action requires SUPER_ADMIN privileges and is currently under development.</p>
      </div>
    </div>
  )
}
