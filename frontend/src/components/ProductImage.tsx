import { useState } from 'react'
import { ImageOff } from 'lucide-react'

type ProductImageProps = {
  src?: string | null
  alt: string
  className?: string
  imageClassName?: string
  loading?: 'eager' | 'lazy'
}

export function ProductImage({ src, alt, className = '', imageClassName = 'object-cover', loading = 'lazy' }: ProductImageProps) {
  const [failedSource, setFailedSource] = useState<string | null>(null)
  const normalizedSource = src?.trim() || ''
  const hasSource = normalizedSource.length > 0 && failedSource !== normalizedSource

  return (
    <div className={`relative overflow-hidden bg-stone-100 ${className}`}>
      {hasSource ? (
        <img
          alt={alt}
          className={`absolute inset-0 h-full w-full ${imageClassName}`}
          loading={loading}
          onError={() => setFailedSource(normalizedSource)}
          src={normalizedSource}
        />
      ) : (
        <div aria-label={alt} className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-gradient-to-br from-stone-100 via-stone-50 to-indigo-50 p-5 text-center text-stone-500" role="img">
          <span className="flex h-16 w-16 items-center justify-center rounded-full border border-stone-200 bg-white/80 text-indigo-800 shadow-sm">
            <ImageOff aria-hidden="true" size={27} strokeWidth={1.5} />
          </span>
          <span className="line-clamp-2 text-xs font-medium">{alt}</span>
        </div>
      )}
    </div>
  )
}
