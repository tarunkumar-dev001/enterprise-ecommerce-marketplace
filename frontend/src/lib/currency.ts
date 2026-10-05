const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function formatCurrency(amount: number | null | undefined): string {
  return inrFormatter.format(Number.isFinite(amount) ? amount ?? 0 : 0)
}
