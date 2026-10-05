import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { profileApi } from '../api/services'
import type { UserProfileResponse } from '../types/api'

export function VendorStorePage() {
  const [profile, setProfile] = useState<UserProfileResponse | null>(null)
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  useEffect(() => { let active = true; profileApi.get().then((result) => { if (active) { setProfile(result); setUsername(result.username); setEmail(result.email) } }).catch(() => { if (active) setError('Store profile could not be loaded.') }); return () => { active = false } }, [])
  async function save(event: FormEvent) { event.preventDefault(); setSaving(true); setMessage(''); setError(''); try { const updated = await profileApi.update(username, email); setProfile(updated); setUsername(updated.username); setEmail(updated.email); setMessage('Profile updated.') } catch { setError('Profile could not be updated. Check the username and email and try again.') } finally { setSaving(false) } }
  return <div className="space-y-8"><header className="border-b border-stone-200 pb-6"><p className="text-xs font-semibold uppercase tracking-[0.14em] text-indigo-800">Vendor workspace</p><h1 className="mt-2 text-3xl font-semibold tracking-tight">Store profile</h1><p className="mt-2 text-sm text-stone-600">Manage the account details available to your marketplace store.</p></header>
    {error && <p className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>}{message && <p className="rounded-xl border border-indigo-200 bg-indigo-50 p-4 text-sm text-indigo-900" role="status">{message}</p>}
    {!profile && !error && <p className="rounded-xl border border-stone-200 bg-white p-6 text-sm text-stone-600" role="status">Loading profile…</p>}
    {profile && <form className="max-w-2xl space-y-5 rounded-2xl border border-stone-200 bg-white p-6" onSubmit={save}><div><label className="mb-2 block text-sm font-medium" htmlFor="store-username">Store name</label><input className="w-full rounded-lg border border-stone-300 px-3 py-2.5 text-sm outline-none focus:border-indigo-700 focus:ring-2 focus:ring-indigo-700/15" id="store-username" onChange={(event) => setUsername(event.target.value)} required value={username} /></div><div><label className="mb-2 block text-sm font-medium" htmlFor="store-email">Contact email</label><input className="w-full rounded-lg border border-stone-300 px-3 py-2.5 text-sm outline-none focus:border-indigo-700 focus:ring-2 focus:ring-indigo-700/15" id="store-email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} /></div><p className="text-xs leading-5 text-stone-500">The current account model stores username and email only. These are the profile fields available to edit.</p><button className="rounded-lg bg-indigo-900 px-4 py-2.5 text-sm font-medium text-white hover:bg-indigo-800 disabled:opacity-50" disabled={saving} type="submit">{saving ? 'Saving…' : 'Save profile'}</button></form>}
  </div>
}
