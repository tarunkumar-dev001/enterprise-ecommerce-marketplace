import { useState } from 'react'
import type { FormEvent } from 'react'
import axios from 'axios'
import { LoaderCircle, Star } from 'lucide-react'
import { Link } from 'react-router-dom'
import { productApi } from '../api/services'
import { useToast } from './toastContext'
import type { ProductReviewResponse, UserProfileResponse } from '../types/api'

type ReviewSectionProps = {
  productId: number
  averageRating: number
  reviewCount: number
  reviews: ProductReviewResponse[]
  user: UserProfileResponse | null
  onReviewAdded: (review: ProductReviewResponse) => void
}

function reviewError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const body: unknown = error.response?.data
    if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
      return body.message
    }
  }
  return 'Your review could not be submitted. Please try again.'
}

export function ReviewSection({
  productId,
  averageRating,
  reviewCount,
  reviews,
  user,
  onReviewAdded,
}: ReviewSectionProps) {
  const { showToast } = useToast()
  const [rating, setRating] = useState(0)
  const [text, setText] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const canReview = Boolean(user?.roles.some((role) => role === 'ROLE_USER' || role === 'ROLE_CUSTOMER' || role === 'ROLE_VENDOR'))
  const hasReviewed = Boolean(user && reviews.some((review) => review.customerId === user.id))

  async function submitReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (rating < 1) {
      showToast('Choose a star rating before submitting.', 'error')
      return
    }

    setIsSubmitting(true)
    try {
      const review = await productApi.addReview(productId, rating, text.trim())
      onReviewAdded(review)
      setRating(0)
      setText('')
      showToast('Your review has been submitted.')
    } catch (error) {
      showToast(reviewError(error), 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="space-y-6 border-t border-stone-300 pt-8" id="reviews">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-800">Customer feedback</p>
          <h2 className="mt-1 text-xl font-semibold text-stone-950">Reviews</h2>
        </div>
        <div className="text-right">
          <div aria-label={`Average rating ${averageRating.toFixed(1)} out of 5`} className="flex items-center justify-end gap-1 text-amber-600">
            {Array.from({ length: 5 }, (_, index) => (
              <Star aria-hidden="true" className={index < Math.round(averageRating) ? 'fill-current' : ''} key={index} size={15} />
            ))}
            <span className="ml-1 text-sm font-semibold text-stone-900">{averageRating.toFixed(1)}</span>
          </div>
          <span className="text-xs text-stone-500">{reviewCount} {reviewCount === 1 ? 'review' : 'reviews'}</span>
        </div>
      </div>

      {canReview && !hasReviewed && (
        <form className="space-y-4 border border-stone-200 bg-white p-5" onSubmit={(event) => void submitReview(event)}>
          <h3 className="font-semibold text-stone-950">Write a review</h3>
          <fieldset>
            <legend className="text-sm font-medium text-stone-700">Your rating</legend>
            <div aria-label="Choose a rating from one to five stars" className="mt-2 flex gap-1" role="group">
              {Array.from({ length: 5 }, (_, index) => {
                const value = index + 1
                return (
                  <button
                    aria-label={`${value} ${value === 1 ? 'star' : 'stars'}`}
                    aria-pressed={rating === value}
                    className="rounded-sm p-1 text-amber-600 transition hover:scale-110 focus:outline-none focus:ring-2 focus:ring-indigo-700"
                    key={value}
                    onClick={() => setRating(value)}
                    type="button"
                  >
                    <Star aria-hidden="true" className={value <= rating ? 'fill-current' : ''} size={23} />
                  </button>
                )
              })}
            </div>
          </fieldset>
          <label className="block text-sm font-medium text-stone-700" htmlFor={`review-text-${productId}`}>
            Your review
            <textarea
              className="mt-1.5 min-h-28 w-full rounded-sm border border-stone-300 px-3 py-2.5 text-sm font-normal text-stone-900 outline-none focus:border-indigo-800"
              id={`review-text-${productId}`}
              maxLength={2000}
              onChange={(event) => setText(event.target.value)}
              required
              value={text}
            />
          </label>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <span className="text-xs text-stone-500">Reviews are available to verified purchasers.</span>
            <button
              className="inline-flex items-center gap-2 rounded-sm bg-indigo-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800 disabled:cursor-wait disabled:opacity-50"
              disabled={isSubmitting || !text.trim() || rating === 0}
              type="submit"
            >
              {isSubmitting && <LoaderCircle aria-hidden="true" className="animate-spin" size={16} />}
              {isSubmitting ? 'Submitting…' : 'Submit review'}
            </button>
          </div>
        </form>
      )}

      {hasReviewed && (
        <p className="border border-indigo-200 bg-indigo-50 p-4 text-sm text-indigo-900" role="status">
          You have already reviewed this product. Thank you for your feedback.
        </p>
      )}
      {!user && (
        <p className="text-sm text-stone-600">
          <Link className="font-medium text-indigo-900 underline underline-offset-4" to="/login">Sign in</Link> to write a review after purchasing this product.
        </p>
      )}

      {reviews.length === 0 ? (
        <p className="border border-dashed border-stone-300 bg-white p-6 text-sm text-stone-600">No reviews yet. Be the first to share your experience.</p>
      ) : (
        <ul className="divide-y divide-stone-200 border-y border-stone-200 bg-white">
          {reviews.map((review) => (
            <li className="grid gap-2 p-5 sm:grid-cols-[10rem_minmax(0,1fr)]" key={review.id}>
              <div>
                <p className="text-sm font-semibold text-stone-900">{review.customerUsername}</p>
                <time className="mt-1 block text-xs text-stone-500" dateTime={review.createdAt}>
                  {new Date(review.createdAt).toLocaleDateString()}
                </time>
              </div>
              <div>
                <div aria-label={`Rated ${review.rating} out of 5`} className="flex gap-0.5 text-amber-600">
                  {Array.from({ length: 5 }, (_, index) => (
                    <Star aria-hidden="true" className={index < review.rating ? 'fill-current' : ''} key={index} size={15} />
                  ))}
                </div>
                <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-stone-700">{review.text}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
