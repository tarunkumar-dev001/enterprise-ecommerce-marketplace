import { useEffect, useState } from 'react'
import { Star } from 'lucide-react'
import { vendorApi } from '../api/services'
import type { ProductReviewResponse } from '../types/api'

export function VendorReviewsPage() {
  const [reviews, setReviews] = useState<ProductReviewResponse[] | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { let active = true; vendorApi.reviews().then((data) => { if (active) setReviews(data) }).catch(() => { if (active) setError('Product reviews could not be loaded.') }); return () => { active = false } }, [])
  const average = reviews?.length ? reviews.reduce((sum, item) => sum + item.rating, 0) / reviews.length : 0
  return <div className="space-y-8"><header className="border-b border-stone-200 pb-6"><p className="text-xs font-semibold uppercase tracking-[0.14em] text-indigo-800">Vendor workspace</p><h1 className="mt-2 text-3xl font-semibold tracking-tight">Product reviews</h1><p className="mt-2 text-sm text-stone-600">Customer feedback on products in your catalog.</p></header>
    {error && <p className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-800" role="alert">{error}</p>}{reviews === null && !error && <p className="rounded-xl border border-stone-200 bg-white p-6 text-sm text-stone-600" role="status">Loading reviews…</p>}
    {reviews && <><section className="flex flex-wrap items-center gap-5 rounded-2xl border border-stone-200 bg-white p-6"><div className="flex items-center gap-2 text-amber-600"><Star fill="currentColor" size={22} /><span className="text-3xl font-semibold tabular-nums text-stone-950">{reviews.length ? average.toFixed(1) : '—'}</span></div><div><p className="font-semibold">Average rating</p><p className="mt-1 text-sm text-stone-500">{reviews.length} {reviews.length === 1 ? 'review' : 'reviews'} across your products</p></div></section>
      {!reviews.length ? <p className="rounded-xl border border-dashed border-stone-300 bg-white p-8 text-sm text-stone-500">Reviews for your products will appear here.</p> : <div className="space-y-3">{reviews.map((review) => <article className="rounded-2xl border border-stone-200 bg-white p-5" key={review.id}><div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="font-semibold">{review.productName}</h2><p className="mt-1 text-xs text-stone-500">by {review.customerUsername} · {new Date(review.createdAt).toLocaleDateString()}</p></div><span aria-label={`${review.rating} out of 5 stars`} className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-3 py-1 text-sm font-semibold text-amber-800"><Star fill="currentColor" size={14} />{review.rating}/5</span></div><p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-stone-700">{review.text}</p></article>)}</div>}</>}
  </div>
}
