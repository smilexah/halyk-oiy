/** Map backend DTOs → the view shapes the screens already render. */
import { fmt } from '../lib/format'
import type { Offer, TransactionResponse } from './types'

export interface Row {
  e: string
  n: string
  d: string
  v: string
  pos: boolean
}

const MCC_EMOJI: Record<string, string> = {
  SALARY: '💼',
  TOPUP: '👛',
  TRANSFER: '↔️',
  PAYMENT: '🧾',
}

export function txnToRow(t: TransactionResponse): Row {
  const pos = t.direction === 'CREDIT'
  const e = pos ? '💼' : MCC_EMOJI[t.operationType] ?? '💳'
  const when = (() => {
    try {
      return new Date(t.occurredAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
    } catch {
      return ''
    }
  })()
  return {
    e,
    n: t.merchant || t.categoryName || t.details || 'Операция',
    d: [when, t.categoryName].filter(Boolean).join(' · '),
    v: `${pos ? '+' : '−'}${fmt(t.amount)} ₸`,
    pos,
  }
}

export interface OfferCard {
  e: string
  a: string
  b: string
  cta: string
}

const PARTNER_EMOJI: Record<string, string> = {
  Magnum: '🛒',
  'Yandex.Go': '🚕',
  'Halyk Maqsat': '🎯',
  Helios: '⛽',
  Europharma: '💊',
}

export function offerToCard(o: Offer): OfferCard {
  return { e: PARTNER_EMOJI[o.partner] ?? '🎁', a: `${o.partner} — ${o.title}`, b: o.title, cta: o.reward }
}
