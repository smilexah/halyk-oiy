import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { usePortalContainer } from './portalContainer'

interface ToastCtx {
  showToast: (msg: string) => void
}

const Ctx = createContext<ToastCtx | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [msg, setMsg] = useState<string | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const container = usePortalContainer()

  const showToast = useCallback((m: string) => {
    setMsg(m)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setMsg(null), 2200)
  }, [])

  const toastEl = (
    <div
      className={cnToast(!!msg, !!container)}
      role="status"
      aria-live="polite"
    >
      {msg}
    </div>
  )

  return (
    <Ctx.Provider value={{ showToast }}>
      {children}
      {createPortal(toastEl, container ?? document.body)}
    </Ctx.Provider>
  )
}

const cnToast = (show: boolean, contained: boolean) =>
  [
    contained ? 'absolute' : 'fixed',
    'pointer-events-none left-1/2 top-5 z-[100] -translate-x-1/2',
    'rounded-2xl bg-ink/90 px-5 py-3 text-[13px] font-semibold text-white shadow-float backdrop-blur',
    'transition-all duration-300',
    show ? 'translate-y-0 opacity-100' : '-translate-y-3 opacity-0',
  ].join(' ')

export function useToast(): ToastCtx {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
