import { useCallback, type ReactNode } from 'react'
import { portalContainer } from './portalContainer'

/**
 * The phone shell. Centered on desktop with a device bezel; full-bleed on
 * mobile. Owns the fixed 440px max-width so every screen inherits the same
 * canvas — no page sets its own width. The inner `.app` is registered as the
 * portal container so overlays clip to the frame.
 */
export default function DeviceFrame({ children }: { children: ReactNode }) {
  const appRef = useCallback((node: HTMLElement | null) => {
    portalContainer.set(node)
  }, [])

  return (
    <div className="flex min-h-dvh items-center justify-center">
      <div
        className="relative flex h-dvh w-full max-w-[440px]
          min-[600px]:h-[min(908px,calc(100dvh-48px))] min-[600px]:rounded-[46px] min-[600px]:p-[11px]
          min-[600px]:shadow-float min-[600px]:[background:linear-gradient(160deg,#1c1c1e,#000)]"
      >
        {/* notch (desktop only) */}
        <div className="absolute left-1/2 top-[11px] z-[60] hidden h-[30px] w-[128px] -translate-x-1/2 rounded-b-[18px] bg-black min-[600px]:block" />
        <div ref={appRef} className="relative flex flex-1 flex-col overflow-hidden bg-bg min-[600px]:rounded-[36px]">
          {children}
        </div>
      </div>
    </div>
  )
}
