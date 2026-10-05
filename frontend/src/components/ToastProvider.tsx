import { useCallback, useEffect, useRef, useState } from 'react'
import type { PropsWithChildren } from 'react'
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import { ToastContext } from './toastContext'
import type { ToastVariant } from './toastContext'

type ToastMessage = { id: number; message: string; variant: ToastVariant }

function ToastCard({ toast, onDismiss }: { toast: ToastMessage; onDismiss: (id: number) => void }) {
  useEffect(() => {
    const timeout = window.setTimeout(() => onDismiss(toast.id), 4500)
    return () => window.clearTimeout(timeout)
  }, [onDismiss, toast.id])

  const styles = {
    success: { icon: <CheckCircle2 aria-hidden="true" size={19} />, color: 'text-indigo-700', role: 'status' as const },
    error: { icon: <AlertCircle aria-hidden="true" size={19} />, color: 'text-rose-700', role: 'alert' as const },
    info: { icon: <Info aria-hidden="true" size={19} />, color: 'text-sky-700', role: 'status' as const },
  }[toast.variant]

  return (
    <li className="pointer-events-auto flex items-start gap-3 rounded-xl border border-stone-200 bg-white p-4 shadow-xl ring-1 ring-black/5" role={styles.role}>
      <span className={`mt-0.5 shrink-0 ${styles.color}`}>{styles.icon}</span>
      <p className="min-w-0 flex-1 text-sm font-medium leading-5 text-stone-800">{toast.message}</p>
      <button
        aria-label="Dismiss notification"
        className="-mr-1 -mt-1 rounded-md p-1 text-stone-400 transition hover:bg-stone-100 hover:text-stone-700 focus:outline-none focus:ring-2 focus:ring-indigo-700"
        onClick={() => onDismiss(toast.id)}
        type="button"
      >
        <X aria-hidden="true" size={16} />
      </button>
    </li>
  )
}

export function ToastProvider({ children }: PropsWithChildren) {
  const [toasts, setToasts] = useState<ToastMessage[]>([])
  const nextId = useRef(0)

  const showToast = useCallback((message: string, variant: ToastVariant = 'success') => {
    const id = ++nextId.current
    setToasts((current) => [...current, { id, message, variant }].slice(-4))
  }, [])

  const dismissToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <ol aria-label="Notifications" className="pointer-events-none fixed right-4 top-4 z-[100] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-3 sm:right-6 sm:top-6">
        {toasts.map((toast) => <ToastCard key={toast.id} onDismiss={dismissToast} toast={toast} />)}
      </ol>
    </ToastContext.Provider>
  )
}
