import { useRef } from 'react'
import { ChevronDown } from 'lucide-react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const navigation = [
  { label: 'Overview', to: '/' },
  { label: 'Products', to: '/products' },
  { label: 'Cart', to: '/cart' },
  { label: 'Orders', to: '/orders' },
  { label: 'Wishlist', to: '/wishlist' },
]

const vendorNavigation = [
  { label: 'Overview', to: '/vendor' },
  { label: 'Products', to: '/vendor/products' },
  { label: 'Orders', to: '/vendor/orders' },
  { label: 'Analytics', to: '/vendor/analytics' },
  { label: 'Earnings & Payouts', to: '/vendor/earnings' },
  { label: 'Reviews', to: '/vendor/reviews' },
  { label: 'Store Profile', to: '/vendor/store' },
]

export function AppShell() {
  const { user, signOut } = useAuth()
  const location = useLocation()
  const vendorMenuRef = useRef<HTMLDetailsElement>(null)
  const isVendorRoute = location.pathname === '/vendor' || location.pathname.startsWith('/vendor/')
  const isVendor = Boolean(user?.roles.includes('ROLE_VENDOR'))
  const isAdmin = Boolean(user?.roles.includes('ROLE_ADMIN'))

  if (isAdmin && location.pathname.startsWith('/admin')) {
    return <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-5 py-3">
        <Link className="text-lg font-semibold tracking-tight text-slate-950" to="/admin">Market / Place <span className="ml-2 text-xs font-medium uppercase tracking-widest text-indigo-700">Admin</span></Link>
        <div className="flex items-center gap-3"><span className="hidden text-sm text-slate-600 sm:inline">{user?.username}</span><button className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium hover:bg-slate-50" onClick={signOut} type="button">Sign out</button></div>
      </header>
      <Outlet />
    </div>
  }

  return (
    <div className="min-h-screen bg-stone-100 text-stone-900">
      <header className="sticky top-0 z-50 border-b border-stone-200/70 bg-white/80 backdrop-blur-md supports-[backdrop-filter]:bg-white/75">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-5 gap-y-3 px-5 py-3">
          <Link className="shrink-0 text-lg font-semibold tracking-tight text-indigo-900" to="/">
            Market / Place
          </Link>
          <nav
            aria-label="Main navigation"
            className="order-3 -mx-1 flex w-full flex-wrap items-center gap-1 px-1 pb-1 md:order-none md:mx-0 md:w-auto md:flex-1 md:justify-center md:pb-0"
          >
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `inline-flex shrink-0 items-center gap-2 rounded-full px-3 py-2 text-sm transition-colors ${
                    isActive
                      ? 'bg-indigo-50 font-medium text-indigo-950 ring-1 ring-inset ring-indigo-200/80'
                      : 'text-stone-600 hover:bg-stone-100/80 hover:text-stone-950'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    {isActive && (
                      <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-indigo-700" />
                    )}
                    {item.label}
                  </>
                )}
              </NavLink>
            ))}
            {isVendor && <details className="group relative" ref={vendorMenuRef}>
              <summary
                className={`inline-flex cursor-pointer list-none items-center gap-1 rounded-full px-3 py-2 text-sm transition-colors marker:hidden [&::-webkit-details-marker]:hidden focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-700/40 ${
                  isVendorRoute
                    ? 'bg-indigo-50 font-medium text-indigo-950 ring-1 ring-inset ring-indigo-200/80'
                    : 'text-stone-600 hover:bg-stone-100/80 hover:text-stone-950'
                }`}
                aria-label="Vendor dashboard menu"
              >
                {isVendorRoute && <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-indigo-700" />}
                Vendor Dashboard
                <ChevronDown aria-hidden="true" className="transition-transform duration-200 group-open:rotate-180" size={15} />
              </summary>
              <div className="absolute left-0 top-full z-50 mt-2 min-w-52 origin-top-left rounded-xl border border-stone-200 bg-white p-1.5 shadow-lg shadow-stone-900/10 group-open:animate-[vendor-menu-in_140ms_ease-out] md:left-auto md:right-0">
                {vendorNavigation.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    onClick={() => { if (vendorMenuRef.current) vendorMenuRef.current.open = false }}
                    className={({ isActive }) =>
                      `block rounded-lg px-3 py-2.5 text-sm transition-colors ${
                        isActive
                          ? 'bg-indigo-50 font-medium text-indigo-950'
                          : 'text-stone-600 hover:bg-stone-100 hover:text-stone-950'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </details>}
          </nav>
          <div className="ml-auto flex shrink-0 items-center gap-2 text-sm">
            {user ? (
              <>
                <div className="flex min-w-0 items-center gap-2 rounded-full border border-stone-200/80 bg-white/70 py-1 pl-1 pr-3">
                  <span
                    aria-hidden="true"
                    className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold uppercase text-indigo-900"
                  >
                    {user.username.slice(0, 1)}
                  </span>
                  <span className="max-w-28 truncate font-medium text-stone-700">{user.username}</span>
                </div>
                <button
                  className="rounded-full px-3 py-2 font-medium text-stone-600 transition-colors hover:bg-stone-100 hover:text-stone-950"
                  onClick={signOut}
                  type="button"
                >
                  <span className="hidden sm:inline">Sign out</span>
                  <span className="sm:hidden">Exit</span>
                </button>
              </>
            ) : (
              <>
                <Link
                  className="rounded-full px-3 py-2 text-stone-600 transition-colors hover:bg-stone-100 hover:text-stone-950"
                  to="/login"
                >
                  Sign in
                </Link>
                <Link
                  className="rounded-full bg-indigo-900 px-4 py-2 font-medium text-white transition-colors hover:bg-indigo-800"
                  to="/register"
                >
                  Create account
                </Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-5 py-10">
        <Outlet />
      </main>
      <footer className="border-t border-stone-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-wrap justify-between gap-2 px-5 py-4 text-xs text-stone-500">
          <span>Marketplace frontend</span>
          <span>API: {import.meta.env.VITE_API_BASE_URL || '/api'}</span>
        </div>
      </footer>
    </div>
  )
}
