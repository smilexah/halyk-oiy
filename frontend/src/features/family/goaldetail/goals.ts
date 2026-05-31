/** Goal detail data (from goaldetail.js). */

export type Perm = 'owner' | 'topup' | 'view' | 'full'

export interface GoalMember {
  av: string
  name: string
  perm: Perm
  permLabel: string
  amount: number
  you?: boolean
}

export interface GoalRule {
  e: string
  who: string
  t: string
  on: boolean
  sub: string
  pos?: boolean
}

export interface GoalContact {
  av: string
  name: string
  phone: string
}

export interface GoalDef {
  emoji: string
  name: string
  titleSub: string
  heroSub: string
  target: number
  collected: number
  perMonth: string
  term: string
  ai: string
  memberSec: string
  offer: string
  memberSub: string
  members: GoalMember[]
  rules: GoalRule[]
  contacts: GoalContact[]
}

export const PERM_PI: Record<Perm, string> = { owner: '👑', topup: '➕', view: '👁️', full: '🔁' }
export const PERM_LABEL: Record<Exclude<Perm, 'owner'>, string> = {
  view: 'Только просмотр',
  topup: 'Может пополнять',
  full: 'Пополнять и снимать',
}

export const GOALS: Record<'paris' | 'turkey', GoalDef> = {
  paris: {
    emoji: '🗼',
    name: 'Париж 2027',
    titleSub: 'Семейная цель · краудфандинг',
    heroSub: 'Отпуск · 2 взрослых + 1 ребёнок · к июлю 2027',
    target: 1200000,
    collected: 740000,
    perMonth: '≈ 57 000 ₸',
    term: '8 мес.',
    ai: 'По текущему темпу <b>≈ 57 000 ₸/мес</b> цель закроется <b>к 5 июля</b> — на 3 недели раньше срока. Хотите ускорить? Включите автопополнение для всех.',
    memberSec: 'Кто пополняет',
    offer: 'Скидка −8% на тур через маркетплейс банка + ставка 16.5%',
    memberSub: 'Выберите из контактов. Семейный кошелёк — для родных.',
    members: [
      { av: '👨🏻', name: 'Асхат', perm: 'owner', permLabel: 'Владелец · полный доступ', amount: 420000, you: true },
      { av: '👩🏻', name: 'Динара', perm: 'topup', permLabel: 'Может пополнять · кэшбэк', amount: 210000 },
      { av: '👵🏻', name: 'Апа (бабушка)', perm: 'topup', permLabel: 'Может пополнять', amount: 110000 },
    ],
    rules: [
      { e: '🪙', who: 'Асхат', t: 'округлять остатки от покупок в цель', on: true, sub: '≈ 4 500 ₸ в этом месяце', pos: true },
      { e: '💸', who: 'Динара', t: 'весь кэшбэк — в цель', on: true, sub: '+8 400 ₸ в этом месяце', pos: true },
      { e: '📅', who: 'Все', t: 'автопополнение 1 числа', on: false, sub: 'по 25 000 ₸ с каждого' },
    ],
    contacts: [
      { av: '👴🏻', name: 'Ата (дедушка)', phone: '+7 701 ··· 4408' },
      { av: '👩🏻‍🦰', name: 'Гульнара (тётя)', phone: '+7 747 ··· 7731' },
      { av: '👧🏻', name: 'Жанеке', phone: '+7 700 ··· 5560' },
    ],
  },
  turkey: {
    emoji: '🏖️',
    name: 'Турция 2026',
    titleSub: 'Личная цель · можно добавить друзей',
    heroSub: 'Отпуск с друзьями · к сентябрю 2026',
    target: 900000,
    collected: 540000,
    perMonth: '≈ 45 000 ₸',
    term: '8 мес.',
    ai: 'Это <b>ваша личная цель</b> — копите сами или позовите друзей вскладчину. По текущему темпу <b>≈ 45 000 ₸/мес</b> закроете <b>к августу</b>.',
    memberSec: 'Участники складчины',
    offer: 'Раннее бронирование −10% и рассрочка 0% через маркетплейс банка',
    memberSub: 'Добавьте кого угодно из контактов — не обязательно члена семьи.',
    members: [
      { av: '👨🏻', name: 'Асхат', perm: 'owner', permLabel: 'Владелец · полный доступ', amount: 380000, you: true },
      { av: '🧔🏻', name: 'Ерлан (друг)', perm: 'topup', permLabel: 'Может пополнять · вскладчину', amount: 160000 },
    ],
    rules: [
      { e: '🪙', who: 'Асхат', t: 'округлять остатки от покупок в цель', on: true, sub: '≈ 3 200 ₸ в этом месяце', pos: true },
      { e: '💳', who: 'Асхат', t: '10% кэшбэка с карты — в цель', on: true, sub: '+2 600 ₸ в этом месяце', pos: true },
      { e: '📅', who: 'Асхат', t: 'автопополнение 5 числа', on: false, sub: 'по 40 000 ₸' },
    ],
    contacts: [
      { av: '🧑🏻', name: 'Дамир (друг)', phone: '+7 705 ··· 1192' },
      { av: '👩🏻', name: 'Сауле (коллега)', phone: '+7 778 ··· 3340' },
      { av: '🧔🏻', name: 'Тимур (друг)', phone: '+7 747 ··· 9981' },
    ],
  },
}
