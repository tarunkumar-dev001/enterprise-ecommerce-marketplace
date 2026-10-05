import { useEffect, useState } from 'react'
import { ArrowUpRight, Boxes, DollarSign, ShoppingBag } from 'lucide-react'
import { vendorApi } from '../api/services'
import type { VendorAnalyticsResponse } from '../types/api'
import { formatCurrency } from '../lib/currency'

const money = { format: formatCurrency }

export function VendorAnalyticsPage() {
  const [analytics, setAnalytics] = useState<VendorAnalyticsResponse | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    vendorApi.analytics(10)
      .then((result) => { if (active) setAnalytics(result) })
      .catch(() => { if (active) setError('Sales analytics could not be loaded. Try again.') })
    return () => { active = false }
  }, [])

  const bestUnits = Math.max(1, ...(analytics?.bestSellingProducts.map((item) => item.unitsSold) ?? []))

  return (
    <div className="space-y-8">
      <header className="border-b border-slate-300 pb-5">
        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-700">Vendor workspace</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">Sales analytics</h1>
        <p className="mt-2 text-sm text-slate-600">Paid sales performance across your products.</p>
      </header>

      {error && <p className="border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>}
      {!analytics && !error && <p className="py-10 text-sm text-slate-600" role="status">Loading analytics…</p>}

      {analytics && (
        <>
          <section aria-label="Sales metrics" className="grid gap-4 sm:grid-cols-3">
            <Metric icon={<DollarSign aria-hidden="true" size={18} />} label="Sales revenue" value={money.format(analytics.totalSalesRevenue)} detail="Paid, non-cancelled orders" />
            <Metric icon={<ShoppingBag aria-hidden="true" size={18} />} label="Orders" value={analytics.orderCount.toLocaleString()} detail="Orders with your products" />
            <Metric icon={<Boxes aria-hidden="true" size={18} />} label="Units sold" value={analytics.unitsSold.toLocaleString()} detail="Across all paid orders" />
          </section>

          <section className="border border-slate-200 bg-white">
            <div className="flex flex-wrap items-end justify-between gap-3 border-b border-slate-200 px-5 py-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-indigo-700">Product performance</p>
                <h2 className="mt-1 text-lg font-semibold text-slate-950">Best sellers</h2>
              </div>
              <span className="text-xs text-slate-500">Ranked by units sold</span>
            </div>
            {analytics.bestSellingProducts.length === 0 ? (
              <p className="p-8 text-sm text-slate-600">No paid sales yet. Product rankings will appear when orders are paid.</p>
            ) : (
              <ol className="divide-y divide-slate-100">
                {analytics.bestSellingProducts.map((product, index) => (
                  <li className="grid gap-3 px-5 py-4 sm:grid-cols-[2rem_minmax(0,1fr)_7rem_8rem] sm:items-center" key={product.productId}>
                    <span className="text-sm font-semibold tabular-nums text-slate-400">{String(index + 1).padStart(2, '0')}</span>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-slate-900">{product.productName}</p>
                      <div className="mt-2 h-1.5 overflow-hidden bg-slate-100">
                        <div className="h-full bg-indigo-600" style={{ width: `${Math.max(6, (product.unitsSold / bestUnits) * 100)}%` }} />
                      </div>
                    </div>
                    <span className="text-sm text-slate-600">{product.unitsSold.toLocaleString()} sold</span>
                    <span className="text-sm font-semibold tabular-nums text-slate-900">{money.format(product.salesRevenue)}</span>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </>
      )}
    </div>
  )
}

function Metric({ icon, label, value, detail }: { icon: React.ReactNode; label: string; value: string; detail: string }) {
  return (
    <article className="border border-slate-200 bg-white p-5">
      <div className="flex items-center justify-between text-indigo-700">
        <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
        {icon}
      </div>
      <p className="mt-5 text-3xl font-semibold tabular-nums tracking-tight text-slate-950">{value}</p>
      <p className="mt-1 flex items-center gap-1 text-xs text-slate-500"><ArrowUpRight aria-hidden="true" size={13} /> {detail}</p>
    </article>
  )
}
