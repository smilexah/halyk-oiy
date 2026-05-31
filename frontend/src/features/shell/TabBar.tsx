import { TABS, type TabId, type ScreenId } from './nav'
import { useToast } from '../../shared/ui/toast'
import { cn } from '../../shared/lib/cn'

export default function TabBar({
  active,
  onSwitch,
}: {
  active: ScreenId
  onSwitch: (id: TabId) => void
}) {
  const { showToast } = useToast()

  return (
    <nav className="absolute inset-x-0 bottom-0 z-40 flex h-[calc(70px+env(safe-area-inset-bottom))] border-t border-line bg-card pb-[env(safe-area-inset-bottom)] shadow-[0_-4px_20px_rgba(0,0,0,0.04)]">
      {TABS.map((t) => {
        const isActive = active === t.id
        if (t.fab) {
          return (
            <button
              key={t.id}
              onClick={() => showToast('📷 Сканер QR для оплаты')}
              className="flex flex-1 flex-col items-center justify-end gap-1 pb-2 text-muted"
            >
              <span className="grid h-[52px] w-[52px] -translate-y-4 place-items-center rounded-2xl bg-accent text-white shadow-glow">
                <t.Icon size={24} strokeWidth={2} />
              </span>
              <span className="-mt-3 text-[10px] font-bold">{t.label}</span>
            </button>
          )
        }
        return (
          <button
            key={t.id}
            onClick={() => onSwitch(t.id)}
            className={cn(
              'flex flex-1 flex-col items-center justify-center gap-1 pt-2 transition-colors',
              isActive ? 'text-accent' : 'text-muted',
            )}
          >
            <t.Icon size={23} strokeWidth={isActive ? 2.4 : 1.9} className={cn('transition-transform', isActive && '-translate-y-px')} />
            <span className="text-[10px] font-bold">{t.label}</span>
          </button>
        )
      })}
    </nav>
  )
}
