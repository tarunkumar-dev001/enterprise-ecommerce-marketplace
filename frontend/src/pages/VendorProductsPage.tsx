import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import axios from 'axios'
import { Check, Pencil, Plus, X } from 'lucide-react'
import { productApi, vendorApi } from '../api/services'
import type { ProductRequest, VendorProductResponse } from '../types/api'
import { useToast } from '../components/toastContext'
import { ProductImage } from '../components/ProductImage'
import { formatCurrency } from '../lib/currency'

const emptyForm: ProductRequest = { name: '', description: '', price: 0, stock: 0, category: '', imageUrl: '' }
const currency = { format: formatCurrency }

function responseError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const body: unknown = error.response?.data
    if (typeof body === 'string' && body) return body
    if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') return body.message
  }
  return 'Product request failed. Check the fields and try again.'
}

export function VendorProductsPage() {
  const { showToast } = useToast()
  const [products, setProducts] = useState<VendorProductResponse[] | null>(null)
  const [form, setForm] = useState<ProductRequest>(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    let active = true
    vendorApi.listProducts()
      .then((result) => { if (active) setProducts(result) })
      .catch(() => { if (active) setError('Your inventory could not be loaded.') })
    return () => { active = false }
  }, [])

  function editProduct(product: VendorProductResponse) {
    setEditingId(product.id)
    setForm({
      name: product.name,
      description: product.description ?? '',
      price: product.price,
      stock: product.stock,
      category: product.category,
      imageUrl: product.imageUrl ?? "",
    })
    setError('')
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptyForm)
    setError('')
  }

  async function submitProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setIsSaving(true)
    try {
      if (editingId === null) {
        await productApi.create(form)
        showToast('Product created.')
      } else {
        await productApi.update(editingId, form)
        showToast('Product updated.')
      }
      setProducts(await vendorApi.listProducts())
      setEditingId(null)
      setForm(emptyForm)
    } catch (requestError) {
      showToast(responseError(requestError), 'error')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-slate-300 pb-5">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-700">Vendor workspace</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">Product inventory</h1>
          <p className="mt-2 text-sm text-slate-600">Create listings and keep pricing and stock current.</p>
        </div>
        <span className="text-sm tabular-nums text-slate-500">{products?.length ?? 'â€”'} products</span>
      </header>

      <section className="border border-slate-200 bg-white">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-indigo-700">Listing details</p>
            <h2 className="mt-1 font-semibold text-slate-950">{editingId === null ? 'Add a product' : `Edit product #${editingId}`}</h2>
          </div>
          {editingId !== null && <button aria-label="Cancel editing" className="rounded-sm p-2 text-slate-500 hover:bg-slate-100" onClick={resetForm} type="button"><X size={17} /></button>}
        </div>
        <form className="grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-6" onSubmit={(event) => void submitProduct(event)}>
          <label className="text-xs font-semibold text-slate-600 sm:col-span-1 lg:col-span-2" htmlFor="product-name">
            Product name
            <input className="mt-1.5 w-full rounded-sm border border-slate-300 px-3 py-2.5 text-sm font-normal text-slate-950" id="product-name" maxLength={255} onChange={(event) => setForm({ ...form, name: event.target.value })} required value={form.name} />
          </label>
          <label className="text-xs font-semibold text-slate-600 sm:col-span-1 lg:col-span-2" htmlFor="product-category">
            Category
            <input className="mt-1.5 w-full rounded-sm border border-slate-300 px-3 py-2.5 text-sm font-normal text-slate-950" id="product-category" onChange={(event) => setForm({ ...form, category: event.target.value })} required value={form.category} />
          </label>
          <label className="text-xs font-semibold text-slate-600" htmlFor="product-price">
            Price
            <input className="mt-1.5 w-full rounded-sm border border-slate-300 px-3 py-2.5 text-sm font-normal text-slate-950" id="product-price" min="0.01" onChange={(event) => setForm({ ...form, price: Number(event.target.value) })} required step="0.01" type="number" value={form.price || ''} />
          </label>
          <label className="text-xs font-semibold text-slate-600" htmlFor="product-stock">
            Stock
            <input className="mt-1.5 w-full rounded-sm border border-slate-300 px-3 py-2.5 text-sm font-normal text-slate-950" id="product-stock" min="0" onChange={(event) => setForm({ ...form, stock: Number(event.target.value) })} required type="number" value={form.stock} />
          </label>
          <label className="text-xs font-semibold text-slate-600 sm:col-span-2 lg:col-span-3" htmlFor="product-image-url">
            Image URL
            <input className="mt-1.5 w-full rounded-sm border border-slate-300 px-3 py-2.5 text-sm font-normal text-slate-950" id="product-image-url" maxLength={2048} onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} placeholder="https://example.com/product.jpg" type="url" value={form.imageUrl} />
          </label>
          <label className="text-xs font-semibold text-slate-600 sm:col-span-2 lg:col-span-4" htmlFor="product-description">
            Description
            <textarea className="mt-1.5 min-h-20 w-full resize-y rounded-sm border border-slate-300 px-3 py-2.5 text-sm font-normal text-slate-950" id="product-description" maxLength={2000} onChange={(event) => setForm({ ...form, description: event.target.value })} required value={form.description} />
          </label>
          <div className="flex items-end gap-2 sm:col-span-2 lg:col-span-2">
            <button className="inline-flex items-center gap-2 rounded-sm bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50" disabled={isSaving} type="submit">
              {editingId === null ? <Plus aria-hidden="true" size={16} /> : <Check aria-hidden="true" size={16} />}
              {isSaving ? 'Savingâ€¦' : editingId === null ? 'Create product' : 'Save changes'}
            </button>
            {editingId !== null && <button className="rounded-sm border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50" onClick={resetForm} type="button">Cancel</button>}
          </div>
          {error && <p className="text-sm text-red-800 sm:col-span-2 lg:col-span-6" role="alert">{error}</p>}
        </form>
      </section>

      <section className="border border-slate-200 bg-white">
        <div className="border-b border-slate-200 px-5 py-4">
          <h2 className="font-semibold text-slate-950">Your listings</h2>
        </div>
        {products === null && !error ? <p className="p-5 text-sm text-slate-600" role="status">Loading your inventory…</p> : null}
        {products?.length === 0 && <p className="p-8 text-sm text-slate-600">No products listed yet. Add your first product to start selling.</p>}
        {products && products.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[42rem] text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr><th className="px-5 py-3 font-semibold">Product</th><th className="px-5 py-3 font-semibold">Category</th><th className="px-5 py-3 text-right font-semibold">Price</th><th className="px-5 py-3 text-right font-semibold">Stock</th><th className="px-5 py-3" /></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {products.map((product) => (
                  <tr className="hover:bg-slate-50" key={product.id}>
                    <td className="max-w-sm px-5 py-3.5 font-medium text-slate-900">
                      <div className="flex items-center gap-3">
                        <ProductImage alt={product.name} className="h-12 w-12 shrink-0 rounded-sm" imageClassName="object-cover" src={product.imageUrl} />
                        <span className="truncate">{product.name}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-slate-600">{product.category}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-800">{currency.format(product.price)}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-800">{product.stock}</td>
                    <td className="px-5 py-3.5 text-right">
                      <button aria-label={`Edit ${product.name}`} className="inline-flex items-center gap-1 rounded-sm border border-slate-300 px-2.5 py-1.5 text-xs font-semibold text-slate-700 hover:border-indigo-700 hover:text-indigo-600" onClick={() => editProduct(product)} type="button">
                        <Pencil aria-hidden="true" size={13} /> Edit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

