import { lazy, Suspense, useEffect, useState } from 'react'
import { ArrowRight, ShieldCheck, Store, Truck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import axios from 'axios'
import { orderApi, productApi } from '../api/services'
import type { OrderResponse, ProductResponse } from '../types/api'
import { formatCurrency } from '../lib/currency'
import { ProductImage } from '../components/ProductImage'
import { useAuth } from '../auth/useAuth'

const DeveloperScene = lazy(() => import('../components/DeveloperScene'))

type EndpointPageProps = {
  eyebrow: string
  title: string
  description: string
  endpoint?: string
}

function EndpointPage({ eyebrow, title, description, endpoint }: EndpointPageProps) {
  return (
    <section className="max-w-3xl">
      <p className="mb-3 text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">{eyebrow}</p>
      <h1 className="text-3xl font-semibold tracking-tight text-stone-950">{title}</h1>
      <p className="mt-3 max-w-2xl text-base leading-7 text-stone-600">{description}</p>
      {endpoint && (
        <div className="mt-8 inline-flex items-center gap-2 border border-stone-200 bg-white px-3 py-2 text-sm text-stone-700">
          <span className="h-2 w-2 rounded-full bg-indigo-600" aria-hidden="true" />
          <code>{endpoint}</code>
        </div>
      )}
    </section>
  )
}

export function HomePage() {
  const { user, isLoading: isAuthLoading } = useAuth()
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [catalogLoaded, setCatalogLoaded] = useState(false)
  const [catalogTotal, setCatalogTotal] = useState<number | null>(null)
  useEffect(() => {
    let active = true
    if (isAuthLoading || !user) return () => { active = false }
    productApi.browse({ page: 0, size: 100, sortBy: 'id', direction: 'desc' })
      .then((page) => { if (active) { setProducts(page.content); setCatalogTotal(page.totalElements) } })
      .catch(() => { if (active) { setProducts([]); setCatalogTotal(null) } })
      .finally(() => { if (active) setCatalogLoaded(true) })
    return () => { active = false }
  }, [isAuthLoading, user])
  const categories = [...new Set(products.map((product) => product.category))].slice(0, 4)
  const vendorCount = new Set(products.map((product) => product.vendorId)).size
  return (
    <div className="space-y-16 pb-8">
      <section className="relative grid items-center gap-6 overflow-hidden rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-white to-indigo-50/80 px-6 py-8 shadow-sm sm:px-10 lg:grid-cols-[0.9fr_1.1fr] lg:px-12 lg:py-10">
        <div className="relative z-10">
          <p className="mb-4 inline-flex items-center gap-2 rounded-full border border-indigo-100 bg-white/80 px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.14em] text-indigo-800"><span className="h-1.5 w-1.5 rounded-full bg-indigo-600"/> Independent finds, all in one place</p>
          <h1 className="max-w-xl text-4xl font-semibold leading-[1.08] tracking-tight text-slate-950 sm:text-5xl lg:text-6xl">Everything you need. <span className="text-indigo-700">From every store.</span></h1>
          <p className="mt-5 max-w-lg text-base leading-7 text-slate-600">Explore thoughtful products from independent vendors, then shop, save, and track every order in one place.</p>
          <div className="mt-7 flex flex-wrap gap-3"><Link className="inline-flex items-center gap-2 rounded-xl bg-indigo-700 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-700/15 transition hover:-translate-y-0.5 hover:bg-indigo-800" to="/products">Explore marketplace <ArrowRight aria-hidden="true" size={16}/></Link><Link className="inline-flex items-center rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-indigo-300 hover:text-indigo-800" to="/register">Become a vendor</Link></div>
          <div className="mt-9 flex flex-wrap gap-x-7 gap-y-3 border-t border-slate-200 pt-5 text-xs text-slate-500"><span>{catalogTotal !== null ? `${catalogTotal} marketplace products` : 'Curated marketplace'}</span><span>{vendorCount ? `${vendorCount} featured stores` : 'Independent stores'}</span><span>One multi-vendor checkout</span></div>
        </div>
        <div className="-mx-5 -my-5 sm:-mx-8 lg:mx-0 lg:my-0">
          <Suspense fallback={<div className="h-72 animate-pulse rounded-3xl bg-slate-100 sm:h-96 lg:h-[30rem]" role="status" aria-label="Loading 3D marketplace scene" />}>
            <DeveloperScene />
          </Suspense>
        </div>
      </section>
      <section className="grid gap-4 sm:grid-cols-3" aria-label="Marketplace features">
        {[[Store, 'Independent stores', 'Discover products from marketplace vendors.'],[Truck, 'Multi-vendor checkout', 'Bring different stores together in one cart.'],[ShieldCheck, 'Account protected', 'JWT authentication and role-based access.']].map(([Icon,title,description]) => { const FeatureIcon = Icon as typeof Store; return <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md" key={String(title)}><span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-50 text-indigo-700"><FeatureIcon aria-hidden="true" size={19}/></span><h2 className="mt-4 font-semibold text-slate-950">{String(title)}</h2><p className="mt-1 text-sm leading-6 text-slate-600">{String(description)}</p></article> })}
      </section>
      {!!categories.length && <section><div className="mb-5 flex items-end justify-between gap-4"><div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-indigo-700">Browse by interest</p><h2 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950">Find your next favorite</h2></div><Link className="text-sm font-semibold text-indigo-700" to="/products">All products →</Link></div><div className="grid grid-cols-2 gap-3 md:grid-cols-4">{categories.map((category,index) => <Link className="group rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-indigo-200 hover:shadow-md" key={category} to={`/products?category=${encodeURIComponent(category)}`}><span className="text-xs font-semibold text-indigo-600">0{index+1}</span><h3 className="mt-5 font-semibold text-slate-900 group-hover:text-indigo-700">{category}</h3><p className="mt-1 text-xs text-slate-500">Explore collection <span aria-hidden="true">→</span></p></Link>)}</div></section>}
      <section><div className="mb-5 flex items-end justify-between gap-4"><div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-indigo-700">Marketplace selection</p><h2 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950">Featured products</h2></div><Link className="text-sm font-semibold text-indigo-700" to="/products">Shop all →</Link></div>{products.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{products.slice(0,4).map(product => <Link className="group overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-xl" key={product.id} to={`/products/${product.id}`}><div className="aspect-[4/3] overflow-hidden bg-slate-100"><ProductImage alt={product.name} className="h-full w-full transition duration-500 group-hover:scale-105" src={product.imageUrl}/></div><div className="p-4"><p className="truncate text-xs text-slate-500">{product.vendorUsername}</p><h3 className="mt-1 truncate font-semibold text-slate-950 group-hover:text-indigo-700">{product.name}</h3><p className="mt-3 font-semibold text-indigo-800">{formatCurrency(product.price)}</p></div></Link>)}</div> : <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-600">{!user ? 'Sign in to explore products from the live catalog.' : catalogLoaded ? 'Products will appear here as vendors add listings.' : 'Loading marketplace highlights…'}</div>}</section>
      <section className="flex flex-col justify-between gap-5 rounded-3xl bg-indigo-950 px-6 py-7 text-white sm:flex-row sm:items-center sm:px-8"><div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-indigo-300">For independent sellers</p><h2 className="mt-2 text-2xl font-semibold">Bring your store to the marketplace.</h2><p className="mt-2 max-w-xl text-sm leading-6 text-indigo-100/75">Create products, manage orders, and follow store performance from one vendor workspace.</p></div><Link aria-label="Start selling" className="seller-cta-link inline-flex shrink-0 items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-semibold" to="/register">Start selling <ArrowRight aria-hidden="true" className="seller-cta-arrow" size={16}/></Link></section>
    </div>
  )
}

export function OrderDetailPage() {
  const { orderId } = useParams()
  const id = Number(orderId)
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const [paymentStatus, setPaymentStatus] = useState<OrderResponse['paymentStatus'] | null>(null)
  useEffect(() => {
    let active = true
    if (!Number.isSafeInteger(id) || id <= 0) return () => { active = false }
    orderApi.details(id).then(value => { if (active) setOrder(value) }).catch((failure: unknown) => {
      if (!active) return
      const data: unknown = axios.isAxiosError(failure) ? failure.response?.data : null
      const message = data && typeof data === 'object' && 'message' in data && typeof data.message === 'string' ? data.message : 'We could not load this order. Please try again.'
      setError(message)
    })
    return () => { active = false }
  }, [id])

  async function payDemo() {
    if (!order) return
    setBusy(true); setNotice('')
    try { const result = await orderApi.simulatePayment(order.id); setPaymentStatus(result.paymentStatus); setOrder(await orderApi.details(order.id)); setNotice(result.paymentStatus === 'PAID' ? 'Demo payment succeeded. No real money was charged.' : 'Demo payment failed. You can retry this test transaction.') }
    catch { setNotice('Demo payment could not be completed. Please retry.') }
    finally { setBusy(false) }
  }
  async function cancel() {
    if (!order) return
    setBusy(true); setNotice('')
    try { setOrder(await orderApi.cancel(order.id)); setNotice('Order cancelled. Stock was restored. Demo payment is not refunded automatically.') }
    catch (failure: unknown) {
      const data: unknown = axios.isAxiosError(failure) ? failure.response?.data : null
      setNotice(data && typeof data === 'object' && 'message' in data && typeof data.message === 'string' ? data.message : 'This order could not be cancelled.')
    } finally { setBusy(false) }
  }
  async function downloadInvoice() {
    if (!order) return
    setBusy(true)
    try { const blob = await orderApi.downloadInvoice(order.id); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = `invoice-${order.id}.pdf`; anchor.click(); URL.revokeObjectURL(url) }
    catch { setNotice('Invoice could not be downloaded. Invoices are available after successful demo payment.') }
    finally { setBusy(false) }
  }
  if (!Number.isSafeInteger(id) || id <= 0 || error) return <section className="mx-auto max-w-3xl rounded-2xl border border-rose-200 bg-white p-8"><p className="text-sm font-semibold text-rose-700" role="alert">{!Number.isSafeInteger(id) || id <= 0 ? 'This order number is not valid.' : error}</p><Link className="mt-5 inline-flex rounded-lg bg-indigo-700 px-4 py-2.5 text-sm font-semibold text-white" to="/orders">Back to orders</Link></section>
  if (!order) return <div aria-label="Loading order details" className="mx-auto max-w-5xl animate-pulse space-y-5" role="status"><div className="h-32 rounded-2xl bg-slate-200"/><div className="h-64 rounded-2xl bg-slate-200"/><span className="sr-only">Loading order details…</span></div>
  const steps: OrderResponse['status'][] = ['PLACED','PROCESSING','SHIPPED','DELIVERED']
  const statusIndex = steps.indexOf(order.status)
  const displayedPayment = paymentStatus ?? order.paymentStatus
  return <main className="mx-auto max-w-5xl space-y-6">
    <Link className="text-sm font-semibold text-indigo-700 hover:text-indigo-900" to="/orders">← All orders</Link>
    <header className="rounded-3xl bg-slate-950 px-6 py-8 text-white sm:px-9"><div className="flex flex-wrap items-start justify-between gap-5"><div><p className="text-xs font-semibold uppercase tracking-[0.18em] text-indigo-300">Order details</p><h1 className="mt-2 text-3xl font-semibold tracking-tight">Order #{order.id}</h1><p className="mt-2 text-sm text-slate-300">Placed {new Date(order.placedAt).toLocaleString('en-IN', { dateStyle: 'long', timeStyle: 'short' })}</p></div><div className="flex flex-wrap gap-2"><span className="rounded-full bg-white/10 px-3 py-1.5 text-xs font-semibold">{order.status.replace('_',' ')}</span><span className="rounded-full bg-indigo-400/20 px-3 py-1.5 text-xs font-semibold text-indigo-100">Payment {displayedPayment}</span></div></div>
      {order.status !== 'CANCELLED' ? <ol aria-label="Order progress" className="mt-8 grid grid-cols-4 gap-2">{steps.map((step,index) => <li key={step}><div className={`mb-2 h-1.5 rounded-full ${index <= statusIndex ? 'bg-indigo-400' : 'bg-white/15'}`} /><span className={`text-[11px] font-medium sm:text-xs ${index <= statusIndex ? 'text-white' : 'text-slate-400'}`}>{step.charAt(0) + step.slice(1).toLowerCase()}</span></li>)}</ol> : <p className="mt-6 rounded-xl bg-rose-400/15 px-4 py-3 text-sm text-rose-100">This order was cancelled. Stock was restored. Refunds are not automated in this demo.</p>}
    </header>
    {notice && <p className="rounded-xl border border-indigo-200 bg-indigo-50 p-4 text-sm text-indigo-950" role="status">{notice}</p>}
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_19rem]"><section className="space-y-5">{order.subOrders.map(subOrder => <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm" key={subOrder.id ?? subOrder.vendorId}><header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4"><div><p className="text-xs font-medium uppercase tracking-wide text-slate-500">Seller</p><h2 className="mt-1 font-semibold text-slate-950">{subOrder.vendorUsername}</h2></div><span className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700">{subOrder.status}</span></header><ul className="divide-y divide-slate-100">{subOrder.items.map(item => <li className="flex gap-4 p-5" key={item.id ?? `${item.productId}-${item.productName}`}><ProductImage alt={item.productName} className="h-20 w-20 shrink-0 rounded-xl object-cover" src={item.imageUrl}/><div className="min-w-0 flex-1"><Link className="font-medium text-slate-950 hover:text-indigo-700" to={`/products/${item.productId}`}>{item.productName}</Link><p className="mt-1 text-sm text-slate-500">{item.quantity} × {formatCurrency(item.unitPrice)}</p></div><span className="font-semibold tabular-nums text-slate-900">{formatCurrency(item.subtotal)}</span></li>)}</ul><div className="flex justify-between border-t border-slate-100 px-5 py-4 text-sm"><span className="text-slate-500">Vendor subtotal</span><span className="font-semibold">{formatCurrency(subOrder.totalAmount)}</span></div></article>)}</section>
      <aside className="space-y-5"><section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="font-semibold text-slate-950">Order summary</h2><dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between"><dt className="text-slate-500">Subtotal</dt><dd>{formatCurrency(order.subtotalAmount)}</dd></div><div className="flex justify-between"><dt className="text-slate-500">Discount</dt><dd>−{formatCurrency(order.discountAmount)}</dd></div><div className="flex justify-between border-t border-slate-100 pt-3 text-base font-semibold"><dt>Total</dt><dd>{formatCurrency(order.totalAmount)}</dd></div></dl><p className="mt-4 rounded-xl bg-indigo-50 p-3 text-xs leading-5 text-indigo-900"><strong>Demo Payment:</strong> test transaction only. No real money is charged.</p>{order.paymentStatus !== 'PAID' && order.status !== 'CANCELLED' && <button className="mt-4 w-full rounded-lg bg-indigo-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800 disabled:opacity-60" disabled={busy} onClick={() => void payDemo()} type="button">{busy ? 'Processing…' : 'Pay with demo payment'}</button>}{order.paymentStatus === 'PAID' && <button className="mt-4 w-full rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold hover:bg-slate-50 disabled:opacity-60" disabled={busy} onClick={() => void downloadInvoice()} type="button">Download invoice</button>}{(order.status === 'PLACED' || order.status === 'PROCESSING') && <button className="mt-3 w-full rounded-lg border border-rose-200 px-4 py-2.5 text-sm font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-60" disabled={busy} onClick={() => void cancel()} type="button">Cancel order</button>}</section>
        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="font-semibold text-slate-950">Shipping address</h2>{order.shippingAddress ? <address className="mt-3 space-y-1 text-sm not-italic leading-6 text-slate-600"><p className="font-medium text-slate-900">{order.shippingAddress.fullName}</p><p>{order.shippingAddress.addressLine}</p><p>{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.postalCode}</p><p>{order.shippingAddress.country}</p><p>{order.shippingAddress.phone}</p></address> : <p className="mt-3 text-sm leading-6 text-slate-500">No shipping address was recorded for this order.</p>}</section>
      </aside></div>
  </main>
}

export function NotFoundPage() {
  return <EndpointPage eyebrow="404" title="Page not found" description="That route is not part of the marketplace frontend." />
}
