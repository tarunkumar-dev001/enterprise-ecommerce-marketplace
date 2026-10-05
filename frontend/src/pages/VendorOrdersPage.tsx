import { useEffect, useState } from 'react'
import { Check, RefreshCw } from 'lucide-react'
import { vendorApi } from '../api/services'
import type { OrderResponse } from '../types/api'
import { useToast } from '../components/toastContext'
import { formatCurrency } from '../lib/currency'

const currency = { format: formatCurrency }

function allowedStatuses(current: OrderResponse['status']): OrderResponse['status'][] {
  if (current === 'PLACED') return ['PLACED', 'PROCESSING', 'CANCELLED']
  if (current === 'PROCESSING') return ['PROCESSING', 'SHIPPED', 'CANCELLED']
  if (current === 'SHIPPED') return ['SHIPPED', 'DELIVERED']
  return [current]
}

export function VendorOrdersPage() {
  const { showToast } = useToast()
  const [orders, setOrders] = useState<OrderResponse[] | null>(null)
  const [selectedStatuses, setSelectedStatuses] = useState<Record<number, OrderResponse['status']>>({})
  const [savingId, setSavingId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    vendorApi.listOrders()
      .then((result) => {
        if (!active) return
        setOrders(result)
        setSelectedStatuses(Object.fromEntries(result.map((order) => [order.id, order.status])))
      })
      .catch(() => { if (active) setError('Vendor orders could not be loaded.') })
    return () => { active = false }
  }, [])

  async function saveStatus(order: OrderResponse) {
    const status = selectedStatuses[order.id] ?? order.status
    if (status === order.status) return
    setSavingId(order.id)
    setError('')
    try {
      const updated = await vendorApi.updateOrderStatus(order.id, status)
      setOrders((current) => current?.map((item) => item.id === order.id ? updated : item) ?? null)
      setSelectedStatuses((current) => ({ ...current, [order.id]: updated.status }))
      showToast(`Order #${order.id} updated to ${updated.status}.`)
    } catch {
      showToast(`Order #${order.id} could not be updated. Refresh and try again.`, 'error')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <div className="space-y-7">
      <header className="border-b border-slate-300 pb-5">
        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-700">Vendor workspace</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">Order fulfillment</h1>
        <p className="mt-2 text-sm text-slate-600">Orders containing your products, with fulfillment status controls.</p>
      </header>

      {error && <p className="border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>}
      {orders === null && !error && <p className="py-10 text-sm text-slate-600" role="status">Loading orders…</p>}
      {orders?.length === 0 && <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-sm text-slate-600">No orders contain your products yet.</p>}

      <div className="space-y-4">
        {orders?.map((order) => (
          <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm" key={order.id}>
            <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 bg-slate-50 px-5 py-4">
              <div>
                <p className="font-semibold text-slate-950">Order #{order.id}</p>
                <p className="mt-1 text-xs text-slate-500">{order.username} · {new Date(order.placedAt).toLocaleDateString()}</p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <span className="rounded-sm bg-slate-200 px-2.5 py-1 text-xs font-semibold text-slate-700">Payment {order.paymentStatus}</span>
                <span className="text-sm font-semibold tabular-nums text-slate-900">{currency.format(order.totalAmount)}</span>
              </div>
            </header>
            {order.shippingAddress && <div className="border-b border-slate-100 bg-indigo-50/50 px-5 py-3 text-xs leading-5 text-slate-600"><span className="font-semibold text-slate-800">Ship to {order.shippingAddress.fullName}:</span> {order.shippingAddress.addressLine}, {order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.postalCode}, {order.shippingAddress.country} · {order.shippingAddress.phone}</div>}
            <div className="divide-y divide-slate-100 px-5">
              {order.items.map((item) => (
                <div className="grid grid-cols-[minmax(0,1fr)_4rem_7rem] gap-3 py-3 text-sm" key={item.id}>
                  <span className="truncate text-slate-800">{item.productName}</span>
                  <span className="text-right tabular-nums text-slate-500">× {item.quantity}</span>
                  <span className="text-right tabular-nums text-slate-700">{currency.format(item.subtotal)}</span>
                </div>
              ))}
            </div>
            <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 px-5 py-4">
              <span className="text-xs text-slate-500">Current status: <strong className="text-slate-800">{order.status}</strong></span>
              <div className="flex items-center gap-2">
                <label className="sr-only" htmlFor={`status-${order.id}`}>Status for order {order.id}</label>
                <select className="rounded-sm border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800" id={`status-${order.id}`} onChange={(event) => setSelectedStatuses((current) => ({ ...current, [order.id]: event.target.value as OrderResponse['status'] }))} value={selectedStatuses[order.id] ?? order.status}>
                  {allowedStatuses(order.status).map((status) => <option key={status} value={status}>{status}</option>)}
                </select>
                <button className="inline-flex items-center gap-2 rounded-sm bg-indigo-600 px-3 py-2 text-xs font-semibold text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50" disabled={savingId === order.id || (selectedStatuses[order.id] ?? order.status) === order.status} onClick={() => void saveStatus(order)} type="button">
                  {savingId === order.id ? <RefreshCw aria-hidden="true" className="animate-spin" size={14} /> : <Check aria-hidden="true" size={14} />}
                  Update status
                </button>
              </div>
            </footer>
          </article>
        ))}
      </div>
    </div>
  )
}
