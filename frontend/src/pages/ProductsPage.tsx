import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ChevronLeft, ChevronRight, Heart, Search, SlidersHorizontal } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { productApi, wishlistApi } from '../api/services'
import type { PageQuery, ProductPageResponse } from '../types/api'
import { useAuth } from '../auth/useAuth'
import { useToast } from '../components/toastContext'
import { ProductImage } from '../components/ProductImage'
import { formatCurrency } from '../lib/currency'

const PAGE_SIZE = 12

type Filters = {
  category: string
  minPrice: string
  maxPrice: string
}

const emptyFilters: Filters = { category: '', minPrice: '', maxPrice: '' }

export function ProductsPage() {
  const [searchParams] = useSearchParams()
  const { user } = useAuth()
  const { showToast } = useToast()
  const canWishlist = Boolean(user?.roles.some((role) => role === 'ROLE_USER' || role === 'ROLE_CUSTOMER' || role === 'ROLE_VENDOR'))
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [filters, setFilters] = useState<Filters>(() => ({ ...emptyFilters, category: searchParams.get('category') ?? '' }))
  const [appliedFilters, setAppliedFilters] = useState<Filters>(() => ({ ...emptyFilters, category: searchParams.get('category') ?? '' }))
  const [sort, setSort] = useState('id-desc')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<{
    key: string
    catalog?: ProductPageResponse
    error?: string
  } | null>(null)
  const [categories, setCategories] = useState<string[]>([])
  const [wishlistIds, setWishlistIds] = useState<Set<number>>(() => new Set())
  const [pendingWishlistIds, setPendingWishlistIds] = useState<Set<number>>(() => new Set())
  const [wishlistLoaded, setWishlistLoaded] = useState(false)

  useEffect(() => {
    let active = true
    productApi.browse({ page: 0, size: 100, sortBy: 'category', direction: 'asc' })
      .then((result) => {
        if (active) setCategories([...new Set(result.content.map((product) => product.category))].sort())
      })
      .catch(() => {
        if (active) setCategories([])
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!canWishlist) return
    let active = true
    wishlistApi.get()
      .then((wishlist) => {
        if (active) setWishlistIds(new Set(wishlist.items.map((item) => item.productId)))
      })
      .catch(() => {
        if (active) showToast('Your wishlist could not be loaded.', 'error')
      })
      .finally(() => {
        if (active) setWishlistLoaded(true)
      })
    return () => { active = false }
  }, [canWishlist, showToast])
  const [sortBy, direction] = sort.split('-') as [NonNullable<PageQuery['sortBy']>, 'asc' | 'desc']
  const queryKey = JSON.stringify({
    page,
    size: PAGE_SIZE,
    sortBy,
    direction,
    search,
    category: appliedFilters.category,
    minPrice: appliedFilters.minPrice,
    maxPrice: appliedFilters.maxPrice,
  })
  const currentResult = result?.key === queryKey ? result : null
  const catalog = currentResult?.catalog ?? null
  const error = currentResult?.error ?? ''
  const isLoading = currentResult === null

  useEffect(() => {
    let active = true
    const params = JSON.parse(queryKey) as {
      page: number
      size: number
      sortBy: NonNullable<PageQuery['sortBy']>
      direction: 'asc' | 'desc'
      search: string
      category: string
      minPrice: string
      maxPrice: string
    }
    const query: PageQuery = {
      ...params,
      search: params.search || undefined,
      category: params.category || undefined,
      minPrice: params.minPrice ? Number(params.minPrice) : undefined,
      maxPrice: params.maxPrice ? Number(params.maxPrice) : undefined,
    }
    productApi.browse(query)
      .then((result) => {
        if (active) setResult({ key: queryKey, catalog: result })
      })
      .catch(() => {
        if (active) setResult({ key: queryKey, error: 'Products could not be loaded. Check the backend connection and try again.' })
      })

    return () => { active = false }
  }, [queryKey])

  async function toggleWishlist(productId: number) {
    if (!canWishlist || pendingWishlistIds.has(productId)) return
    const isSaved = wishlistIds.has(productId)
    setPendingWishlistIds((current) => new Set(current).add(productId))
    try {
      const wishlist = isSaved
        ? await wishlistApi.removeItem(productId)
        : await wishlistApi.addItem(productId)
      setWishlistIds(new Set(wishlist.items.map((item) => item.productId)))
      showToast(isSaved ? 'Removed from your wishlist.' : 'Added to your wishlist.')
    } catch {
      showToast('Wishlist could not be updated. Please try again.', 'error')
    } finally {
      setPendingWishlistIds((current) => {
        const next = new Set(current)
        next.delete(productId)
        return next
      })
    }
  }
  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    setSearch(searchDraft.trim())
  }

  function submitFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    setAppliedFilters(filters)
  }

  function clearFilters() {
    setFilters(emptyFilters)
    setAppliedFilters(emptyFilters)
    setSearchDraft('')
    setSearch('')
    setPage(0)
  }

  return (
    <div className="space-y-8">
        <div className="flex flex-wrap items-end justify-between gap-5 border-b border-stone-300 pb-6">
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">The marketplace</p>
          <h1 className="text-3xl font-semibold tracking-tight text-stone-950">Find your next favorite</h1>
          <p className="mt-2 text-sm text-stone-600">Search independent vendors and compare whatâ€™s in stock.</p>
        </div>
        <form className="flex w-full max-w-lg" onSubmit={submitSearch} role="search">
          <label className="sr-only" htmlFor="product-search">Search products</label>
          <input
            className="min-w-0 flex-1 rounded-l-sm border border-stone-300 bg-white px-4 py-3 text-sm outline-none focus:border-indigo-800 focus:ring-2 focus:ring-indigo-800/15"
            id="product-search"
            onChange={(event) => setSearchDraft(event.target.value)}
            placeholder="Search products, categories, details"
            type="search"
            value={searchDraft}
          />
          <button aria-label="Search" className="rounded-r-sm bg-indigo-900 px-4 text-white hover:bg-indigo-800" type="submit">
            <Search aria-hidden="true" size={18} />
          </button>
        </form>
      </div>

      <div className="grid gap-8 lg:grid-cols-[15rem_minmax(0,1fr)]">
        <aside className="h-fit border border-stone-200 bg-white p-5">
          <div className="mb-5 flex items-center gap-2 border-b border-stone-200 pb-4">
            <SlidersHorizontal aria-hidden="true" className="text-indigo-800" size={17} />
            <h2 className="text-sm font-semibold">Filter products</h2>
          </div>
          <form className="space-y-5" onSubmit={submitFilters}>
            <label className="block text-xs font-semibold uppercase tracking-wide text-stone-600" htmlFor="category-filter">
              Category
              <select
                className="mt-2 block w-full rounded-sm border border-stone-300 bg-white px-3 py-2.5 text-sm font-normal normal-case tracking-normal text-stone-900"
                id="category-filter"
                onChange={(event) => setFilters({ ...filters, category: event.target.value })}
                value={filters.category}
              >
                <option value="">All categories</option>
                {categories.map((category) => <option key={category} value={category}>{category}</option>)}
              </select>
            </label>
            <fieldset>
              <legend className="text-xs font-semibold uppercase tracking-wide text-stone-600">Price range</legend>
              <div className="mt-2 grid grid-cols-2 gap-2">
                <label className="text-xs text-stone-500" htmlFor="min-price">
                  Min
                  <input
                    className="mt-1 w-full rounded-sm border border-stone-300 px-2.5 py-2 text-sm text-stone-900"
                    id="min-price"
                    min="0"
                    onChange={(event) => setFilters({ ...filters, minPrice: event.target.value })}
                    placeholder="₹0"
                    type="number"
                    value={filters.minPrice}
                  />
                </label>
                <label className="text-xs text-stone-500" htmlFor="max-price">
                  Max
                  <input
                    className="mt-1 w-full rounded-sm border border-stone-300 px-2.5 py-2 text-sm text-stone-900"
                    id="max-price"
                    min="0"
                    onChange={(event) => setFilters({ ...filters, maxPrice: event.target.value })}
                    placeholder="No limit"
                    type="number"
                    value={filters.maxPrice}
                  />
                </label>
              </div>
            </fieldset>
            <label className="block text-xs font-semibold uppercase tracking-wide text-stone-600" htmlFor="product-sort">
              Sort by
              <select
                className="mt-2 block w-full rounded-sm border border-stone-300 bg-white px-3 py-2.5 text-sm font-normal normal-case tracking-normal text-stone-900"
                id="product-sort"
                onChange={(event) => { setSort(event.target.value); setPage(0) }}
                value={sort}
              >
                <option value="id-desc">Newest</option>
                <option value="price-asc">Price: low to high</option>
                <option value="price-desc">Price: high to low</option>
                <option value="name-asc">Name: A to Z</option>
              </select>
            </label>
            <button className="w-full rounded-sm bg-indigo-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800" type="submit">
              Apply filters
            </button>
            <button className="w-full py-2 text-sm font-medium text-stone-600 hover:text-stone-950" onClick={clearFilters} type="button">
              Clear all
            </button>
          </form>
        </aside>

        <section aria-label="Product results">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-stone-600" aria-live="polite">
              {isLoading ? 'Loading products...' : `${catalog?.totalElements ?? 0} products`}
              {search && <span> matching "{search}"</span>}
            </p>
            {catalog && catalog.totalPages > 0 && <span className="text-xs text-stone-500">Page {catalog.page + 1} of {catalog.totalPages}</span>}
          </div>

          {error && <p className="border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>}
          {!isLoading && !error && catalog?.content.length === 0 && (
            <div className="border border-dashed border-stone-300 bg-white p-10 text-center">
              <p className="font-medium text-stone-900">No products match these filters.</p>
              <button className="mt-3 text-sm font-semibold text-indigo-900 underline underline-offset-4" onClick={clearFilters} type="button">Clear filters</button>
            </div>
          )}
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {catalog?.content.map((product) => {
              const isSaved = wishlistIds.has(product.id)
              const isPending = pendingWishlistIds.has(product.id)
              return (
                <article className="group min-w-0 overflow-hidden rounded-2xl border border-stone-200 bg-white transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl" key={product.id}>
                  <div className="relative aspect-square overflow-hidden bg-stone-100">
                    <Link aria-label={`View ${product.name}`} className="block h-full w-full" to={`/products/${product.id}`}>
                      <ProductImage alt={product.name} className="h-full w-full transition-transform duration-500 group-hover:scale-105" src={product.imageUrl} />
                    </Link>
                    <span className="absolute left-4 top-4 max-w-[70%] truncate rounded-full border border-white/70 bg-white/90 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-stone-700 shadow-sm backdrop-blur">
                      {product.category}
                    </span>
                    <span className={`absolute bottom-4 left-4 rounded-full px-3 py-1.5 text-xs font-semibold shadow-sm backdrop-blur ${product.stock > 0 ? 'bg-indigo-50/95 text-indigo-800' : 'bg-stone-900/85 text-white'}`}>
                      {product.stock > 0 ? (product.stock < 5 ? `Only ${product.stock} left` : 'In stock') : 'Sold out'}
                    </span>
                    {canWishlist && (
                      <button
                        aria-label={isSaved ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`}
                        aria-pressed={isSaved}
                        className="absolute right-4 top-4 inline-flex h-10 w-10 items-center justify-center rounded-full border border-white/70 bg-white/90 text-stone-600 shadow-sm backdrop-blur transition hover:scale-105 hover:text-rose-600 focus:outline-none focus:ring-2 focus:ring-indigo-700 focus:ring-offset-2 disabled:cursor-wait disabled:opacity-60"
                        disabled={isPending || !wishlistLoaded}
                        onClick={() => void toggleWishlist(product.id)}
                        type="button"
                      >
                        <Heart aria-hidden="true" className={isSaved ? 'fill-rose-500 text-rose-500' : ''} size={18} />
                      </button>
                    )}
                  </div>
                  <div className="space-y-3 p-5">
                    <Link className="block truncate text-base font-semibold tracking-tight text-stone-950 transition-colors hover:text-indigo-800" to={`/products/${product.id}`}>
                      {product.name}
                    </Link>
                    <div className="flex items-end justify-between gap-3">
                        <span className="text-xl font-bold tracking-tight text-indigo-800">{formatCurrency(product.price)}</span>
                      <span className="max-w-[55%] truncate rounded-full bg-stone-100 px-3 py-1 text-xs font-medium text-stone-600" title={product.vendorUsername}>
                        by {product.vendorUsername}
                      </span>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
          {catalog && catalog.totalPages > 1 && (
            <nav aria-label="Product pages" className="mt-8 flex items-center justify-between border-t border-stone-200 pt-4">
              <button
                className="inline-flex items-center gap-1 rounded-sm border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 hover:bg-stone-50 disabled:opacity-40"
                disabled={catalog.first || isLoading}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                type="button"
              >
                <ChevronLeft aria-hidden="true" size={16} /> Previous
              </button>
              <span className="text-sm tabular-nums text-stone-600">{catalog.page + 1} / {catalog.totalPages}</span>
              <button
                className="inline-flex items-center gap-1 rounded-sm border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 hover:bg-stone-50 disabled:opacity-40"
                disabled={catalog.last || isLoading}
                onClick={() => setPage((current) => Math.min(catalog.totalPages - 1, current + 1))}
                type="button"
              >
                Next <ChevronRight aria-hidden="true" size={16} />
              </button>
            </nav>
          )}
        </section>
      </div>
    </div>
  )
}


