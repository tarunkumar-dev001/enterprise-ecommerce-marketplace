import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import axios from 'axios'
import { ArrowRight, LoaderCircle, Minus, Plus, ShieldCheck, Tag, Trash2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cartApi, orderApi } from '../api/services'
import type { CartResponse, OrderResponse, PaymentResponse } from '../types/api'
import { useToast } from '../components/toastContext'
import { formatCurrency } from '../lib/currency'
import type { ShippingAddress } from '../types/api'

function requestError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const body: unknown = error.response?.data
    if (typeof body === 'string' && body.trim()) return body
    if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
      return body.message
    }
  }
  return 'The request could not be completed. Please try again.'
}

function groupCartItems(items: CartResponse['items']) {
  const groups = new Map<number, { vendorId: number; vendorUsername: string; items: CartResponse['items'] }>()
  items.forEach((item) => {
    const group = groups.get(item.vendorId) ?? { vendorId: item.vendorId, vendorUsername: item.vendorUsername, items: [] }
    group.items.push(item)
    groups.set(item.vendorId, group)
  })
  return [...groups.values()]
}

export function CartPage() {
  const { showToast } = useToast()
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [couponCode, setCouponCode] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isCheckingOut, setIsCheckingOut] = useState(false)
  const [busyProductId, setBusyProductId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [checkoutOrder, setCheckoutOrder] = useState<OrderResponse | null>(null)
  const [payment, setPayment] = useState<PaymentResponse | null>(null)
  const [shippingOpen, setShippingOpen] = useState(false)
  const [shippingAddress, setShippingAddress] = useState<ShippingAddress>({
    fullName: '', phone: '', addressLine: '', city: '', state: '', postalCode: '', country: 'India',
  })

  useEffect(() => {
    let active = true
    cartApi.get()
      .then((response) => { if (active) setCart(response) })
      .catch((requestFailure: unknown) => { if (active) setError(requestError(requestFailure)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [])

  async function changeQuantity(productId: number, quantity: number) {
    setBusyProductId(productId)
    try {
      setCart(await cartApi.updateItemQuantity(productId, quantity))
      showToast('Cart quantity updated.')
    } catch (requestFailure) {
      showToast(requestError(requestFailure), 'error')
    } finally {
      setBusyProductId(null)
    }
  }

  async function removeItem(productId: number) {
    setBusyProductId(productId)
    try {
      setCart(await cartApi.removeItem(productId))
      showToast('Item removed from your cart.')
    } catch (requestFailure) {
      showToast(requestError(requestFailure), 'error')
    } finally {
      setBusyProductId(null)
    }
  }

  async function applyCoupon(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      setCart(await cartApi.applyCoupon(couponCode.trim()))
      showToast('Discount code applied.')
    } catch (requestFailure) {
      showToast(requestError(requestFailure), 'error')
    }
  }

  async function removeCoupon() {
    try {
      setCart(await cartApi.removeCoupon())
      showToast('Discount code removed.')
      setCouponCode('')
    } catch (requestFailure) {
      showToast(requestError(requestFailure), 'error')
    }
  }

  async function payForOrder(order: OrderResponse) {
    setIsCheckingOut(true)
    try {
      const result = await orderApi.simulatePayment(order.id)
      setPayment(result)
      if (result.paymentStatus === 'PAID') {
        showToast('Demo payment complete. No real money was charged.')
      } else {
        showToast('Demo payment was declined. Your order is saved and the test can be retried.', 'error')
      }
    } catch (requestFailure) {
      showToast(requestError(requestFailure), 'error')
    } finally {
      setIsCheckingOut(false)
    }
  }

  async function checkout() {
    setIsCheckingOut(true)
    setPayment(null)
    try {
      if (!cart || cart.items.length === 0) return
      const order = await orderApi.checkout(
        cart.items.map((item) => ({ productId: item.productId, quantity: item.quantity })),
        cart.couponCode ?? undefined,
        shippingAddress,
      )
      setCheckoutOrder(order)
      setCart((current) => current
        ? { ...current, items: [], subtotal: 0, discountAmount: 0, couponCode: null, totalAmount: 0 }
        : current)
      showToast(`Order #${order.id} placed successfully.`)
    } catch (requestFailure) {
      showToast(requestError(requestFailure), 'error')
    } finally {
      setIsCheckingOut(false)
    }
  }

  if (isLoading) {
    return <div className="py-16 text-sm text-stone-600" role="status">Loading your cart…</div>
  }

  if (checkoutOrder) {
    return (
      <section className="mx-auto max-w-3xl border border-indigo-200 bg-white p-8 sm:p-10">
        <div className="flex h-11 w-11 items-center justify-center rounded-full bg-indigo-100 text-indigo-900">
          <ShieldCheck aria-hidden="true" size={23} />
        </div>
        <p className="mt-6 text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">Order placed</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-stone-950">Thank you for your order</h1>
        <div className="mt-5 grid gap-3 border-y border-stone-200 py-4 text-sm sm:grid-cols-3">
          <p><span className="text-stone-500">Order</span><br /><strong className="text-stone-950">#{checkoutOrder.id}</strong></p>
          <p><span className="text-stone-500">Total</span><br /><strong className="text-stone-950">{formatCurrency(checkoutOrder.totalAmount)}</strong></p>
          <p><span className="text-stone-500">Payment status</span><br /><strong className="text-stone-950">{payment?.paymentStatus ?? checkoutOrder.paymentStatus}</strong></p>
        </div>
        <section aria-label="Order breakdown by vendor" className="mt-7">
          <h2 className="font-semibold text-stone-950">Vendor order breakdown</h2>
          {checkoutOrder.subOrders.length > 0 ? (
            <div className="mt-3 divide-y divide-stone-200 border-y border-stone-200">
              {checkoutOrder.subOrders.map((subOrder) => (
                <article className="py-4" key={subOrder.id ?? subOrder.vendorId}>
                  <div className="flex flex-wrap justify-between gap-2">
                    <h3 className="font-medium text-stone-900">{subOrder.vendorUsername}</h3>
                    <span className="font-semibold tabular-nums text-stone-900">{formatCurrency(subOrder.totalAmount)}</span>
                  </div>
                  <ul className="mt-2 space-y-1 text-sm text-stone-600">
                    {subOrder.items.map((item) => (
                      <li className="flex justify-between gap-3" key={item.id ?? `${item.productId}-${item.productName}`}>
                        <span>{item.productName} × {item.quantity}</span>
                        <span className="tabular-nums">{formatCurrency(item.subtotal)}</span>
                      </li>
                    ))}
                  </ul>
                </article>
              ))}
            </div>
          ) : (
            <p className="mt-2 text-sm text-stone-600">Your order items are being prepared.</p>
          )}
        </section>
        <div className="mt-6 rounded-xl border border-indigo-100 bg-indigo-50/70 p-4 text-sm text-indigo-950">
          <p className="font-semibold">Demo Payment</p>
          <p className="mt-1 text-indigo-800">This is a test transaction. No real money is charged.</p>
        </div>
        {payment?.paymentStatus !== 'PAID' && (
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <button
              className="inline-flex items-center gap-2 rounded-sm bg-indigo-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800 disabled:cursor-wait disabled:opacity-50"
              disabled={isCheckingOut}
              onClick={() => void payForOrder(checkoutOrder)}
              type="button"
            >
              {isCheckingOut && <LoaderCircle aria-hidden="true" className="animate-spin" size={16} />}
              {isCheckingOut ? 'Processing demo payment…' : payment ? 'Retry demo payment' : 'Pay with demo payment'}
            </button>
            {payment && <span className="text-sm text-rose-700">Payment was not completed. Your order is saved.</span>}
          </div>
        )}
        <div className="mt-7 flex flex-wrap gap-3">
          <Link className="inline-flex items-center gap-2 rounded-sm bg-indigo-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800" to={`/orders/${checkoutOrder.id}`}>
            View order <ArrowRight aria-hidden="true" size={16} />
          </Link>
          <Link className="rounded-sm border border-stone-300 px-4 py-2.5 text-sm font-medium text-stone-700 hover:bg-stone-50" to="/products">Continue shopping</Link>
        </div>
      </section>
    )
  }

  return (
    <div className="space-y-8">
      <header className="border-b border-stone-300 pb-5">
        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">Customer</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-stone-950">Your cart</h1>
        <p className="mt-2 text-sm text-stone-600">Review quantities and discounts before placing your order.</p>
      </header>

      {error && <p className="border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>}


      {!cart || cart.items.length === 0 ? (
        <section className="border border-dashed border-stone-300 bg-white p-10 text-center">
          <h2 className="text-lg font-semibold text-stone-950">Your cart is empty</h2>
          <p className="mt-2 text-sm text-stone-600">Find something useful from one of our marketplace vendors.</p>
          <Link className="mt-5 inline-flex rounded-sm bg-indigo-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800" to="/products">Browse products</Link>
        </section>
      ) : (
        <div className="grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_21rem]">
          <section aria-label="Cart items" className="space-y-4">
            {groupCartItems(cart.items).map((group) => <div className="overflow-hidden rounded-2xl border border-stone-200 bg-white" key={group.vendorId}>
              <h2 className="border-b border-stone-100 bg-stone-50 px-4 py-3 text-sm font-semibold text-stone-800">Sold by {group.vendorUsername}</h2>
              {group.items.map((item) => (
              <article className="grid gap-4 p-4 sm:grid-cols-[8rem_minmax(0,1fr)_auto] sm:items-center" key={item.id}>
                <Link className="block overflow-hidden bg-stone-100" to={`/products/${item.productId}`}>
                  <img alt="" className="aspect-square w-full object-cover" loading="lazy" src={item.imageUrl ?? undefined} />
                </Link>
                <div className="min-w-0">
                  <Link className="font-semibold text-stone-950 hover:text-indigo-900" to={`/products/${item.productId}`}>{item.productName}</Link>
                  <p className="mt-1 text-sm text-stone-500">{formatCurrency(item.unitPrice)} each</p>
                  <button className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-stone-500 hover:text-red-700" disabled={busyProductId === item.productId} onClick={() => void removeItem(item.productId)} type="button">
                    <Trash2 aria-hidden="true" size={14} /> Remove
                  </button>
                </div>
                <div className="flex items-center justify-between gap-5 sm:flex-col sm:items-end">
                  <div className="inline-flex items-center border border-stone-300">
                    <button aria-label={`Decrease ${item.productName} quantity`} className="p-2 text-stone-600 hover:bg-stone-100 disabled:opacity-40" disabled={item.quantity <= 1 || busyProductId === item.productId} onClick={() => void changeQuantity(item.productId, item.quantity - 1)} type="button">
                      <Minus aria-hidden="true" size={15} />
                    </button>
                    <span className="min-w-9 text-center text-sm tabular-nums" aria-live="polite">{item.quantity}</span>
                    <button aria-label={`Increase ${item.productName} quantity`} className="p-2 text-stone-600 hover:bg-stone-100 disabled:opacity-40" disabled={busyProductId === item.productId} onClick={() => void changeQuantity(item.productId, item.quantity + 1)} type="button">
                      <Plus aria-hidden="true" size={15} />
                    </button>
                  </div>
                  <span className="font-semibold tabular-nums text-stone-950">{formatCurrency(item.subtotal)}</span>
                </div>
              </article>
              ))}
            </div>)}
          </section>

          <aside className="space-y-5 border border-stone-200 bg-white p-5">
            <form className="border-b border-stone-200 pb-5" onSubmit={(event) => void applyCoupon(event)}>
              <label className="flex items-center gap-2 text-sm font-semibold text-stone-900" htmlFor="coupon-code">
                <Tag aria-hidden="true" className="text-indigo-800" size={16} /> Discount code
              </label>
              <div className="mt-3 flex">
                <input className="min-w-0 flex-1 rounded-l-sm border border-stone-300 px-3 py-2 text-sm uppercase outline-none focus:border-indigo-800" id="coupon-code" onChange={(event) => setCouponCode(event.target.value)} placeholder="Enter code" value={couponCode} />
                <button className="rounded-r-sm bg-stone-900 px-3 text-sm font-semibold text-white hover:bg-stone-700" type="submit">Apply</button>
              </div>
              {cart.couponCode && (
                <div className="mt-3 flex items-center justify-between text-xs text-indigo-800">
                  <span>{cart.couponCode} applied</span>
                  <button className="font-semibold underline underline-offset-2" onClick={() => void removeCoupon()} type="button">Remove</button>
                </div>
              )}
            </form>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between text-stone-600"><span>Subtotal</span><span className="tabular-nums">{formatCurrency(cart.subtotal)}</span></div>
              <div className="flex justify-between text-stone-600"><span>Discount</span><span className="tabular-nums">−{formatCurrency(cart.discountAmount)}</span></div>
              <div className="flex justify-between border-t border-stone-200 pt-3 text-base font-semibold text-stone-950"><span>Total</span><span className="tabular-nums">{formatCurrency(cart.totalAmount)}</span></div>
            </div>

            <div className="border-t border-stone-200 pt-5">
              <div className="mb-4 flex items-center gap-2 text-xs leading-5 text-stone-500">
                <ShieldCheck aria-hidden="true" className="shrink-0 text-indigo-800" size={17} />
                Demo Payment · No real money is charged. No card details are entered.
              </div>
              <button className="flex w-full items-center justify-center gap-2 rounded-sm bg-indigo-900 px-4 py-3 text-sm font-semibold text-white hover:bg-indigo-800 disabled:cursor-wait disabled:opacity-50" disabled={isCheckingOut} onClick={() => setShippingOpen(true)} type="button">
                {isCheckingOut && <LoaderCircle aria-hidden="true" className="animate-spin" size={16} />}
                {isCheckingOut ? 'Preparing checkout…' : 'Proceed to Checkout'}
                {!isCheckingOut && <ArrowRight aria-hidden="true" size={16} />}
              </button>
              <p className="mt-3 text-center text-xs text-stone-500">Order total: {formatCurrency(cart.totalAmount)}</p>
            </div>
          </aside>
        </div>
      )}
      {shippingOpen && <div className="fixed inset-0 z-[60] grid place-items-center bg-slate-950/45 p-4" role="presentation">
        <form aria-labelledby="shipping-title" aria-modal="true" className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl sm:p-8" onSubmit={(event) => { event.preventDefault(); setShippingOpen(false); void checkout() }} role="dialog">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-indigo-800">Checkout · Shipping</p>
          <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950" id="shipping-title">Where should we send your order?</h2>
          <p className="mt-2 text-sm text-slate-600">One address will be shared with each vendor in this multi-vendor order.</p>
          <ol aria-label="Checkout progress" className="mt-5 grid grid-cols-4 gap-2 text-center text-[11px] font-medium sm:text-xs">{['Cart','Shipping','Review','Demo payment'].map((step,index) => <li className={index === 1 ? 'text-indigo-800' : index === 0 ? 'text-slate-500' : 'text-slate-400'} key={step}><span className={`mx-auto mb-1 grid h-6 w-6 place-items-center rounded-full ${index === 1 ? 'bg-indigo-700 text-white' : index === 0 ? 'bg-indigo-100 text-indigo-800' : 'bg-slate-100'}`}>{index === 0 ? '✓' : index + 1}</span>{step}</li>)}</ol>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            {([['fullName','Full name','text'],['phone','Phone','tel'],['addressLine','Address line','text'],['city','City','text'],['state','State','text'],['postalCode','Postal code','text'],['country','Country','text']] as const).map(([key,label,type]) => <label className={`block text-sm font-medium text-slate-700 ${key === 'addressLine' ? 'sm:col-span-2' : ''}`} htmlFor={`shipping-${key}`} key={key}>{label}<input autoComplete={key === 'fullName' ? 'name' : key === 'phone' ? 'tel' : key === 'addressLine' ? 'street-address' : key} className="mt-1.5 block w-full rounded-lg border border-slate-300 px-3 py-2.5 outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10" id={`shipping-${key}`} maxLength={180} onChange={event => setShippingAddress(current => ({...current,[key]:event.target.value}))} required type={type} value={shippingAddress[key]} /></label>)}
          </div>
          <section className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-slate-50 p-4 text-sm"><div><p className="font-semibold text-slate-900">Review your order</p><p className="mt-1 text-slate-600">{cart?.items.length ?? 0} items across {new Set(cart?.items.map(item => item.vendorId)).size} stores. Demo payment follows order placement.</p></div><strong className="text-lg tabular-nums text-slate-950">{formatCurrency(cart?.totalAmount)}</strong></section>
          <div className="mt-7 flex flex-wrap justify-end gap-3">
            <button className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50" onClick={() => setShippingOpen(false)} type="button">Back to cart</button>
            <button className="rounded-lg bg-indigo-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800 disabled:opacity-50" disabled={isCheckingOut} type="submit">Place order · {formatCurrency(cart?.totalAmount)}</button>
          </div>
        </form>
      </div>}
    </div>
  )
}
