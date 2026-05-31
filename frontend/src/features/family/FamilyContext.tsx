import { createContext, useContext, useRef, useState, type ReactNode } from 'react'
import { INITIAL_VCARDS, type Role, type VCard } from './data'
import { useToast } from '../../shared/ui/toast'
import { useOverlay } from '../../shared/ui/overlay'

export type PayResult = 'idle' | 'declined' | 'denied' | 'paid'
export type PushStage = 'ask' | 'loading'
export type GoalKey = 'paris' | 'turkey'

interface FamilyCtx {
  role: Role
  setRole: (r: Role) => void

  vcards: VCard[]
  addFundedCards: (cards: VCard[]) => void

  // SOS conflict flow
  childToday: string
  payResult: PayResult
  pushVisible: boolean
  pushStage: PushStage
  startSOS: () => void
  requestApproval: () => void
  approveSOS: () => void
  denySOS: () => void

  // overlays
  mode: 'first' | 'power'
  setMode: (m: 'first' | 'power') => void
  distributeOpen: boolean
  setDistributeOpen: (open: boolean) => void
  goalKey: GoalKey | null
  openGoal: (key: GoalKey) => void
  closeGoal: () => void
}

const Ctx = createContext<FamilyCtx | null>(null)

export function FamilyProvider({ children }: { children: ReactNode }) {
  const { showToast } = useToast()
  const { showSuccess } = useOverlay()

  const [role, setRoleState] = useState<Role>('papa')
  const [vcards, setVcards] = useState<VCard[]>(INITIAL_VCARDS)

  const [childToday, setChildToday] = useState('2 000 ₸')
  const [payResult, setPayResult] = useState<PayResult>('idle')
  const [pushVisible, setPushVisible] = useState(false)
  const [pushStage, setPushStage] = useState<PushStage>('ask')

  const [mode, setMode] = useState<'first' | 'power'>('first')
  const [distributeOpen, setDistributeOpen] = useState(false)
  const [goalKey, setGoalKey] = useState<GoalKey | null>(null)

  const timers = useRef<ReturnType<typeof setTimeout>[]>([])
  const later = (fn: () => void, ms: number) => {
    timers.current.push(setTimeout(fn, ms))
  }

  const setRole = (r: Role) => {
    setRoleState(r)
    setPushVisible(false)
  }

  const addFundedCards = (cards: VCard[]) =>
    setVcards((prev) => {
      const seen = new Set(prev.map((v) => v.num))
      return [...prev, ...cards.filter((c) => !seen.has(c.num))]
    })

  const startSOS = () => setPayResult('declined')

  const requestApproval = () => {
    showToast('📨 Smart-push отправлен папе')
    later(() => {
      setRole('papa')
      setPushStage('ask')
      setPushVisible(true)
    }, 1100)
  }

  const approveSOS = () => {
    setPushStage('loading')
    later(() => {
      setPushVisible(false)
      setRoleState('child')
      setChildToday('0 ₸')
      setPayResult('paid')
      showToast('✅ Оплачено на кассе')
      showSuccess({
        title: 'Разовое превышение одобрено',
        text: 'Мадеке может оплатить обед прямо на кассе. Лимит вернётся к 2 000 ₸ завтра.',
      })
    }, 1300)
  }

  const denySOS = () => {
    setPushVisible(false)
    showToast('Запрос отклонён')
    setRoleState('child')
    setPayResult('denied')
  }

  return (
    <Ctx.Provider
      value={{
        role,
        setRole,
        vcards,
        addFundedCards,
        childToday,
        payResult,
        pushVisible,
        pushStage,
        startSOS,
        requestApproval,
        approveSOS,
        denySOS,
        mode,
        setMode,
        distributeOpen,
        setDistributeOpen,
        goalKey,
        openGoal: setGoalKey,
        closeGoal: () => setGoalKey(null),
      }}
    >
      {children}
    </Ctx.Provider>
  )
}

export function useFamily(): FamilyCtx {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useFamily must be used within FamilyProvider')
  return ctx
}
