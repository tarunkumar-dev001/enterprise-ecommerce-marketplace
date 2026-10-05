const productPhotos = [
  { keywords: ['headphone', 'audio', 'speaker'], id: 'photo-1505740420928-5e560c06d30e' },
  { keywords: ['shoe', 'sneaker', 'runner'], id: 'photo-1542291026-7eec264c27ff' },
  { keywords: ['watch', 'clock'], id: 'photo-1523275335684-37898b6baf30' },
  { keywords: ['camera', 'lens'], id: 'photo-1516035069371-29a1b244cc32' },
  { keywords: ['keyboard', 'computer', 'electronic'], id: 'photo-1587829741301-dc798b83add3' },
  { keywords: ['mug', 'cup', 'home'], id: 'photo-1514228742587-6b1558fcca3d' },
]

export function productImageFor(name: string, category: string) {
  const searchable = `${name} ${category}`.toLowerCase()
  const photo = productPhotos.find((entry) => entry.keywords.some((keyword) => searchable.includes(keyword)))
  const imageId = photo?.id ?? 'photo-1542291026-7eec264c27ff'
  return `https://images.unsplash.com/${imageId}?auto=format&fit=crop&w=900&q=85`
}