import { useState } from 'react'
import type { FormEvent } from 'react'
import axios from 'axios'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '../api/services'
import { useAuth } from '../auth/useAuth'

function responseMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const body: unknown = error.response?.data
    if (typeof body === 'string' && body.trim()) return body
    if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
      return body.message
    }
  }
  return 'We could not complete that request. Check your details and try again.'
}

function AuthFrame({ title, description, children }: {
  title: string
  description: string
  children: React.ReactNode
}) {
  return (
    <section className="mx-auto grid max-w-4xl overflow-hidden border border-stone-200 bg-white md:grid-cols-[0.8fr_1.2fr]">
      <div className="flex min-h-56 flex-col justify-between bg-indigo-950 p-7 text-indigo-50 md:p-9">
        <span className="text-xs font-semibold uppercase tracking-[0.12em] text-amber-300">Market / Place</span>
        <div>
          <p className="text-2xl font-semibold leading-tight">A better way to bring good finds home.</p>
          <p className="mt-3 text-sm leading-6 text-indigo-100/75">One account for shopping, orders, and seller tools.</p>
        </div>
        <span className="text-xs text-indigo-100/60">Marketplace account</span>
      </div>
      <div className="p-7 md:p-10">
        <h1 className="text-2xl font-semibold tracking-tight text-stone-950">{title}</h1>
        <p className="mt-2 text-sm leading-6 text-stone-600">{description}</p>
        <div className="mt-7">{children}</div>
      </div>
    </section>
  )
}

function Field({ label, name, type = 'text', autoComplete, required = true }: {
  label: string
  name: string
  type?: string
  autoComplete?: string
  required?: boolean
}) {
  return (
    <label className="block text-sm font-medium text-stone-700" htmlFor={name}>
      {label}
      <input
        className="mt-2 block w-full rounded-sm border border-stone-300 bg-white px-3 py-2.5 text-stone-950 outline-none transition focus:border-indigo-800 focus:ring-2 focus:ring-indigo-800/15"
        id={name}
        name={name}
        type={type}
        autoComplete={autoComplete}
        required={required}
      />
    </label>
  )
}

export function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const state = location.state as { from?: { pathname?: string }; message?: string } | null

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    const form = new FormData(event.currentTarget)
    try {
      const user = await signIn(String(form.get('username')), String(form.get('password')))
      const fallback = user.roles.includes('ROLE_ADMIN') ? '/admin' : user.roles.includes('ROLE_VENDOR') ? '/vendor' : '/'
      navigate(state?.from?.pathname || fallback, { replace: true })
    } catch (requestError) {
      setError(responseMessage(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthFrame title="Welcome back" description="Sign in to continue to your marketplace account.">
      {state?.message && <p className="mb-4 text-sm text-indigo-800" role="status">{state.message}</p>}
      <form className="space-y-5" onSubmit={handleSubmit}>
        <Field label="Username" name="username" autoComplete="username" />
        <Field label="Password" name="password" type="password" autoComplete="current-password" />
        {error && <p className="text-sm text-red-700" role="alert">{error}</p>}
        <button
          className="w-full rounded-sm bg-indigo-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-indigo-800 disabled:cursor-wait disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="mt-6 text-sm text-stone-600">
        New to the marketplace? <Link className="font-semibold text-indigo-900 underline decoration-indigo-300 underline-offset-4" to="/register">Create an account</Link>
      </p>
    </AuthFrame>
  )
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    const form = new FormData(event.currentTarget)
    const username = String(form.get('username')).trim()
    const email = String(form.get('email')).trim()
    const password = String(form.get('password'))
    const accountType = String(form.get('accountType')) as 'CUSTOMER' | 'VENDOR'
    const confirmation = String(form.get('confirmPassword'))
    if (password !== confirmation) {
      setError('The passwords do not match.')
      setIsSubmitting(false)
      return
    }

    try {
      const message = await authApi.register(username, email, password, accountType)
      navigate('/login', { replace: true, state: { message } })
    } catch (requestError) {
      setError(responseMessage(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthFrame title="Create your account" description="Join the marketplace as a customer or independent seller.">
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Field label="Username" name="username" autoComplete="username" />
        <Field label="Email address" name="email" type="email" autoComplete="email" />
        <label className="block text-sm font-medium text-stone-700" htmlFor="account-type">
          Account type
          <select
            className="mt-2 block w-full rounded-sm border border-stone-300 bg-white px-3 py-2.5 text-stone-950 outline-none transition focus:border-indigo-800 focus:ring-2 focus:ring-indigo-800/15"
            id="account-type"
            name="accountType"
            defaultValue="CUSTOMER"
          >
            <option value="CUSTOMER">Customer</option>
            <option value="VENDOR">Vendor / seller</option>
          </select>
        </label>
        <Field label="Password" name="password" type="password" autoComplete="new-password" />
        <Field label="Confirm password" name="confirmPassword" type="password" autoComplete="new-password" />
        {error && <p className="text-sm text-red-700" role="alert">{error}</p>}
        <button
          className="w-full rounded-sm bg-indigo-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-indigo-800 disabled:cursor-wait disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="mt-6 text-sm text-stone-600">
        Already registered? <Link className="font-semibold text-indigo-900 underline decoration-indigo-300 underline-offset-4" to="/login">Sign in</Link>
      </p>
    </AuthFrame>
  )
}
