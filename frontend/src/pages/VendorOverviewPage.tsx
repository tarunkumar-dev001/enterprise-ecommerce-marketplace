import { useEffect, useMemo, useState } from 'react'
import { ArrowRight, Boxes, CircleDollarSign, ClipboardList, PackageCheck, Star } from 'lucide-react'
import { Link } from 'react-router-dom'
import { vendorApi } from '../api/services'
import type { OrderResponse, VendorAnalyticsResponse, VendorLedgerSummaryResponse, VendorProductResponse } from '../types/api'
import { formatCurrency } from '../lib/currency'

const money = { format: formatCurrency }

export function VendorOverviewPage() {
  const [data, setData] = useState<{ analytics: VendorAnalyticsResponse; orders: OrderResponse[]; products: VendorProductResponse[]; ledger: VendorLedgerSummaryResponse; reviews: Awaited<ReturnType<typeof vendorApi.reviews>> } | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    Promise.all([vendorApi.analytics(), vendorApi.listOrders(), vendorApi.listProducts(), vendorApi.ledger(), vendorApi.reviews()])
      .then(([analytics, orders, products, ledger, reviews]) => { if (active) setData({ analytics, orders, products, ledger, reviews }) })
      .catch(() => { if (active) setError('Dashboard data could not be loaded. Please try again.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [attempt])

  const chart = useMemo(() => {
    const months = Array.from({ length: 6 }, (_, index) => {
      const date = new Date()
      date.setDate(1)
      date.setMonth(date.getMonth() - (5 - index))
      return { key: `${date.getFullYear()}-${date.getMonth()}`, label: date.toLocaleDateString(undefined, { month: 'short' }), total: 0 }
    })
    data?.orders.filter((order) => order.paymentStatus === 'PAID' && order.status !== 'CANCELLED').forEach((order) => {
      const date = new Date(order.placedAt)
      const month = months.find((item) => item.key === `${date.getFullYear()}-${date.getMonth()}`)
      if (month) month.total += order.totalAmount
    })
    return months
  }, [data])
  const max = Math.max(1, ...chart.map((item) => item.total))
  const averageRating = data?.reviews.length
    ? data.reviews.reduce((sum, review) => sum + review.rating, 0) / data.reviews.length
    : 0
  const recentOrders = data?.orders.slice(0, 5) ?? []

  return <div className="space-y-8">
    <header className="flex flex-wrap items-end justify-between gap-4 border-b border-stone-200 pb-6">
      <div><p className="text-xs font-semibold uppercase tracking-[0.14em] text-indigo-800">Vendor workspace</p><h1 className="mt-2 text-3xl font-semibold tracking-tight">Overview</h1><p className="mt-2 text-sm text-stone-600">Your store performance at a glance.</p></div>
      <Link className="inline-flex items-center gap-2 rounded-lg bg-indigo-900 px-4 py-2.5 text-sm font-medium text-white hover:bg-indigo-800" to="/vendor/products">Manage products <ArrowRight size={16} /></Link>
    </header>
    {loading && <p className="rounded-xl border border-stone-200 bg-white p-6 text-sm text-stone-600" role="status">Loading your dashboard…</p>}
    {error && <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-800" role="alert">{error}<button className="ml-3 font-semibold underline" onClick={() => { setError(''); setLoading(true); setAttempt((value) => value + 1) }} type="button">Retry</button></div>}
    {data && <>
      <section aria-label="Store performance" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <Stat icon={<CircleDollarSign />} label="Total revenue" value={money.format(data.analytics.totalSalesRevenue)} note="Paid, non-cancelled sales" />
        <Stat icon={<ClipboardList />} label="Total orders" value={data.orders.length.toLocaleString()} note="Orders with your products" />
        <Stat icon={<Boxes />} label="Total products" value={data.products.length.toLocaleString()} note="In your catalog" />
        <Stat icon={<PackageCheck />} label="Pending orders" value={data.orders.filter((order) => ['PLACED', 'PROCESSING'].includes(order.status)).length.toLocaleString()} note="Awaiting fulfillment" />
        <Stat icon={<Star />} label="Average rating" value={averageRating ? averageRating.toFixed(1) : '—'} note="From your product reviews" />
        <Stat icon={<CircleDollarSign />} label="Available payout" value={money.format(data.ledger.pendingPayouts)} note="Tracked in vendor ledger" />
      </section>
      <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
        <article className="rounded-2xl border border-stone-200 bg-white p-5 sm:p-6">
          <div className="flex items-start justify-between"><div><p className="text-xs font-semibold uppercase tracking-wide text-indigo-800">Paid sales</p><h2 className="mt-1 text-lg font-semibold">Revenue trend</h2></div><span className="text-xs text-stone-500">Last 6 months</span></div>
          {chart.every((item) => item.total === 0) ? <p className="py-16 text-center text-sm text-stone-500">Paid sales will appear here when available.</p> : <div className="mt-6 grid h-52 grid-cols-6 items-end gap-3" aria-label="Monthly paid revenue chart">{chart.map((item) => <div className="flex h-full flex-col items-center justify-end gap-2" key={item.key} title={`${item.label}: ${money.format(item.total)}`}><div className="w-full rounded-t-md bg-indigo-700" style={{ height: `${Math.max(item.total ? 8 : 0, item.total / max * 100)}%` }} /><span className="text-xs text-stone-500">{item.label}</span></div>)}</div>}
          <p className="mt-3 text-xs text-stone-500">Calculated from paid vendor orders by order date.</p>
        </article>
        <article className="rounded-2xl border border-stone-200 bg-white">
          <header className="flex items-center justify-between border-b border-stone-100 px-5 py-4"><div><p className="text-xs font-semibold uppercase tracking-wide text-indigo-800">Catalog</p><h2 className="mt-1 text-lg font-semibold">Top products</h2></div><Link className="text-sm font-medium text-indigo-900 hover:underline" to="/vendor/analytics">Analytics</Link></header>
          {!data.analytics.bestSellingProducts.length ? <p className="p-6 text-sm text-stone-500">No paid sales yet. Product rankings appear after your first sale.</p> : <ul className="divide-y divide-stone-100">{data.analytics.bestSellingProducts.slice(0, 5).map((product) => { const item = data.products.find((entry) => entry.id === product.productId); return <li className="flex items-center justify-between gap-3 px-5 py-4" key={product.productId}><div className="min-w-0"><p className="truncate text-sm font-semibold">{product.productName}</p><p className="mt-1 text-xs text-stone-500">{product.unitsSold} units · Stock {item?.stock ?? '—'}</p></div><span className="shrink-0 text-sm font-semibold">{money.format(product.salesRevenue)}</span></li> })}</ul>}
        </article>
      </section>
      <section className="rounded-2xl border border-stone-200 bg-white">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 px-5 py-4"><div><p className="text-xs font-semibold uppercase tracking-wide text-indigo-800">Fulfillment</p><h2 className="mt-1 text-lg font-semibold">Recent orders</h2></div><Link className="inline-flex items-center gap-1 text-sm font-semibold text-indigo-900 hover:underline" to="/vendor/orders">All orders <ArrowRight size={15} /></Link></header>
        {!recentOrders.length ? <p className="p-8 text-sm text-stone-500">Orders containing your products will appear here.</p> : <div className="overflow-x-auto"><table className="w-full min-w-[600px] text-left text-sm"><thead className="bg-stone-50 text-xs uppercase tracking-wide text-stone-500"><tr><th className="px-5 py-3 font-medium">Order</th><th className="px-5 py-3 font-medium">Customer</th><th className="px-5 py-3 font-medium">Date</th><th className="px-5 py-3 font-medium">Amount</th><th className="px-5 py-3 font-medium">Status</th></tr></thead><tbody className="divide-y divide-stone-100">{recentOrders.map((order) => <tr key={order.id}><td className="px-5 py-4 font-semibold">#{order.id}</td><td className="px-5 py-4">{order.username}</td><td className="px-5 py-4 text-stone-600">{new Date(order.placedAt).toLocaleDateString()}</td><td className="px-5 py-4 font-medium">{money.format(order.totalAmount)}</td><td className="px-5 py-4"><span className="rounded-full bg-stone-100 px-2.5 py-1 text-xs font-medium">{order.status}</span></td></tr>)}</tbody></table></div>}
      </section>
    </>}
  </div>
}

function Stat({ icon, label, value, note }: { icon: React.ReactNode; label: string; value: string; note: string }) {
  return <article className="rounded-2xl border border-stone-200 bg-white p-5"><div className="flex items-center justify-between"><span className="text-sm font-medium text-stone-600">{label}</span><span className="text-indigo-800">{icon}</span></div><p className="mt-5 text-2xl font-semibold tracking-tight tabular-nums">{value}</p><p className="mt-1 text-xs text-stone-500">{note}</p></article>
}
