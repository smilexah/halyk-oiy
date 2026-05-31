/** Budget domain — mirrors budget.js data + pure calculations. */

export interface MandatoryItem {
  id: string
  name: string
  icon: string
  /** target amount that should sit on the shared account by month end */
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

export const INITIAL_MANDATORY: MandatoryItem[] = [
  { id: 'util', name: 'Коммуналка', icon: '🏠', amount: 33000, have: 8000, sub: 'свет · газ · вода', why: 'Регулярные списания 1 числа последние 8 месяцев.', sel: true },
  { id: 'tax', name: 'Wi-fi', icon: '🏛️', amount: 9000, have: 9000, sub: 'Интернет', why: 'Годовой налог разбит помесячно — копим заранее.', sel: true },
  { id: 'subs', name: 'Подписки', icon: '🔁', amount: 6500, have: 2000, sub: 'Netflix · Spotify · iCloud', why: '3 регулярных автоплатежа распознаны по истории карты.', sel: true },
  { id: 'kids', name: 'Дети и образование', icon: '🎓', amount: 71000, have: 30000, sub: 'карманные · кружки · общежитие', why: 'Ежемесячные переводы детям и за обучение — фиксированы.', sel: true },
]

export const INITIAL_RECO: RecoItem[] = [
  { id: 'food', name: 'Еда и продукты', icon: '🛒', limit: 90000, avg: 88000, spent: 52400, why: 'Magnum, Small, базары — в среднем 88 000 ₸/мес за 3 месяца.' },
  { id: 'fun', name: 'Развлечения', icon: '🎬', limit: 30000, avg: 34000, spent: 18900, why: 'Кафе, кино, рестораны. В прошлом месяце вышли за рамки.' },
  { id: 'taxi', name: 'Такси и транспорт', icon: '🚕', limit: 18000, avg: 21000, spent: 13200, why: 'Яндекс Go и inDrive — поездок стало больше обычного.' },
  { id: 'auto', name: 'Авто и бензин', icon: '⛽', limit: 28000, avg: 26000, spent: 9600, why: 'АЗС и мелкое обслуживание. СТO раз в полгода — отдельно.' },
]

export const needOf = (c: MandatoryItem): number => Math.max(0, c.amount - c.have)
export const depOf = (c: MandatoryItem): number => (c.sel ? needOf(c) : 0)
export const sumMandatory = (items: MandatoryItem[]): number => items.reduce((s, c) => s + depOf(c), 0)
export const sumReco = (items: RecoItem[]): number => items.reduce((s, c) => s + c.limit, 0)
export const afterMandatory = (m: MandatoryItem[]): number => INCOME - sumMandatory(m)
export const freeMoney = (m: MandatoryItem[], r: RecoItem[]): number => afterMandatory(m) - sumReco(r)
