import { useEffect, useEffectEvent, useMemo, useState } from 'react'
import axios from 'axios'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { Activity, Boxes, CircleDollarSign, Package, ShoppingBag, Star, Store, Users } from 'lucide-react'
import { adminApi } from '../api/services'
import type { AdminDashboardResponse, AdminProductResponse, AdminUserResponse, AdminVendorResponse, OrderResponse, ProductReviewResponse } from '../types/api'
import { formatCurrency } from '../lib/currency'

const money = formatCurrency
const date = (value: string) => new Date(value).toLocaleDateString()
const navigation = [
  { label: 'Dashboard', to: '/admin', icon: Activity, end: true },
  { label: 'Users', to: '/admin/users', icon: Users },
  { label: 'Vendors', to: '/admin/vendors', icon: Store },
  { label: 'Products', to: '/admin/products', icon: Boxes },
  { label: 'Orders', to: '/admin/orders', icon: ShoppingBag },
  { label: 'Payouts', to: '/admin/payouts', icon: CircleDollarSign },
  { label: 'Reviews', to: '/admin/reviews', icon: Star },
]

export function AdminPortalLayout() {
  const location = useLocation()
  const current = navigation.find(item => item.to === location.pathname)?.label ?? 'Dashboard'
  return <div className="-mx-5 -my-10 min-h-[calc(100vh-64px)] bg-slate-50 lg:flex">
    <aside className="border-b border-slate-200 bg-slate-950 px-4 py-5 text-slate-100 lg:w-64 lg:shrink-0 lg:border-b-0 lg:border-r lg:px-5">
      <div className="mb-6 flex items-center gap-3 px-2"><span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-400 text-slate-950"><Activity size={20}/></span><div><p className="font-semibold">Marketplace</p><p className="text-xs text-slate-400">Administration</p></div></div>
      <nav aria-label="Admin navigation" className="flex gap-1 overflow-x-auto lg:flex-col">
        {navigation.map(({ label, to, icon: Icon, end }) => <NavLink key={to} to={to} end={end} className={({ isActive }) => `flex shrink-0 items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition ${isActive ? 'bg-indigo-400/15 font-medium text-indigo-300' : 'text-slate-300 hover:bg-white/5 hover:text-white'}`}><Icon size={17}/>{label}</NavLink>)}
      </nav>
      <Link to="/" className="mt-7 hidden rounded-lg px-3 py-2 text-sm text-slate-400 hover:bg-white/5 hover:text-white lg:block">← Marketplace home</Link>
    </aside>
    <div className="min-w-0 flex-1">
      <div className="flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4 md:px-8"><div><p className="text-xs font-medium uppercase tracking-wider text-slate-500">Admin Portal</p><h1 className="mt-1 text-xl font-semibold text-slate-900">{current}</h1></div><span className="rounded-full bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-800">Platform Admin</span></div>
      <main className="p-5 md:p-8"><Outlet/></main>
    </div>
  </div>
}

function useLoad<T>(load: () => Promise<T>, key: string) {
  const [result, setResult] = useState<{ key: string; data: T } | null>(null)
  const [failure, setFailure] = useState<{ key: string; message: string } | null>(null)
  const loadData = useEffectEvent(load)
  useEffect(() => { let alive = true; loadData().then(data => { if (alive) setResult({ key, data }) }).catch(error => { if (alive) setFailure({ key, message: sectionLoadError(error) }) }); return () => { alive = false } }, [key])
  return { data: result?.key === key ? result.data : null, error: failure?.key === key ? failure.message : '' }
}

function sectionLoadError(error: unknown) {
  const fallback = 'Could not load this section. Check the connection and try again.'
  if (!import.meta.env.DEV || !axios.isAxiosError(error)) return fallback
  const status = error.response?.status
  const body: unknown = error.response?.data
  const message = body && typeof body === 'object' && 'message' in body && typeof body.message === 'string'
    ? body.message
    : typeof body === 'string' && body.trim() ? body.trim() : ''
  if (!status && !message) return `${fallback} (${error.message})`
  return `Admin API request failed${status ? ` (${status})` : ''}${message ? `: ${message}` : '.'}`
}

function State({ error, empty }: { error: string; empty?: boolean }) {
  if (error) return <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-800" role="alert">{error}</div>
  if (empty) return <div className="py-10 text-center text-sm text-slate-500">No records found.</div>
  return <div className="py-10 text-center text-sm text-slate-500" role="status">Loading marketplace data…</div>
}
function Panel({ title, children, action }: { title: string; children: React.ReactNode; action?: React.ReactNode }) { return <section className="overflow-hidden rounded-xl border border-slate-200 bg-white"><div className="flex items-center justify-between border-b border-slate-100 px-5 py-4"><h2 className="font-semibold text-slate-900">{title}</h2>{action}</div>{children}</section> }
function Table({ headers, children }: { headers: string[]; children: React.ReactNode }) { return <div className="overflow-x-auto"><table className="w-full min-w-[700px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500"><tr>{headers.map(header => <th key={header} className="px-5 py-3 font-medium">{header}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">{children}</tbody></table></div> }
function Cell({ children, className = '' }: { children: React.ReactNode; className?: string }) { return <td className={`px-5 py-3.5 text-slate-700 ${className}`}>{children}</td> }

export function AdminDashboardPage() {
  const { data, error } = useLoad<AdminDashboardResponse>(() => adminApi.dashboard(), 'dashboard')
  if (!data) return <State error={error}/>
  const metrics = [
    ['Total users', data.totalUsers, Users], ['Vendors', data.totalVendors, Store], ['Customers', data.totalCustomers, Users], ['Products', data.totalProducts, Boxes],
    ['Orders', data.totalOrders, ShoppingBag], ['Revenue', money(data.totalRevenue), CircleDollarSign], ['Pending payouts', money(data.pendingPayouts), Package], ['Commission', money(data.platformCommission), Activity],
  ] as const
  return <div className="space-y-6">
    <div><p className="text-sm text-slate-500">Marketplace performance at a glance</p><h2 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950">Good overview, all in one place.</h2></div>
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{metrics.map(([label, value, Icon]) => <div key={label} className="rounded-xl border border-slate-200 bg-white p-4"><div className="flex items-center justify-between"><span className="text-sm text-slate-500">{label}</span><Icon className="text-indigo-700" size={18}/></div><p className="mt-3 text-2xl font-semibold text-slate-950">{value}</p></div>)}</div>
    <div className="grid gap-5 xl:grid-cols-2">
      <Panel title="Recent orders" action={<Link className="text-sm font-medium text-indigo-800" to="/admin/orders">View all</Link>}>{data.recentOrders.length ? <Table headers={['Order','Customer','Date','Total','Status']}><>{data.recentOrders.map(order => <tr key={order.id}><Cell>#{order.id}</Cell><Cell>{order.username}</Cell><Cell>{date(order.placedAt)}</Cell><Cell>{money(order.totalAmount)}</Cell><Cell><Badge>{order.status}</Badge></Cell></tr>)}</></Table> : <State error="" empty/>}</Panel>
      <Panel title="Order status"><div className="grid grid-cols-2 gap-3 p-5 sm:grid-cols-3">{Object.entries(data.orderStatuses).map(([status, count]) => <div key={status} className="rounded-lg bg-slate-50 p-3"><p className="text-xs text-slate-500">{status}</p><p className="mt-1 text-xl font-semibold">{count}</p></div>)}</div></Panel>
      <Panel title="Top products">{data.topProducts.length ? <Table headers={['Product','Vendor','Units sold','Rating']}><>{data.topProducts.map(product => <tr key={product.id}><Cell className="font-medium">{product.name}</Cell><Cell>{product.vendorUsername}</Cell><Cell>{product.unitsSold}</Cell><Cell>★ {product.averageRating.toFixed(1)} ({product.reviewCount})</Cell></tr>)}</></Table> : <State error="" empty/>}</Panel>
      <Panel title="Top vendors">{data.topVendors.length ? <Table headers={['Vendor','Products','Orders','Gross revenue']}><>{data.topVendors.map(vendor => <tr key={vendor.id}><Cell className="font-medium">{vendor.username}</Cell><Cell>{vendor.productCount}</Cell><Cell>{vendor.orderCount}</Cell><Cell>{money(vendor.grossRevenue)}</Cell></tr>)}</></Table> : <State error="" empty/>}</Panel>
      <Panel title="Recent reviews">{data.recentReviews.length ? <div className="divide-y divide-slate-100">{data.recentReviews.map(review => <div key={review.id} className="px-5 py-4"><div className="flex justify-between gap-3"><p className="font-medium">{review.productName}</p><span className="shrink-0 text-amber-600">★ {review.rating}</span></div><p className="mt-1 line-clamp-2 text-sm text-slate-600">{review.text}</p><p className="mt-2 text-xs text-slate-400">{review.customerUsername} · {date(review.createdAt)}</p></div>)}</div> : <State error="" empty/>}</Panel>
      <Panel title="Revenue summary"><div className="p-5"><div className="flex items-end justify-between"><div><p className="text-sm text-slate-500">All recorded orders</p><p className="mt-1 text-3xl font-semibold">{money(data.totalRevenue)}</p></div><span className="rounded-full bg-indigo-50 px-3 py-1 text-xs text-indigo-800">Lifetime</span></div><div className="mt-5 h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full w-full rounded-full bg-indigo-600"/></div><p className="mt-3 text-xs text-slate-500">Platform commission {money(data.platformCommission)} · Pending vendor payouts {money(data.pendingPayouts)}</p></div></Panel>
    </div>
  </div>
}
function Badge({ children }: { children: React.ReactNode }) { return <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">{children}</span> }

export function AdminUsersPage() {
  const [role, setRole] = useState('')
  const { data, error } = useLoad<AdminUserResponse[]>(() => adminApi.users(role), role)
  return <Panel title="Marketplace users" action={<select aria-label="Filter users" className="rounded-lg border border-slate-200 px-3 py-2 text-sm" value={role} onChange={event => setRole(event.target.value)}><option value="">All roles</option><option value="CUSTOMER">Customers</option><option value="VENDOR">Vendors</option><option value="ADMIN">Admins</option></select>}>
    {data ? data.length ? <Table headers={['Username','Email','Role','Account status']}><>{data.map(user => <tr key={user.id}><Cell className="font-medium">{user.username}</Cell><Cell>{user.email}</Cell><Cell><div className="flex flex-wrap gap-1">{user.roles.map(item => <Badge key={item}>{item.replace('ROLE_','')}</Badge>)}</div></Cell><Cell><Badge>Not tracked</Badge></Cell></tr>)}</></Table> : <State error="" empty/> : <State error={error}/>}
  </Panel>
}
export function AdminVendorsPage() { const { data, error } = useLoad<AdminVendorResponse[]>(() => adminApi.vendors(), 'vendors'); return <Panel title="Vendor accounts">{data ? data.length ? <Table headers={['Vendor','Email','Products','Orders','Revenue','Pending payout','Paid payout','Status']}><>{data.map(v => <tr key={v.id}><Cell className="font-medium">{v.username}</Cell><Cell>{v.email}</Cell><Cell>{v.productCount}</Cell><Cell>{v.orderCount}</Cell><Cell>{money(v.grossRevenue)}</Cell><Cell>{money(v.pendingPayout)}</Cell><Cell>{money(v.paidPayout)}</Cell><Cell><Badge>{v.status === 'NOT_TRACKED' ? 'Not tracked' : v.status}</Badge></Cell></tr>)}</></Table> : <State error="" empty/> : <State error={error}/>}</Panel> }
export function AdminProductsPage() { const { data, error } = useLoad<AdminProductResponse[]>(() => adminApi.products(), 'products'); return <Panel title="All marketplace products">{data ? data.length ? <Table headers={['Product','Category','Vendor','Price','Stock','Rating','Status']}><>{data.map(p => <tr key={p.id}><Cell><div className="flex items-center gap-3">{p.imageUrl ? <img src={p.imageUrl} alt="" className="h-10 w-10 rounded-lg object-cover"/> : <span className="grid h-10 w-10 place-items-center rounded-lg bg-slate-100"><Package size={16}/></span>}<span className="font-medium">{p.name}</span></div></Cell><Cell>{p.category}</Cell><Cell>{p.vendorUsername}</Cell><Cell>{money(p.price)}</Cell><Cell>{p.stock}</Cell><Cell>★ {p.averageRating.toFixed(1)} ({p.reviewCount})</Cell><Cell><Badge>{p.status.replace('_',' ')}</Badge></Cell></tr>)}</></Table> : <State error="" empty/> : <State error={error}/>}</Panel> }
export function AdminOrdersPage() { const [status, setStatus] = useState(''); const { data, error } = useLoad<OrderResponse[]>(() => adminApi.orders(), 'orders'); const filtered = useMemo(() => data?.filter(order => !status || order.status === status), [data, status]); return <Panel title="Marketplace orders" action={<select aria-label="Filter orders" value={status} onChange={e => setStatus(e.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="">All statuses</option>{['PLACED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'].map(s => <option key={s}>{s}</option>)}</select>}>{filtered ? filtered.length ? <Table headers={['Order','Customer','Ship to','Date','Total','Vendors','Payment','Status']}><>{filtered.map(o => <tr key={o.id}><Cell className="font-medium">#{o.id}</Cell><Cell>{o.username}</Cell><Cell>{o.shippingAddress ? `${o.shippingAddress.city}, ${o.shippingAddress.state}` : '—'}</Cell><Cell>{date(o.placedAt)}</Cell><Cell>{money(o.totalAmount)}</Cell><Cell>{o.subOrders.length}</Cell><Cell><Badge>{o.paymentStatus}</Badge></Cell><Cell><Badge>{o.status}</Badge></Cell></tr>)}</></Table> : <State error="" empty/> : <State error={error}/>}</Panel> }
export function AdminPayoutsPage() { const [status, setStatus] = useState(''); const { data, error } = useLoad(() => adminApi.ledger(), 'payouts'); const rows = data?.records.filter(row => !status || row.payoutStatus === status); return <Panel title="Vendor payouts" action={<select aria-label="Filter payouts" className="rounded-lg border border-slate-200 px-3 py-2 text-sm" value={status} onChange={event => setStatus(event.target.value)}><option value="">All payouts</option><option value="PENDING">Pending</option><option value="PAID">Paid</option></select>}>{rows ? rows.length ? <Table headers={['Vendor','Order','Order status','Payment','Gross','Commission','Net payout','Payout status','Created','Paid']}><>{rows.map(row => <tr key={row.id}><Cell className="font-medium">{row.vendorUsername}</Cell><Cell>#{row.orderId}</Cell><Cell><Badge>{row.orderStatus}</Badge></Cell><Cell><Badge>{row.paymentStatus}</Badge></Cell><Cell>{money(row.totalOrderAmount)}</Cell><Cell>{money(row.platformCommissionFee)}</Cell><Cell>{money(row.netVendorPayout)}</Cell><Cell><Badge>{row.payoutStatus === 'PENDING' && row.paymentStatus !== 'PAID' ? 'Awaiting payment' : row.payoutStatus}</Badge></Cell><Cell>{date(row.createdAt)}</Cell><Cell>{row.paidAt ? date(row.paidAt) : '—'}</Cell></tr>)}</></Table> : <State error="" empty/> : <State error={error}/>}</Panel> }
export function AdminReviewsPage() { const { data, error } = useLoad<ProductReviewResponse[]>(() => adminApi.reviews(), 'reviews'); return <Panel title="Marketplace reviews">{data ? data.length ? <div className="divide-y divide-slate-100">{data.map(review => <article key={review.id} className="flex flex-wrap items-start justify-between gap-3 px-5 py-4"><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h3 className="font-semibold">{review.productName}</h3><Badge>Vendor: {review.vendorUsername ?? '—'}</Badge><Badge>Customer: {review.customerUsername}</Badge></div><p className="mt-2 text-sm leading-6 text-slate-600">{review.text}</p><p className="mt-2 text-xs text-slate-400">{date(review.createdAt)}</p></div><span className="shrink-0 rounded-full bg-amber-50 px-3 py-1.5 text-sm font-medium text-amber-700">★ {review.rating}/5</span></article>)}</div> : <State error="" empty/> : <State error={error}/>}</Panel> }
