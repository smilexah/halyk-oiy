import { createContext, useContext, useState, type ReactNode } from 'react'
import * as Dialog from '@radix-ui/react-dialog'
import { usePortalContainer } from './portalContainer'

export interface SuccessRow {
  k: ReactNode
  v: ReactNode
  variant?: 'hd' | 'tot'
}
export interface SuccessData {
  title: string
  text: string
  summary?: SuccessRow[]
}

interface OverlayCtx {
  showSuccess: (data: SuccessData) => void
}

const Ctx = createContext<OverlayCtx | null>(null)

export function OverlayProvider({ children }: { children: ReactNode }) {
  const [success, setSuccess] = useState<SuccessData | null>(null)
  const container = usePortalContainer()

  return (
    <Ctx.Provider value={{ showSuccess: setSuccess }}>
      {children}

      <Dialog.Root open={!!success} onOpenChange={(o) => !o && setSuccess(null)}>
        <Dialog.Portal container={container ?? undefined}>
          <Dialog.Overlay className="absolute inset-0 z-[60] bg-card data-[state=open]:animate-[fade_.2s_ease]" />
          <Dialog.Content className="absolute inset-0 z-[60] flex flex-col items-center justify-center px-8 text-center focus:outline-none">
            <span className="grid h-[92px] w-[92px] place-items-center rounded-full bg-green-soft animate-[popIn_.4s_ease]">
              <svg viewBox="0 0 100 100" className="h-14 w-14">
                <circle cx="50" cy="50" r="46" fill="none" stroke="var(--accent)" strokeWidth="6" opacity="0.25" />
                <path d="M28 52 L44 67 L73 35" fill="none" stroke="var(--accent)" strokeWidth="8" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </span>
            <Dialog.Title className="mt-6 text-[22px] font-extrabold tracking-[-0.02em]">{success?.title}</Dialog.Title>
            <Dialog.Description className="mt-2 text-[13.5px] font-medium leading-relaxed text-muted">
              {success?.text}
            </Dialog.Description>

            {success?.summary && (
              <div className="mt-5 w-full overflow-hidden rounded-card bg-bg text-left text-[13px]">
                {success.summary.map((r, i) => (
                  <div
                    key={i}
                    className={
                      'flex items-center justify-between px-4 py-3 ' +
                      (r.variant === 'hd'
                        ? 'border-b border-line text-[11px] font-bold uppercase tracking-wide text-muted'
                        : r.variant === 'tot'
                          ? 'border-t border-line font-extrabold'
                          : 'font-medium')
                    }
                  >
                    <span>{r.k}</span>
                    <span className="tabular-nums">{r.v}</span>
                  </div>
                ))}
              </div>
            )}

            <button
              onClick={() => setSuccess(null)}
              className="mt-7 w-full rounded-[15px] bg-accent px-4 py-4 text-[15.5px] font-extrabold text-white glow-sm active:scale-[.975]"
            >
              Готово
            </button>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </Ctx.Provider>
  )
}

export function useOverlay(): OverlayCtx {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useOverlay must be used within OverlayProvider')
  return ctx
}
