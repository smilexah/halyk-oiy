/** AI-рекомендации (сторис) — 4 ленты по частым тратам пользователя.
 *  Ported from maqsat-prototype/recos.js. */

export interface RecoStory {
  id: string
  /** full-bleed background gradient for the slide */
  grad: string
  icon: string
  eyebrow: string
  stat: string
  /** pre-formatted big number, e.g. "88 000" */
  big: string
  unit: string
  bigsub: string
  title: string
  /** may contain inline <b> markup */
  body: string
  pill: string
  cta: string
  /** dark CTA label (gold slide) */
  dark?: boolean
}

export const RECO_STORIES: RecoStory[] = [
  {
    id: 'magnum',
    grad: 'linear-gradient(165deg,#13A981 0%,#00815F 55%,#005A42 100%)',
    icon: '🛒',
    eyebrow: 'ЧАСТЫЕ ТРАТЫ · ПРОДУКТЫ',
    stat: '14 покупок в Magnum за месяц',
    big: '88 000',
    unit: '₸',
    bigsub: 'тратите на продукты',
    title: 'Платите картой Halyk в Magnum',
    body: 'AI заметил: продукты — ваша самая частая трата. Включите кешбэк и возвращайте <b>5% бонусами</b> с каждой покупки.',
    pill: '≈ 4 400 ₸ возвращается каждый месяц',
    cta: 'Включить Magnum-кешбэк',
  },
  {
    id: 'taxi',
    grad: 'linear-gradient(165deg,#F8BE3C 0%,#F1A400 48%,#D98600 100%)',
    icon: '🚕',
    eyebrow: 'ЧАСТЫЕ ТРАТЫ · ТРАНСПОРТ',
    stat: '21 поездка Yandex Go в этом месяце',
    big: '21 000',
    unit: '₸',
    bigsub: 'на такси',
    title: 'Подписка Halyk + Yandex Go',
    body: 'Вы ездите почти каждый день. С подпиской возвращаем <b>10%</b> за каждую поездку — окупается уже с 4-й.',
    pill: '≈ 2 100 ₸/мес экономии',
    cta: 'Оформить подписку',
    dark: true,
  },
  {
    id: 'subs',
    grad: 'linear-gradient(165deg,#1C4D8F 0%,#123B73 52%,#0C2A55 100%)',
    icon: '🔁',
    eyebrow: 'AI НАШЁЛ СКРЫТЫЕ ПЛАТЕЖИ',
    stat: '3 автоплатежа: Netflix · Spotify · iCloud',
    big: '78 000',
    unit: '₸',
    bigsub: 'в год уходит незаметно',
    title: 'Соберите подписки в Halyk',
    body: 'Один счёт вместо трёх: видите все списания, отключаете в один тап и получаете <b>3% кешбэка</b>.',
    pill: '6 500 ₸/мес под контролем',
    cta: 'Управлять подписками',
  },
  {
    id: 'fuel',
    grad: 'linear-gradient(165deg,#0E5A43 0%,#0A4534 60%,#063425 100%)',
    icon: '⛽',
    eyebrow: 'ЧАСТЫЕ ТРАТЫ · АВТО',
    stat: '4 заправки на Helios за месяц',
    big: '26 000',
    unit: '₸',
    bigsub: 'на топливо',
    title: 'Карта Halyk × Helios',
    body: 'Заправляетесь всегда на Helios. Совместная карта даёт <b>7% на топливо</b> и приоритет на АЗС-партнёрах.',
    pill: '≈ 1 800 ₸/мес · 21 600 ₸/год',
    cta: 'Заказать карту',
  },
]