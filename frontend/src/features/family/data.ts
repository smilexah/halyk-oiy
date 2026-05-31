/** Maqsat & Family — domain data (from family.js). */

export type Role = 'papa' | 'mama' | 'child'

export interface Member {
  av: string
  name: string
  role: string
  badge: 'owner' | 'kaspi' | 'child'
  badgeText: string
}

export interface VCard {
  name: string
  num: string
  cls: 'gold' | ''
  type: string
  bal: string
  linked: boolean
}

export interface Kid {
  av: string
  name: string
  limit: number
  usedPct: number
  pocket: string
  note: string
}

export interface Offer {
  e: string
  a: string
  b: string
  cta: string
}

export interface HistoryItem {
  e: string
  n: string
  d: string
  v: string
  pos?: boolean
}

export const MEMBERS: Member[] = [
  { av: '👨🏻', name: 'Асхат', role: 'Папа · владелец', badge: 'owner', badgeText: 'Владелец' },
  { av: '👩🏻', name: 'Динара', role: 'Мама · со-родитель', badge: 'kaspi', badgeText: 'из Kaspi' },
  { av: '👧🏻', name: 'Жанеке', role: 'Дочь · 18 · студентка', badge: 'child', badgeText: 'Студент' },
  { av: '🧒🏻', name: 'Мадеке', role: 'Сын · 14 · школа', badge: 'child', badgeText: 'Ребёнок' },
  { av: '👶🏻', name: 'Аминош', role: 'Дочь · 10', badge: 'child', badgeText: 'Ребёнок' },
]

export const INITIAL_VCARDS: VCard[] = [
  { name: 'Коммуналка', num: '7781', cls: 'gold', type: 'депозитная 14%', bal: '32 590', linked: true },
  { name: 'Париж 2027', num: '2026', cls: '', type: 'сберегат. 16.5%', bal: '740 000', linked: false },
]

export const KIDS: Kid[] = [
  { av: '🧒🏻', name: 'Мадеке', limit: 2000, usedPct: 0, pocket: '15 000', note: 'школа' },
  { av: '👶🏻', name: 'Аминош', limit: 1500, usedPct: 40, pocket: '12 000', note: 'танцы' },
  { av: '👧🏻', name: 'Жанеке', limit: 8000, usedPct: 25, pocket: '80 000', note: 'общежитие' },
]

export const OFFERS: Offer[] = [
  { e: '🛒', a: 'Magnum — продукты', b: 'Оплатите из кошелька и получите +5% бонусов', cta: '+5%' },
  { e: '⛽', a: 'Helios — топливо', b: 'Кэшбэк 3% уходит в цель «Париж 2027»', cta: '3%' },
  { e: '💊', a: 'Аптеки Europharma', b: 'Семейная скидка для участников группы', cta: '−7%' },
]

export const CHILD_HISTORY: HistoryItem[] = [
  { e: '🍫', n: 'Магазин у школы', d: 'Сегодня · 11:20', v: '−400 ₸' },
  { e: '🚌', n: 'Проезд · автобус', d: 'Сегодня · 08:05', v: '−160 ₸' },
  { e: '👛', n: 'Карманные от папы', d: '1 июня', v: '+15 000 ₸', pos: true },
]
