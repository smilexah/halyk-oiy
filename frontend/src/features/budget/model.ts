/** Budget domain — mirrors budget.js data + pure calculations. */

export interface MandatoryItem {
  id: string
  name: string
  icon: string
  /** target amount that should sit on the account by month end */
  amount: number
  /** how much is already accumulated */
  have: number
  sub: string
  why: string
  /** top up from salary (default true) */
  sel: boolean
  /** paid from salary */
  paid?: boolean
}

export interface RecoItem {
  id: string
  name: string
  icon: string
  /** user-editable spending guideline */
  limit: number
  /** historical average */
  avg: number
  /** spent this month */
  spent: number
  why: string
}

export const INCOME = 320000
export const MONTH = 'июнь'
// Pocket money for kids from the wizard (Жанека 20 000 + Аминош 12 000) — goes to
// the children's accounts, so it's part of the salary distribution.
export const KIDS_TOTAL = 32000

export const INITIAL_MANDATORY: MandatoryItem[] = [
  { id: 'util', name: 'АО "АЛСЕКО"', icon: '🏠', amount: 33000, have: 8000, sub: 'свет · вода', why: 'Регулярные списания 1 числа последние 8 месяцев.', sel: true },
  { id: 'gas', name: 'ТОО "Тауекел-Н-Алғабас"', icon: '🔥', amount: 13000, have: 0, sub: 'газоснабжение', why: 'Ежемесячный платёж за газ — фиксированный.', sel: true },
  { id: 'tax', name: 'ТОО «Meganet»', icon: '📶', amount: 9000, have: 9000, sub: 'Мобильная связь и Интернет', why: 'Годовой налог разбит помесячно — копим заранее.', sel: true },
  { id: 'subs', name: 'APPLE.COM/BILL', icon: '🔁', amount: 6500, have: 2000, sub: 'Netflix · Spotify · iCloud', why: '3 регулярных автоплатежа распознаны по истории карты.', sel: true },
]

export const INITIAL_RECO: RecoItem[] = [
  { id: 'food', name: 'Еда и продукты', icon: '🛒', limit: 90000, avg: 88000, spent: 52400, why: 'Magnum, Small, базары — в среднем 88 000 ₸/мес за 3 месяца.' },
  { id: 'fun', name: 'Развлечения', icon: '🎬', limit: 30000, avg: 34000, spent: 18900, why: 'Кафе, кино, рестораны. В прошлом месяце вышли за рамки.' },
  { id: 'taxi', name: 'Такси и транспорт', icon: '🚕', limit: 18000, avg: 21000, spent: 13200, why: 'Яндекс Go и inDrive — поездок стало больше обычного.' },
  { id: 'auto', name: 'Авто и бензин', icon: '⛽', limit: 28000, avg: 26000, spent: 9600, why: 'АЗС и мелкое обслуживание. СТO раз в полгода — отдельно.' },
]

export const needOf = (c: MandatoryItem): number => Math.max(0, c.amount - c.have)
export const depOf = (c: MandatoryItem): number => (c.sel ? needOf(c) : 0)

/* ── Formulas (everything derives from the salary) ───────────────
   Зарплата = Обязательные(полные) + Дети + Потребности(лимиты) + Свободные
   320 000   =     61 500          + 32 000 +    166 000        +   60 500     */
export const sumMandatoryFull = (m: MandatoryItem[]): number => m.reduce((s, c) => s + c.amount, 0)
/** how much to top up from this salary (selected, up to goal) */
export const topUp = (m: MandatoryItem[]): number => m.reduce((s, c) => s + depOf(c), 0)
export const sumReco = (r: RecoItem[]): number => r.reduce((s, c) => s + c.limit, 0)
export const spentReco = (r: RecoItem[]): number => r.reduce((s, c) => s + c.spent, 0)
export const freeMoney = (m: MandatoryItem[], r: RecoItem[]): number =>
  INCOME - sumMandatoryFull(m) - KIDS_TOTAL - sumReco(r)
/** balance left on the main card = (needs − spent) + free money */
export const mainBalance = (m: MandatoryItem[], r: RecoItem[]): number =>
  sumReco(r) - spentReco(r) + Math.max(0, freeMoney(m, r))
