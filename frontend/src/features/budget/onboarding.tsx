import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { useLocalStorage } from '../../shared/hooks/useLocalStorage'
import { useOverlay, type SuccessData } from '../../shared/ui/overlay'

type FirstRunStart = 'lock' | 'wizard'

interface OnboardingCtx {
  onboarded: boolean
  firstRun: FirstRunStart | null
  /** times the wizard has finished — Home watches this to jump to «Мои финансы». */
  finTick: number
  openFirstRun: (start: FirstRunStart) => void
  closeFirstRun: () => void
  completeFirstRun: (summary: SuccessData) => void
  reset: () => void
}

const Ctx = createContext<OnboardingCtx | null>(null)

/**
 * Owns the salary-distribution onboarding. On first load (not onboarded) the
 * lockscreen push auto-appears — matching the prototype's entry point — and
 * stays the entry until the wizard is completed (persisted in `halyk_onboarded`).
 */
export function OnboardingProvider({ children }: { children: ReactNode }) {
  const { showSuccess } = useOverlay()
  const [onboarded, setOnboarded] = useLocalStorage<boolean>('halyk_onboarded', false)
  const [firstRun, setFirstRun] = useState<FirstRunStart | null>(null)
  const [finTick, setFinTick] = useState(0)
  const autoShown = useRef(false)

  useEffect(() => {
    if (!onboarded && !autoShown.current) {
      autoShown.current = true
      setFirstRun('lock')
    }
  }, [onboarded])

  const value: OnboardingCtx = {
    onboarded,
    firstRun,
    finTick,
    openFirstRun: setFirstRun,
    closeFirstRun: () => setFirstRun(null),
    completeFirstRun: (summary) => {
      setOnboarded(true)
      setFirstRun(null)
      setFinTick((t) => t + 1)
      showSuccess(summary)
    },
    reset: () => {
      setOnboarded(false)
      autoShown.current = true
      setFirstRun('lock')
    },
  }

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useOnboarding(): OnboardingCtx {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useOnboarding must be used within OnboardingProvider')
  return ctx
}
