/** First-run wizard domain (from firstrun.js). */

export interface FrMandatory {
  id: string
  name: string
  sub: string
  emoji: string
  amount: number
  on: boolean
  why: string
}

export interface FrKid {
  id: string
  name: string
  role: string
  emoji: string
  topup: number
  limit: number
  skip: boolean
  why: string
}

export interface SplitSeg {
  id: string
  name: string
  pct: number
  color: string
}

export const FR_SALARY = 320000
export const FR_MONTH = 'июнь'

export const FR_MANDATORY: FrMandatory[] = [
  { id: 'util', name: 'Коммуналка', sub: 'свет · газ · вода', emoji: '🏠', amount: 33000, on: true, why: 'Списания 1-го числа 8 месяцев подряд' },
  { id: 'tax', name: 'Wi-fi', sub: 'Интернет', emoji: '🏛️', amount: 9000, on: true, why: 'Годовой налог — коплю помесячно' },
  { id: 'subs', name: 'Подписки', sub: 'Netflix · Spotify · iCloud', emoji: '🔁', amount: 6500, on: true, why: '3 автоплатежа распознаны по истории карты' },
]

export const FR_KIDS: FrKid[] = [
  { id: 'zhan', name: 'Жанека', role: 'дочь · студентка', emoji: '👩🏻', topup: 20000, limit: 2000, skip: false, why: 'Каждый месяц вы переводите 20 000 ₸ контакту Жанека. Откройте счёт, с которого она сможет снимать деньги сама, а вы — управлять лимитами.' },
  { id: 'amin', name: 'Аминош', role: 'дочь · школа', emoji: '👧🏻', topup: 12000, limit: 1000, skip: false, why: 'Аминош вы тоже даёте карманные + кружок танцев. Откройте счёт с дневным лимитом — она тратит сама, а превышение вы подтверждаете.' },
]

export const FR_SPLIT: SplitSeg[] = [
  { id: 'food', name: 'Еда и продукты', pct: 0.35, color: '#006B4F' },
  { id: 'tran', name: 'Транспорт', pct: 0.15, color: '#77D9B2' },
  { id: 'fun', name: 'Развлечения', pct: 0.2, color: '#F1A400' },
  { id: 'free', name: 'Свободные деньги', pct: 0.3, color: '#D2D2D2' },
]

export const round100 = (n: number): number => Math.round(n / 100) * 100
export const sumMandatory = (m: FrMandatory[]): number => m.filter((c) => c.on).reduce((s, c) => s + c.amount, 0)
export const sumKids = (k: FrKid[]): number => k.filter((x) => !x.skip).reduce((s, x) => s + x.topup, 0)
export const freePool = (m: FrMandatory[], k: FrKid[]): number => FR_SALARY - sumMandatory(m) - sumKids(k)
