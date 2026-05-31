import { useCallback, useState } from 'react'
import { SCEN, type DistCard, type Scenario } from './scenarios'

export type CardStatus =
  | 'new'
  | 'opened'
  | 'linked'
  | 'funded'
  | 'clarify'
  | 'clarified'
  | 'choice'
  | 'chosen'

export interface RuntimeCard extends DistCard {
  status: CardStatus
  amount: number
  linked: boolean
  expanded: boolean
  answer?: string
  chosenName?: string
}

const cloneCard = (c: DistCard, s: Scenario): RuntimeCard => {
  const o: RuntimeCard = {
    ...c,
    receipts: c.receipts ? c.receipts.map((r) => ({ ...r })) : c.receipts,
    amount: c.def != null ? c.def : 0,
    status: 'new',
    linked: !c.receipts,
    expanded: false,
  }
  if (c.kind === 'clarify') o.status = 'clarify'
  else if (c.kind === 'choice') o.status = 'choice'
  else o.status = s.startOpened ? 'opened' : 'new'
  return o
}

export function useDistribute(mode: 'first' | 'power') {
  const scen = SCEN[mode]
  const [plan, setPlan] = useState<RuntimeCard[]>(() => scen.cards.map((c) => cloneCard(c, scen)))
  const [nbaDismissed, setNbaDismissed] = useState(false)

  const reset = useCallback(() => {
    setPlan(scen.cards.map((c) => cloneCard(c, scen)))
    setNbaDismissed(false)
  }, [scen])

  const fundable = plan.filter((c) => !c.kind)
  const remaining = scen.salary - fundable.filter((c) => c.status === 'funded').reduce((s, c) => s + c.amount, 0)
  const fundedCount = fundable.filter((c) => c.status === 'funded').length

  const patch = (id: string, fn: (c: RuntimeCard) => RuntimeCard) =>
    setPlan((list) => list.map((c) => (c.id === id ? fn(c) : c)))

  const setAmount = (id: string, amount: number) => patch(id, (c) => ({ ...c, amount }))
  const open = (id: string) => patch(id, (c) => ({ ...c, status: 'opened' }))
  const expand = (id: string) => patch(id, (c) => ({ ...c, expanded: true }))
  const link = (id: string) => patch(id, (c) => ({ ...c, linked: true }))
  const fund = (id: string) =>
    patch(id, (c) => (c.amount > remaining ? c : { ...c, status: 'funded' }))
  const cancel = (id: string) => patch(id, (c) => ({ ...c, status: 'opened' }))
  const toggleReceipt = (id: string, i: number) =>
    patch(id, (c) => ({
      ...c,
      receipts: c.receipts ? c.receipts.map((r, j) => (j === i ? { ...r, on: !r.on } : r)) : c.receipts,
    }))

  const answerClarify = (id: string, chipIndex: number) =>
    patch(id, (c) => ({ ...c, answer: c.chips?.[chipIndex]?.result, status: 'clarified' }))

  const chooseOption = (id: string, optIndex: number) =>
    setPlan((list) => {
      const idx = list.findIndex((c) => c.id === id)
      if (idx < 0) return list
      const opt = list[idx].options?.[optIndex]
      if (!opt) return list
      const added = cloneCard(opt.add, scen)
      const next = [...list]
      next[idx] = { ...next[idx], status: 'chosen', chosenName: opt.add.name }
      next.splice(idx + 1, 0, added)
      return next
    })

  const acceptNba = () => {
    if (scen.nba) setPlan((list) => [...list, cloneCard(scen.nba!.add, scen)])
    setNbaDismissed(true)
  }
  const dismissNba = () => setNbaDismissed(true)

  const fundAll = () =>
    setPlan((list) => {
      let r = remaining
      return list.map((c) => {
        if (!c.kind && c.status !== 'funded' && c.amount <= r) {
          r -= c.amount
          return { ...c, status: 'funded' as CardStatus }
        }
        return c
      })
    })

  return {
    scen,
    plan,
    nbaDismissed,
    fundable,
    remaining,
    fundedCount,
    reset,
    setAmount,
    open,
    expand,
    link,
    fund,
    cancel,
    toggleReceipt,
    answerClarify,
    chooseOption,
    acceptNba,
    dismissNba,
    fundAll,
  }
}
