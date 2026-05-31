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

export interface Seg {
  id: string
  name: string
  amt: number
  color: string
}

export const FR_SALARY = 320000
export const FR_MONTH = 'июнь'

/** Step order: mandatory → free-money summary → kids. */
export const FR_STEPS = ['mandatory', 'summary', 'kid0', 'kid1'] as const

export const FR_MANDATORY: FrMandatory[] = [
  { id: 'util', name: 'АО "АЛСЕКО"', sub: 'свет · вода', emoji: '🏠', amount: 33000, on: true, why: 'Списания 1-го числа 8 месяцев подряд' },
  { id: 'gas', name: 'ТОО "Тауекел-Н-Алғабас"', sub: 'газоснабжение', emoji: '🔥', amount: 13000, on: true, why: 'Ежемесячный платёж за газ' },
  { id: 'tax', name: 'ТОО «Meganet»', sub: 'Мобильная связь и Интернет', emoji: '📶', amount: 9000, on: true, why: 'Регулярный платёж за связь' },
  { id: 'subs', name: 'APPLE.COM/BILL', sub: 'Netflix · Spotify · iCloud', emoji: '🔁', amount: 6500, on: true, why: '3 автоплатежа распознаны по истории карты' },
]

export const FR_KIDS: FrKid[] = [
  { id: 'zhan', name: 'Жанека', role: 'дочь · студентка', emoji: '👩🏻', topup: 20000, limit: 2000, skip: false, why: 'Каждый месяц вы переводите 20 000 ₸ контакту Жанека. Откройте счёт, с которого она сможет снимать деньги сама, а вы — управлять лимитами.' },
  { id: 'amin', name: 'Аминош', role: 'дочь · школа', emoji: '👧🏻', topup: 12000, limit: 1000, skip: false, why: 'Аминош вы тоже даёте карманные + кружок танцев. Откройте счёт с дневным лимитом — она тратит сама, а превышение вы подтверждаете.' },
]

/** Fixed monthly needs (same categories as «Мои финансы»). */
export const NEEDS: Seg[] = [
  { id: 'food', name: 'Еда и продукты', amt: 90000, color: '#006B4F' },
  { id: 'fun', name: 'Развлечения', amt: 30000, color: '#F1A400' },
  { id: 'taxi', name: 'Такси и транспорт', amt: 18000, color: '#77D9B2' },
  { id: 'auto', name: 'Авто и бензин', amt: 28000, color: '#C9A23F' },
]

export const sumMandatory = (m: FrMandatory[]): number => m.filter((c) => c.on).reduce((s, c) => s + c.amount, 0)
export const sumKids = (k: FrKid[]): number => k.filter((x) => !x.skip).reduce((s, x) => s + x.topup, 0)
export const freePool = (m: FrMandatory[], k: FrKid[]): number => FR_SALARY - sumMandatory(m) - sumKids(k)
export const sumNeeds = (): number => NEEDS.reduce((s, n) => s + n.amt, 0)

/** Chart segments = fixed needs + remainder as «Свободные деньги». */
export const splitSegs = (m: FrMandatory[], k: FrKid[]): Seg[] => {
  const free = Math.max(0, freePool(m, k) - sumNeeds())
  return [...NEEDS.map((n) => ({ ...n })), { id: 'free', name: 'Свободные деньги', amt: free, color: '#D2D2D2' }]
}

export const segPct = (amt: number, m: FrMandatory[], k: FrKid[]): number => {
  const p = freePool(m, k)
  return p > 0 ? Math.round((amt / p) * 100) : 0
}
