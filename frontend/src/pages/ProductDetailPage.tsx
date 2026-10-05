import { useEffect, useState } from 'react'
import { ArrowLeft, ShoppingCart, Star } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { cartApi, productApi } from '../api/services'
import { useAuth } from '../auth/useAuth'
import { ReviewSection } from '../components/ReviewSection'
import { ProductImage } from '../components/ProductImage'
import { useToast } from '../components/toastContext'
import type { ProductDetailsResponse, ProductReviewResponse } from '../types/api'
import { formatCurrency } from '../lib/currency'

export function ProductDetailPage() {
  const { productId } = useParams()
  const { user } = useAuth()
  const { showToast } = useToast()
  const [result, setResult] = useState<{
    id: number
    product?: ProductDetailsResponse
    reviews?: ProductReviewResponse[]
    error?: string
  } | null>(null)
  const [isAdding, setIsAdding] = useState(false)
  const id = Number(productId)
  const invalidProductId = !Number.isSafeInteger(id) || id < 1
  const currentResult = result?.id === id ? result : null
  const product = currentResult?.product ?? null
  const reviews = currentResult?.reviews ?? []
  const error = currentResult?.error ?? ''
  const isLoading = !invalidProductId && currentResult === null
  const canAddToCart = Boolean(user?.roles.some((role) => role === 'ROLE_USER' || role === 'ROLE_CUSTOMER' || role === 'ROLE_VENDOR'))

  useEffect(() => {
    let active = true
    if (invalidProductId) {
      return () => { active = false }
    }

    Promise.all([productApi.details(id), productApi.reviews(id)])
      .then(([details, productReviews]) => {
        if (!active) return
        setResult({ id, product: details, reviews: productReviews })
      })
      .catch(() => {
        if (active) setResult({ id, error: 'Product details could not be loaded.' })
      })

    return () => { active = false }
  }, [id, invalidProductId])

  async function addToCart() {
    if (!product) return
    setIsAdding(true)
    try {
      await cartApi.addItem(product.id, 1)
      showToast(`${product.name} added to your cart.`)
    } catch {
      showToast('Could not add this item. It may be out of stock; please try again.', 'error')
    } finally {
      setIsAdding(false)
    }
  }

  function addReview(review: ProductReviewResponse) {
    setResult((current) => {
      if (!current?.product || current.id !== id) return current
      const previousCount = current.product.reviewCount
      const reviewCount = previousCount + 1
      const averageRating = ((current.product.averageRating * previousCount) + review.rating) / reviewCount
      return {
        ...current,
        product: { ...current.product, averageRating, reviewCount },
        reviews: [review, ...(current.reviews ?? [])],
      }
    })
  }

  if (isLoading) {
    return <div className="py-16 text-sm text-stone-600" role="status">Loading productâ€¦</div>
  }

  if (invalidProductId) {
    return <div className="space-y-4"><p className="text-sm text-red-800" role="alert">Product not found.</p><Link className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-900" to="/products"><ArrowLeft size={16} /> Back to products</Link></div>
  }

  if (error && !product) {
    return <div className="space-y-4"><p className="text-sm text-red-800" role="alert">{error}</p><Link className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-900" to="/products"><ArrowLeft size={16} /> Back to products</Link></div>
  }

  if (!product) return null

  return (
    <div className="space-y-12">
      <Link className="inline-flex items-center gap-2 text-sm font-medium text-stone-600 hover:text-indigo-900" to="/products">
        <ArrowLeft aria-hidden="true" size={16} /> Back to products
      </Link>
      <div className="grid gap-8 lg:grid-cols-2 lg:gap-12">
        <ProductImage alt={product.name} className="aspect-square" imageClassName="object-contain" loading="eager" src={product.imageUrl} />
        <section className="flex flex-col justify-center">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">{product.category}</p>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight text-stone-950">{product.name}</h1>
          <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2">
            <div aria-label={`Rated ${product.averageRating.toFixed(1)} out of 5`} className="inline-flex items-center gap-1 text-amber-600">
              {Array.from({ length: 5 }, (_, index) => (
                <Star aria-hidden="true" className={index < Math.round(product.averageRating) ? 'fill-current' : ''} key={index} size={17} />
              ))}
              <span className="ml-1 text-sm font-semibold text-stone-900">{product.averageRating.toFixed(1)}</span>
            </div>
            <a className="text-sm text-stone-500 underline underline-offset-4 hover:text-indigo-900" href="#reviews">
              {product.reviewCount} {product.reviewCount === 1 ? 'review' : 'reviews'}
            </a>
          </div>
          <p className="mt-5 text-2xl font-semibold tabular-nums text-stone-950">{formatCurrency(product.price)}</p>
          <p className="mt-5 max-w-xl leading-7 text-stone-600">{product.description || 'A quality find from one of our marketplace vendors.'}</p>
          <p className="mt-4 text-sm text-stone-600">Sold by <span className="font-medium text-stone-900">{product.vendorUsername}</span></p>
          <p className="mt-1 text-sm text-stone-600">{product.stock > 0 ? `${product.stock} available` : 'Currently out of stock'}</p>
          {canAddToCart && (
            <button
              className="mt-7 inline-flex w-fit items-center justify-center gap-2 rounded-sm bg-indigo-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-indigo-800 disabled:cursor-not-allowed disabled:opacity-50"
              disabled={product.stock < 1 || isAdding}
              onClick={addToCart}
              type="button"
            >
              <ShoppingCart aria-hidden="true" size={17} />
              {isAdding ? 'Addingâ€¦' : 'Add to cart'}
            </button>
          )}
        </section>
      </div>

      <ReviewSection
        averageRating={product.averageRating}
        onReviewAdded={addReview}
        productId={product.id}
        reviewCount={product.reviewCount}
        reviews={reviews}
        user={user}
      />
    </div>
  )
}
