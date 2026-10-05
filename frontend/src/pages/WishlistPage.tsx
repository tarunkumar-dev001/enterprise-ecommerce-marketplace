import { useEffect, useState } from 'react'
import { ArrowRightLeft, Heart, ShoppingCart, Trash2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { wishlistApi } from '../api/services'
import type { WishlistResponse } from '../types/api'
import { useToast } from '../components/toastContext'
import { formatCurrency } from '../lib/currency'

export function WishlistPage() {
  const { showToast } = useToast()
  const [wishlist, setWishlist] = useState<WishlistResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [busyProductId, setBusyProductId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    wishlistApi.get()
      .then((result) => { if (active) setWishlist(result) })
      .catch(() => { if (active) setError('Your wishlist could not be loaded. Try again.') })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [])

  async function removeItem(productId: number) {
    setBusyProductId(productId)
    setError('')
    try {
      setWishlist(await wishlistApi.removeItem(productId))
      showToast('Item removed from your wishlist.')
    } catch {
      showToast('This item could not be removed. Refresh and try again.', 'error')
    } finally {
      setBusyProductId(null)
    }
  }

  async function moveToCart(productId: number, productName: string) {
    setBusyProductId(productId)
    setError('')
    try {
      await wishlistApi.moveItemToCart(productId)
      setWishlist(await wishlistApi.get())
      showToast(`${productName} moved to your cart.`)
    } catch {
      showToast('This item could not be moved. It may be out of stock, or your cart may have changed.', 'error')
    } finally {
      setBusyProductId(null)
    }
  }

  if (isLoading) {
    return <div className="py-16 text-sm text-stone-600" role="status">Loading your wishlist…</div>
  }

  return (
    <div className="space-y-7">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-stone-300 pb-5">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">Customer</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-stone-950">Your wishlist</h1>
          <p className="mt-2 text-sm text-stone-600">Keep a list of products you’d like to come back to.</p>
        </div>
        <Link className="text-sm font-semibold text-indigo-900 underline decoration-indigo-300 underline-offset-4" to="/products">Browse products</Link>
      </header>

      {error && <p className="border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>}

      {!error && wishlist?.items.length === 0 && (
        <section className="border border-dashed border-stone-300 bg-white p-10 text-center">
          <Heart aria-hidden="true" className="mx-auto text-indigo-800" size={25} />
          <h2 className="mt-4 font-semibold text-stone-950">Your wishlist is empty</h2>
          <p className="mt-2 text-sm text-stone-600">Save products from the catalog and they’ll be collected here.</p>
          <Link className="mt-5 inline-flex rounded-sm bg-indigo-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800" to="/products">Explore products</Link>
        </section>
      )}

      <div className="divide-y divide-stone-200 border-y border-stone-200 bg-white">
        {wishlist?.items.map((item) => (
          <article className="grid gap-4 p-4 sm:grid-cols-[7rem_minmax(0,1fr)_auto] sm:items-center" key={item.productId}>
            <Link className="block overflow-hidden bg-stone-100" to={`/products/${item.productId}`}>
              <img alt="" className="aspect-square w-full object-cover" loading="lazy" src={item.imageUrl ?? undefined} />
            </Link>
            <div className="min-w-0">
              <p className="text-xs font-medium uppercase tracking-wide text-indigo-800">{item.category}</p>
              <Link className="mt-1 block truncate font-semibold text-stone-950 hover:text-indigo-900" to={`/products/${item.productId}`}>{item.productName}</Link>
              <p className="mt-1 text-xs text-stone-500">Sold by {item.vendorUsername}</p>
              <p className="mt-2 text-base font-semibold tabular-nums text-stone-950">{formatCurrency(item.price)}</p>
            </div>
            <div className="flex flex-wrap items-center gap-2 sm:justify-end">
              <button
                className="inline-flex items-center gap-2 rounded-sm bg-indigo-900 px-3 py-2.5 text-xs font-semibold text-white hover:bg-indigo-800 disabled:cursor-not-allowed disabled:opacity-50"
                disabled={busyProductId === item.productId || item.stock < 1}
                onClick={() => void moveToCart(item.productId, item.productName)}
                title={item.stock < 1 ? 'Currently out of stock' : 'Move item to cart'}
                type="button"
              >
                <ArrowRightLeft aria-hidden="true" size={15} />
                {busyProductId === item.productId ? 'Moving…' : 'Move to cart'}
              </button>
              <button
                aria-label={`Remove ${item.productName} from wishlist`}
                className="rounded-sm border border-stone-300 p-2.5 text-stone-600 hover:border-red-300 hover:text-red-700 disabled:opacity-50"
                disabled={busyProductId === item.productId}
                onClick={() => void removeItem(item.productId)}
                title="Remove from wishlist"
                type="button"
              >
                <Trash2 aria-hidden="true" size={16} />
              </button>
              {item.stock < 1 && <span className="basis-full text-right text-xs text-stone-500">Out of stock</span>}
            </div>
          </article>
        ))}
      </div>
      <Link className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-900 hover:text-indigo-700" to="/cart">
        <ShoppingCart aria-hidden="true" size={16} /> Go to cart
      </Link>
    </div>
  )
}
