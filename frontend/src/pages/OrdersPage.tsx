import { useEffect, useState } from 'react'
import { FileDown, PackageCheck, ReceiptText } from 'lucide-react'
import { Link } from 'react-router-dom'
import { orderApi } from '../api/services'
import type { OrderResponse } from '../types/api'
import { formatCurrency } from '../lib/currency'

function statusTone(status: string) {
  if (status === 'PAID' || status === 'DELIVERED') return 'bg-indigo-100 text-indigo-900'
  if (status === 'FAILED' || status === 'CANCELLED') return 'bg-red-100 text-red-800'
  return 'bg-amber-100 text-amber-900'
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function OrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[] | null>(null)
  const [error, setError] = useState('')
  const [downloadingId, setDownloadingId] = useState<number | null>(null)
  const [downloadError, setDownloadError] = useState('')

  useEffect(() => {
    let active = true
    orderApi.list()
      .then((result) => { if (active) setOrders(result) })
      .catch(() => { if (active) setError('Order history could not be loaded. Try again.') })
    return () => { active = false }
  }, [])

  async function downloadInvoice(order: OrderResponse) {
    setDownloadingId(order.id)
    setDownloadError('')
    try {
      const pdf = await orderApi.downloadInvoice(order.id)
      const url = URL.createObjectURL(pdf)
      const link = document.createElement('a')
      link.href = url
      link.download = `invoice-${order.id}.pdf`
      document.body.append(link)
      link.click()
      link.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch {
      setDownloadError(`Invoice for order #${order.id} could not be downloaded.`)
    } finally {
      setDownloadingId(null)
    }
  }

  return (
    <div className="space-y-7">
      <header className="border-b border-stone-300 pb-5">
        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">Customer</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-stone-950">Order history</h1>
        <p className="mt-2 text-sm text-stone-600">Items, payment status, fulfillment, and invoices for your orders. Payments are demo transactions; no real money is charged.</p>
      </header>

      {downloadError && <p className="border border-red-200 bg-red-50 p-3 text-sm text-red-800" role="alert">{downloadError}</p>}
      {error && <p className="border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>}
      {orders === null && !error && <p className="py-10 text-sm text-stone-600" role="status">Loading your orders…</p>}
      {orders?.length === 0 && (
        <section className="border border-dashed border-stone-300 bg-white p-10 text-center">
          <PackageCheck className="mx-auto text-indigo-800" size={25} />
          <h2 className="mt-4 font-semibold text-stone-950">No orders yet</h2>
          <p className="mt-2 text-sm text-stone-600">Your completed checkouts will appear here.</p>
          <Link className="mt-5 inline-flex rounded-sm bg-indigo-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800" to="/products">Browse products</Link>
        </section>
      )}

      <div className="space-y-5">
        {orders?.map((order) => (
          <article className="border border-stone-200 bg-white" key={order.id}>
            <header className="flex flex-wrap items-center justify-between gap-4 border-b border-stone-200 bg-stone-50 px-5 py-4">
              <div>
                <p className="text-sm font-semibold text-stone-950">Order #{order.id}</p>
                <time className="mt-1 block text-xs text-stone-500" dateTime={order.placedAt}>{formatDate(order.placedAt)}</time>
              </div>
              <div className="flex flex-wrap gap-2">
                <span className={`rounded-sm px-2.5 py-1 text-xs font-semibold ${statusTone(order.status)}`}>{order.status}</span>
                <span className={`rounded-sm px-2.5 py-1 text-xs font-semibold ${statusTone(order.paymentStatus)}`}>{order.paymentStatus}</span>
              </div>
            </header>

            <div className="divide-y divide-stone-100 px-5">
              {order.items.map((item) => (
                <div className="grid grid-cols-[minmax(0,1fr)_3.5rem_6rem] gap-3 py-3 text-sm" key={item.id}>
                  <span className="min-w-0 truncate font-medium text-stone-800">{item.productName}</span>
                  <span className="text-right tabular-nums text-stone-500">× {item.quantity}</span>
                  <span className="text-right tabular-nums text-stone-800">{formatCurrency(item.subtotal)}</span>
                </div>
              ))}
            </div>

            <footer className="flex flex-wrap items-end justify-between gap-5 border-t border-stone-200 px-5 py-4">
              <div className="space-y-1 text-xs text-stone-500">
                <p>Subtotal <span className="ml-2 tabular-nums text-stone-700">{formatCurrency(order.subtotalAmount)}</span></p>
                {order.couponCode && <p>Code <span className="ml-2 font-medium text-stone-700">{order.couponCode}</span></p>}
                <p>Discount <span className="ml-2 tabular-nums text-stone-700">−{formatCurrency(order.discountAmount)}</span></p>
              </div>
              <div className="flex flex-wrap items-center gap-4">
                <p className="text-sm font-semibold text-stone-950">Total <span className="ml-2 tabular-nums">{formatCurrency(order.totalAmount)}</span></p>
                {order.paymentStatus === 'PAID' ? (
                  <button
                    className="inline-flex items-center gap-2 rounded-sm border border-stone-300 px-3 py-2 text-xs font-semibold text-stone-700 hover:border-indigo-800 hover:text-indigo-900 disabled:opacity-50"
                    disabled={downloadingId === order.id}
                    onClick={() => void downloadInvoice(order)}
                    type="button"
                  >
                    <FileDown aria-hidden="true" size={15} />
                    {downloadingId === order.id ? 'Preparing…' : 'Download invoice'}
                  </button>
                ) : (
                  <span className="inline-flex items-center gap-2 text-xs text-stone-500" title="Invoices are available after payment">
                    <ReceiptText aria-hidden="true" size={15} /> Invoice after payment
                  </span>
                )}
              </div>
            </footer>
          </article>
        ))}
      </div>
    </div>
  )
}
