import {
  CreditCard,
  PiggyBank,
  HandCoins,
  CalendarClock,
  ShoppingBag,
  Plane,
  ShieldCheck,
  Clapperboard,
  Sparkles,
  Landmark,
  TrendingUp,
  QrCode,
  type LucideIcon,
} from 'lucide-react'

export interface ServiceDef {
  l: string
  Icon: LucideIcon
  bz?: string
}

export const SERVICES: ServiceDef[] = [
  { l: 'Карты', Icon: CreditCard },
  { l: 'Депозиты', Icon: PiggyBank },
  { l: 'Кредиты', Icon: HandCoins },
  { l: 'Рассрочка', Icon: CalendarClock },
  { l: 'Маркет', Icon: ShoppingBag, bz: '0-0-24' },
  { l: 'Travel', Icon: Plane },
  { l: 'Страхование', Icon: ShieldCheck },
  { l: 'Kino.kz', Icon: Clapperboard },
  { l: 'Halyk+', Icon: Sparkles, bz: 'NEW' },
  { l: 'Госуслуги', Icon: Landmark },
  { l: 'Инвестиции', Icon: TrendingUp },
  { l: 'QR', Icon: QrCode },
]
