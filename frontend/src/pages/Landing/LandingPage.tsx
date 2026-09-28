import { useState, FormEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  GraduationCap,
  Users,
  IndianRupee,
  Receipt,
  LayoutGrid,
  LayoutDashboard,
  CalendarClock,
  ShieldCheck,
  Check,
  Mail,
  Phone,
  MapPin,
  Menu,
  X,
  ArrowRight,
  LogOut,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import api from '@/api/axios'
import { LogoutConfirmModal } from '@/components/layout/LogoutConfirmModal'

// TODO: replace with real contact details before going live
const CONTACT = {
  email: 'hello@example.com',
  phone: '+91 00000 00000',
  address: 'Your office address, City, State',
}

const NAV_LINKS = [
  { href: '#about', label: 'About' },
  { href: '#pricing', label: 'Pricing' },
  { href: '#contact', label: 'Contact' },
]

const FEATURES = [
  {
    icon: Users,
    title: 'Student Management',
    desc: 'Admissions, class and section assignment, and a complete profile for every student.',
  },
  {
    icon: LayoutGrid,
    title: 'Flexible Fee Structures',
    desc: 'Define fee categories and monthly amounts per class. Structures carry over to each new year.',
  },
  {
    icon: IndianRupee,
    title: 'Fast Fee Collection',
    desc: 'See outstanding dues at a glance, collect for multiple months at once, and apply late fees automatically.',
  },
  {
    icon: Receipt,
    title: 'Instant Receipts',
    desc: 'Every payment gets a numbered, printable receipt and a searchable payment history.',
  },
  {
    icon: LayoutDashboard,
    title: 'Live Dashboard',
    desc: 'Collections, pending dues and class-wise summaries, updated as payments come in.',
  },
  {
    icon: CalendarClock,
    title: 'Year-End Promotion',
    desc: 'Promote students to the next class in one step, with arrears carried forward.',
  },
]

// TODO: confirm plan names and prices before going live
const PLANS = [
  {
    name: 'Starter',
    price: '₹999',
    period: '/month',
    desc: 'For small schools getting started.',
    features: ['Up to 300 students', 'Fee collection & receipts', 'Dashboard & reports', 'Email support'],
    highlighted: false,
  },
  {
    name: 'Standard',
    price: '₹2,499',
    period: '/month',
    desc: 'For growing schools with more classes.',
    features: ['Up to 1,500 students', 'Everything in Starter', 'Late fee rules', 'Year-end promotion', 'Priority support'],
    highlighted: true,
  },
  {
    name: 'Enterprise',
    price: 'Custom',
    period: '',
    desc: 'For large schools and school groups.',
    features: ['Unlimited students', 'Everything in Standard', 'Multiple branches', 'Data migration help', 'Dedicated support'],
    highlighted: false,
  },
]

export function LandingPage() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const logout = useAuthStore((s) => s.logout)
  const [menuOpen, setMenuOpen] = useState(false)
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false)

  // Signed-in visitors (session restored from the refresh cookie) can sign out here too
  const handleLogout = async () => {
    try { await api.post('/auth/logout') } catch { /* ignore */ }
    logout()
    setShowLogoutConfirm(false)
    setMenuOpen(false)
  }

  const authCta = isAuthenticated
    ? { to: '/dashboard', label: 'Go to Dashboard' }
    : { to: '/login', label: 'Login' }

  return (
    <div className="min-h-screen bg-white text-slate-900 scroll-smooth">
      {/* ── Header ─────────────────────────────────────────────── */}
      <header className="sticky top-0 z-40 bg-white/90 backdrop-blur border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <a href="#top" className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-lg bg-brand-600 flex items-center justify-center">
              <GraduationCap className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-lg">School ERP</span>
          </a>

          <nav className="hidden md:flex items-center gap-8">
            {NAV_LINKS.map(({ href, label }) => (
              <a key={href} href={href} className="text-sm font-medium text-slate-600 hover:text-brand-600 transition-colors">
                {label}
              </a>
            ))}
            <div className="flex items-center gap-3">
              {isAuthenticated ? (
                <button onClick={() => setShowLogoutConfirm(true)} className="btn-secondary text-sm">
                  <LogOut className="w-4 h-4" /> Logout
                </button>
              ) : (
                <Link to="/register" className="btn-secondary text-sm">Create Account</Link>
              )}
              <Link to={authCta.to} className="btn-primary text-sm">
                {authCta.label}
              </Link>
            </div>
          </nav>

          <button
            className="md:hidden p-2 -mr-2 text-slate-600"
            onClick={() => setMenuOpen((o) => !o)}
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={menuOpen}
          >
            {menuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {menuOpen && (
          <nav className="md:hidden border-t border-slate-200 px-4 py-3 space-y-1 animate-fade-in">
            {NAV_LINKS.map(({ href, label }) => (
              <a
                key={href}
                href={href}
                onClick={() => setMenuOpen(false)}
                className="block px-3 py-2 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-100"
              >
                {label}
              </a>
            ))}
            {isAuthenticated ? (
              <button onClick={() => setShowLogoutConfirm(true)} className="btn-secondary w-full justify-center mt-2">
                <LogOut className="w-4 h-4" /> Logout
              </button>
            ) : (
              <Link to="/register" className="btn-secondary justify-center mt-2">Create Account</Link>
            )}
            <Link to={authCta.to} className="btn-primary justify-center mt-2">
              {authCta.label}
            </Link>
          </nav>
        )}
      </header>

      {showLogoutConfirm && (
        <LogoutConfirmModal onConfirm={handleLogout} onClose={() => setShowLogoutConfirm(false)} />
      )}

      <main id="top">
        {/* ── Hero ─────────────────────────────────────────────── */}
        <section className="relative overflow-hidden bg-gradient-to-br from-slate-900 via-brand-950 to-brand-900">
          <div className="absolute inset-0 opacity-10 pointer-events-none">
            <div className="absolute top-10 left-10 w-72 h-72 rounded-full bg-brand-400 blur-3xl" />
            <div className="absolute bottom-0 right-10 w-96 h-96 rounded-full bg-brand-600 blur-3xl" />
          </div>
          <div className="relative max-w-6xl mx-auto px-4 sm:px-6 py-20 sm:py-28 text-center">
            <h1 className="text-4xl sm:text-5xl font-bold text-white leading-tight">
              Simplify School
              <br />
              <span className="text-brand-400">Fee Management</span>
            </h1>
            <p className="mt-6 text-lg text-slate-300 max-w-2xl mx-auto leading-relaxed">
              A modern, reliable system for managing students, collecting fees, generating receipts,
              and tracking payments — built for real schools.
            </p>
            <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-3">
              {isAuthenticated ? (
                <Link to="/dashboard" className="btn-primary px-6 py-3 text-base">
                  Go to Dashboard <ArrowRight className="w-4 h-4" />
                </Link>
              ) : (
                <>
                  <Link to="/register" className="btn-primary px-6 py-3 text-base">
                    Create Your School Account <ArrowRight className="w-4 h-4" />
                  </Link>
                  <Link
                    to="/login"
                    className="px-6 py-3 rounded-lg text-base font-medium text-white border border-white/30 hover:bg-white/10 transition-colors"
                  >
                    Login
                  </Link>
                </>
              )}
            </div>
          </div>
        </section>

        {/* ── About ────────────────────────────────────────────── */}
        <section id="about" className="scroll-mt-16 py-20 sm:py-24 bg-slate-50">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="max-w-2xl mx-auto text-center mb-14">
              <p className="text-sm font-semibold text-brand-600 uppercase tracking-wider">About</p>
              <h2 className="mt-2 text-3xl font-bold">Everything your fee office needs</h2>
              <p className="mt-4 text-slate-600 leading-relaxed">
                School ERP replaces registers and spreadsheets with one simple system. Your staff
                can see who has paid, who hasn't, and print a receipt in seconds.
              </p>
            </div>

            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {FEATURES.map(({ icon: Icon, title, desc }) => (
                <div key={title} className="card p-6">
                  <div className="w-10 h-10 rounded-lg bg-brand-50 text-brand-600 flex items-center justify-center">
                    <Icon className="w-5 h-5" />
                  </div>
                  <h3 className="mt-4 font-semibold text-slate-900">{title}</h3>
                  <p className="mt-2 text-sm text-slate-600 leading-relaxed">{desc}</p>
                </div>
              ))}
            </div>

            <div className="mt-12 flex items-center justify-center gap-2 text-sm text-slate-500">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              Each school's data is kept separate and secure.
            </div>
          </div>
        </section>

        {/* ── Pricing ──────────────────────────────────────────── */}
        <section id="pricing" className="scroll-mt-16 py-20 sm:py-24">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="max-w-2xl mx-auto text-center mb-14">
              <p className="text-sm font-semibold text-brand-600 uppercase tracking-wider">Pricing</p>
              <h2 className="mt-2 text-3xl font-bold">Simple plans for every school</h2>
              <p className="mt-4 text-slate-600">No setup fees. Upgrade or change plans any time.</p>
            </div>

            <div className="grid md:grid-cols-3 gap-6 items-stretch">
              {PLANS.map((plan) => (
                <div
                  key={plan.name}
                  className={`relative rounded-2xl p-8 flex flex-col ${
                    plan.highlighted
                      ? 'bg-slate-900 text-white shadow-xl ring-2 ring-brand-500'
                      : 'card'
                  }`}
                >
                  {plan.highlighted && (
                    <span className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-brand-600 text-white text-xs font-semibold">
                      Most popular
                    </span>
                  )}
                  <h3 className="text-lg font-semibold">{plan.name}</h3>
                  <p className={`mt-1 text-sm ${plan.highlighted ? 'text-slate-300' : 'text-slate-500'}`}>
                    {plan.desc}
                  </p>
                  <p className="mt-6">
                    <span className="text-4xl font-bold">{plan.price}</span>
                    <span className={`text-sm ${plan.highlighted ? 'text-slate-300' : 'text-slate-500'}`}>
                      {plan.period}
                    </span>
                  </p>
                  <ul className="mt-6 space-y-3 flex-1">
                    {plan.features.map((f) => (
                      <li key={f} className="flex items-start gap-2 text-sm">
                        <Check className={`w-4 h-4 mt-0.5 flex-shrink-0 ${plan.highlighted ? 'text-brand-400' : 'text-brand-600'}`} />
                        <span className={plan.highlighted ? 'text-slate-200' : 'text-slate-700'}>{f}</span>
                      </li>
                    ))}
                  </ul>
                  {plan.price === 'Custom' ? (
                    <a href="#contact" className="mt-8 justify-center btn-secondary">Contact Sales</a>
                  ) : (
                    <Link
                      to="/register"
                      className={`mt-8 justify-center ${plan.highlighted ? 'btn-primary' : 'btn-secondary'}`}
                    >
                      Get Started
                    </Link>
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Contact ──────────────────────────────────────────── */}
        <section id="contact" className="scroll-mt-16 py-20 sm:py-24 bg-slate-50">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 grid lg:grid-cols-2 gap-12">
            <div>
              <p className="text-sm font-semibold text-brand-600 uppercase tracking-wider">Contact</p>
              <h2 className="mt-2 text-3xl font-bold">Talk to us</h2>
              <p className="mt-4 text-slate-600 leading-relaxed">
                Want a demo, have a question about pricing, or need help getting your school set up?
                Send us a message and we'll get back to you.
              </p>
              <ul className="mt-8 space-y-4">
                <ContactItem icon={Mail} label="Email" value={CONTACT.email} href={`mailto:${CONTACT.email}`} />
                <ContactItem icon={Phone} label="Phone" value={CONTACT.phone} href={`tel:${CONTACT.phone.replace(/\s/g, '')}`} />
                <ContactItem icon={MapPin} label="Address" value={CONTACT.address} />
              </ul>
            </div>

            <ContactForm />
          </div>
        </section>
      </main>

      {/* ── Footer ─────────────────────────────────────────────── */}
      <footer className="bg-slate-900 text-slate-400">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-12 grid gap-8 sm:grid-cols-3">
          <div>
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-brand-600 flex items-center justify-center">
                <GraduationCap className="w-4 h-4 text-white" />
              </div>
              <span className="font-bold text-white">School ERP</span>
            </div>
            <p className="mt-3 text-sm leading-relaxed">
              Fee management and student records for schools.
            </p>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">Quick Links</p>
            <ul className="mt-3 space-y-2 text-sm">
              {NAV_LINKS.map(({ href, label }) => (
                <li key={href}><a href={href} className="hover:text-white transition-colors">{label}</a></li>
              ))}
              <li><Link to={authCta.to} className="hover:text-white transition-colors">{authCta.label}</Link></li>
            </ul>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">Get in Touch</p>
            <ul className="mt-3 space-y-2 text-sm">
              <li><a href={`mailto:${CONTACT.email}`} className="hover:text-white transition-colors">{CONTACT.email}</a></li>
              <li>{CONTACT.phone}</li>
            </ul>
          </div>
        </div>
        <div className="border-t border-slate-800">
          <p className="max-w-6xl mx-auto px-4 sm:px-6 py-5 text-xs text-slate-500">
            © {new Date().getFullYear()} School ERP. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  )
}

function ContactItem({
  icon: Icon, label, value, href,
}: { icon: typeof Mail; label: string; value: string; href?: string }) {
  return (
    <li className="flex items-start gap-3">
      <div className="w-10 h-10 rounded-lg bg-white border border-slate-200 text-brand-600 flex items-center justify-center flex-shrink-0">
        <Icon className="w-5 h-5" />
      </div>
      <div>
        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{label}</p>
        {href
          ? <a href={href} className="text-slate-900 font-medium hover:text-brand-600">{value}</a>
          : <p className="text-slate-900 font-medium">{value}</p>}
      </div>
    </li>
  )
}

/**
 * There is no backend endpoint for enquiries yet, so the form opens the
 * visitor's email app with the message pre-filled.
 */
function ContactForm() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [school, setSchool] = useState('')
  const [message, setMessage] = useState('')

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    const subject = `Enquiry from ${school || name}`
    const body = `Name: ${name}\nEmail: ${email}\nSchool: ${school}\n\n${message}`
    window.location.href =
      `mailto:${CONTACT.email}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`
  }

  return (
    <form onSubmit={handleSubmit} className="card p-6 sm:p-8 space-y-5">
      <div className="grid sm:grid-cols-2 gap-5">
        <div>
          <label htmlFor="contact-name" className="label">Your name</label>
          <input id="contact-name" required className="input-field" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div>
          <label htmlFor="contact-email" className="label">Email</label>
          <input id="contact-email" type="email" required className="input-field" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
      </div>
      <div>
        <label htmlFor="contact-school" className="label">School name</label>
        <input id="contact-school" className="input-field" value={school} onChange={(e) => setSchool(e.target.value)} />
      </div>
      <div>
        <label htmlFor="contact-message" className="label">Message</label>
        <textarea
          id="contact-message"
          required
          rows={4}
          className="input-field resize-none"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
        />
      </div>
      <button type="submit" className="btn-primary w-full justify-center py-2.5">
        Send Message
      </button>
    </form>
  )
}
