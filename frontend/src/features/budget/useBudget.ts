import { useEffect, useState } from 'react'
import {
  INITIAL_MANDATORY,
  INITIAL_RECO,
  MONTH,
  needOf,
  sumReco,
  type MandatoryItem,
  type RecoItem,
} from './model'
import { useOnboarding } from './onboarding'
import { useToast } from '../../shared/ui/toast'
import { useOverlay } from '../../shared/ui/overlay'
import { fmt } from '../../shared/lib/format'

export type BudgetStep = 'onboard' | 'manage'

const clone = <T>(arr: T[]): T[] => arr.map((x) => ({ ...x }))

export function useBudget() {
  const { showToast } = useToast()
  const { showSuccess } = useOverlay()
  const { onboarded, openFirstRun, reset: resetOnboarding } = useOnboarding()

  const step: BudgetStep = onboarded ? 'manage' : 'onboard'
  const [mandatory, setMandatory] = useState<MandatoryItem[]>(() =>
    INITIAL_MANDATORY.map((c) => ({ ...c, paid: onboarded })),
  )
  const [reco, setReco] = useState<RecoItem[]>(clone(INITIAL_RECO))

  // When the wizard completes elsewhere, mark obligations paid in the manage view.
  useEffect(() => {
    if (onboarded) setMandatory((list) => list.map((c) => ({ ...c, paid: true })))
  }, [onboarded])

  // Launch the first-run wizard from the onboard CTA.
  const start = () => openFirstRun('wizard')

  const toggleMandatory = (id: string) =>
    setMandatory((list) => list.map((c) => (c.id === id ? { ...c, sel: !c.sel } : c)))

  const payMandatory = () => {
    const dep = mandatory.filter((c) => c.sel && needOf(c) > 0 && !c.paid).reduce((s, c) => s + needOf(c), 0)
    if (dep <= 0) return
    setMandatory((list) => list.map((c) => (c.sel && needOf(c) > 0 && !c.paid ? { ...c, paid: true } : c)))
    showToast(`✅ Пополнено ${fmt(dep)} ₸ на единый счёт`)
  }

  const setRecoLimit = (id: string, limit: number) =>
    setReco((list) => list.map((c) => (c.id === id ? { ...c, limit: Math.max(0, limit) } : c)))

  const adjustReco = (id: string, delta: number) =>
    setReco((list) => list.map((c) => (c.id === id ? { ...c, limit: Math.max(0, c.limit + delta) } : c)))

  const confirmPlan = () => {
    const overs = reco.filter((c) => c.limit < c.avg)
    showSuccess({
      title: `План рекомендаций на ${MONTH}`,
      text: overs.length
        ? `Счета не создаём — это ориентир. Если по «${overs[0].name}» выйдете за ${fmt(overs[0].limit)} ₸, пришлю уведомление, но платёж пройдёт.`
        : 'Счета не создаём — это ориентир. При выходе за лимит любой категории пришлю уведомление, но платёж пройдёт.',
      summary: [
        { k: `Можно потратить · ${MONTH}`, v: 'лимит', variant: 'hd' },
        ...reco.map((c) => ({ k: `${c.icon} ${c.name}`, v: `${fmt(c.limit)} ₸` })),
        { k: 'Всего по рекомендациям', v: `${fmt(sumReco(reco))} ₸`, variant: 'tot' as const },
      ],
    })
  }

  const replay = () => {
    setMandatory(INITIAL_MANDATORY.map((c) => ({ ...c })))
    setReco(clone(INITIAL_RECO))
    resetOnboarding()
  }

  return {
    step,
    mandatory,
    reco,
    start,
    toggleMandatory,
    payMandatory,
    setRecoLimit,
    adjustReco,
    confirmPlan,
    replay,
  }
}
