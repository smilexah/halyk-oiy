import { House, CreditCard, QrCode, ArrowLeftRight, ReceiptText, type LucideIcon } from 'lucide-react'

export type TabId = 'home' | 'cards' | 'qr' | 'transfers' | 'payments'
/** Active screens that aren't bottom-nav tabs. */
export type ScreenId = TabId | 'profile'

export interface TabDef {
  id: TabId
  label: string
  Icon: LucideIcon
  fab?: boolean
}

export const TABS: TabDef[] = [
  { id: 'home', label: 'Главная', Icon: House },
  { id: 'cards', label: 'Мой банк', Icon: CreditCard },
  { id: 'qr', label: 'QR', Icon: QrCode, fab: true },
  { id: 'transfers', label: 'Переводы', Icon: ArrowLeftRight },
  { id: 'payments', label: 'Платежи', Icon: ReceiptText },
]
