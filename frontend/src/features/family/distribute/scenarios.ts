/** Salary-distribution scenarios (from distribute.js). */

export interface Receipt {
  e: string
  a: string
  on: boolean
}

export interface ChoiceOption {
  e: string
  a: string
  b: string
  add: DistCard
}

export interface DistCard {
  id: string
  group?: string
  icon?: string
  gold?: boolean
  name?: string
  num?: string
  type?: 'deposit' | 'savings'
  rate?: string
  def?: number
  receipts?: Receipt[] | null
  goal?: boolean
  kind?: 'clarify' | 'choice'
  amount?: number
  sub?: string
  question?: string
  chips?: { label: string; result: string }[]
  options?: ChoiceOption[]
}

export interface Nba {
  flag: string
  title: string
  body: string
  yes: string
  no: string
  add: DistCard
}

export interface Scenario {
  salary: number
  cap: string
  subt: string
  hero: { greet: string; tagline: string; pill: string; cta: string; nba: boolean }
  intro: string
  startOpened: boolean
  bulk: boolean
  nba?: Nba
  cards: DistCard[]
}

const rc = (e: string, a: string): Receipt => ({ e, a, on: true })

export const SCEN: Record<'first' | 'power', Scenario> = {
  first: {
    salary: 223000,
    cap: 'Демо · как агент ведёт нового пользователя',
    subt: 'Первая зарплата · агент изучает траты',
    hero: {
      greet: 'Похоже, это ваша <b>первая зарплата</b> в Maqsat',
      tagline:
        'Я изучил траты за 3 месяца и подобрал план. Деньги останутся на вашем счёте — спишутся только после Face ID.',
      pill: 'Проанализировано 184 операции · 3 месяца',
      cta: 'Разобрать зарплату вместе →',
      nba: false,
    },
    intro:
      'Привет, Асхат! Это ваша первая зарплата здесь. Я нашёл <b>3 регулярных платежа</b> — откроем под них карты-конверты. По одной: <b>открыть → привязать квитанции → пополнить</b>.',
    startOpened: false,
    bulk: false,
    cards: [
      { id: 'util', group: 'Обязательные платежи', icon: '🏠', name: 'Коммуналка и квитанции', num: '7781', type: 'deposit', rate: 'депозитная · 14%', def: 32590, receipts: [rc('💡', 'Свет · АлматыЭнерго'), rc('🔥', 'Газ · QazaqGaz'), rc('💧', 'Вода · Су Арнасы')] },
      { id: 'fuel', group: 'Обязательные платежи', icon: '⛽', gold: true, name: 'Бензин', num: '7902', type: 'deposit', rate: 'депозитная · 14%', def: 27000, receipts: null },
      { id: 'auto', group: 'Обязательные платежи', icon: '🔧', name: 'Авто-резерв · СТО', num: '4419', type: 'savings', rate: 'сберегательная · 16.5%', def: 16000, receipts: [rc('🔧', 'СТО — диагностика · раз в 6 мес'), rc('🛢️', 'Замена масла'), rc('🛡️', 'Страховка ОСГПО')] },
      { id: 'dorm', group: 'Агент уточняет', kind: 'clarify', icon: '🎓', amount: 80000, name: 'TOO SDU DORM', sub: 'Регулярно · 80 000 ₸ каждый месяц', question: 'Вижу регулярный платёж <b>80 000 ₸</b> на «TOO SDU DORM» каждый месяц. Что это за расход?', chips: [{ label: '🏠 Общежитие сына', result: 'Общежитие · сын Жанеке' }, { label: '🏢 Аренда жилья', result: 'Аренда жилья' }, { label: '✏️ Другое', result: 'Другой регулярный платёж' }] },
      { id: 'choice', group: 'Остаток зарплаты', kind: 'choice', question: 'Останется <b>147 410 ₸</b>. Пополнить годовые расходы или создадим цель — куда-нибудь съездить?', options: [
        { e: '📑', a: 'Годовые расходы', b: 'Налог на землю и авто, страховка', add: { id: 'annual', group: 'Годовые расходы', icon: '📑', name: 'Годовой резерв', num: '5540', type: 'savings', rate: 'сберегательная · 16.5%', def: 90000, receipts: [rc('🌍', 'Налог на землю'), rc('🚗', 'Налог на авто'), rc('🛡️', 'Страховка КАСКО')] } },
        { e: '🏖️', a: 'Финансовая цель', b: 'Накопить на поездку', add: { id: 'trip', group: 'Цель', icon: '🏖️', gold: true, name: 'Цель · поездка', num: '2026', type: 'savings', rate: 'сберегательная · 16.5%', def: 120000, receipts: null, goal: true } },
      ] },
    ],
  },

  power: {
    salary: 470000,
    cap: 'Демо · агент уже знает семью и привычки',
    subt: 'Зарплата пришла · план готов как обычно',
    hero: {
      greet: 'Доброе утро, <b>Асхат</b> 👋',
      tagline: 'Зарплата пришла. План распределения готов как обычно — плюс одно предложение. Подтвердите по Face ID.',
      pill: 'NBA готов · 10 карт · 3 ребёнка',
      cta: 'Распределить как обычно →',
      nba: true,
    },
    intro: 'План на этот месяц готов. Карты уже открыты — осталось пополнить. Можно <b>пополнить всё разом</b> или поправить любую сумму.',
    startOpened: true,
    bulk: true,
    nba: {
      flag: 'NBA · проактивное предложение',
      title: 'Год назад вы летали в Баку в эти даты ✈️',
      body: 'Сейчас билеты дешевле на 12%. Заложить остаток на семейную цель «Баку 2026»?',
      yes: 'Создать цель «Баку»',
      no: 'Не сейчас',
      add: { id: 'baku', group: 'Семейная цель', icon: '🏖️', gold: true, name: 'Баку 2026', num: '2026', type: 'savings', rate: 'сберегательная · 16.5%', def: 200410, receipts: null, goal: true },
    },
    cards: [
      { id: 'util', group: 'Обязательные платежи', icon: '🏠', name: 'Коммуналка и квитанции', num: '7781', type: 'deposit', rate: 'депозитная · 14%', def: 32590, receipts: [rc('💡', 'Свет'), rc('🔥', 'Газ'), rc('💧', 'Вода')] },
      { id: 'fuel', group: 'Обязательные платежи', icon: '⛽', name: 'Бензин', num: '7902', type: 'deposit', rate: 'депозитная · 14%', def: 27000, receipts: null },
      { id: 'auto', group: 'Обязательные платежи', icon: '🔧', name: 'Авто-резерв · СТО', num: '4419', type: 'savings', rate: 'сберегательная · 16.5%', def: 16000, receipts: [rc('🔧', 'Диагностика'), rc('🛢️', 'Замена масла')] },
      { id: 'pkt-zh', group: 'Карманные детям', icon: '👧🏻', name: 'Жанеке · карманные', num: '6201', type: 'deposit', rate: 'детская · лимит', def: 20000, receipts: null },
      { id: 'pkt-am', group: 'Карманные детям', icon: '🧒🏻', name: 'Аминош · карманные', num: '6202', type: 'deposit', rate: 'детская · лимит', def: 12000, receipts: null },
      { id: 'pkt-md', group: 'Карманные детям', icon: '👦🏻', name: 'Мадеке · карманные', num: '6203', type: 'deposit', rate: 'детская · лимит', def: 15000, receipts: null },
      { id: 'dance', group: 'Образование и кружки', icon: '💃', gold: true, name: 'Танцы · Аминош', num: '4810', type: 'deposit', rate: 'депозитная · 14%', def: 24000, receipts: [rc('💃', 'Студия «Алма» · абонемент')] },
      { id: 'eng', group: 'Образование и кружки', icon: '🇬🇧', name: 'Английский · Мадеке', num: '4811', type: 'deposit', rate: 'депозитная · 14%', def: 16000, receipts: [rc('🇬🇧', 'Курсы · ежемесячно')] },
      { id: 'dorm', group: 'Образование и кружки', icon: '🎓', name: 'Общежитие · Жанеке', num: '4812', type: 'deposit', rate: 'депозитная · 14%', def: 80000, receipts: [rc('🎓', 'TOO SDU DORM · авто')] },
      { id: 'save', group: 'Накопления', icon: '💎', gold: true, name: 'Saving plan', num: '9001', type: 'savings', rate: 'сберегательная · 16.5%', def: 27000, receipts: null },
    ],
  },
}

export const pl = (n: number, a: string, b: string, c: string): string => {
  const m = n % 100
  const d = n % 10
  if (m >= 11 && m <= 14) return c
  if (d === 1) return a
  if (d >= 2 && d <= 4) return b
  return c
}
