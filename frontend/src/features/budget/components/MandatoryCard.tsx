import { fmt } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import { needOf, type MandatoryItem } from '../model'

export default function MandatoryCard({ item, onToggle }: { item: MandatoryItem; onToggle: () => void }) {
  const need = needOf(item)
  const full = need <= 0
  const havePct = Math.min(100, Math.round((item.have / item.amount) * 100))
  const solidPct = item.paid ? 100 : havePct
  const addPct = item.sel && !full && !item.paid ? 100 - havePct : 0

  const status = item.paid
    ? '✓ пополнено'
    : full
      ? '✓ накоплено'
      : item.sel
        ? `+${fmt(need)} ₸ из зарплаты`
        : `не хватает ${fmt(need)} ₸`
  const statusCls = item.paid || full ? 'text-pos' : item.sel ? 'text-accent' : 'text-muted'

  return (
    <div className={cn('mb-2.5 rounded-card bg-card p-3.5 shadow-card transition', !item.sel && 'opacity-60')}>
      <div className="flex items-center gap-3">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gold-soft text-lg">{item.icon}</span>
        <div className="min-w-0 flex-1">
          <div className="text-[13.5px] font-bold">{item.name}</div>
          <div className="text-[11.5px] text-muted">{item.sub}</div>
        </div>
        <button
          role="checkbox"
          aria-checked={item.sel}
          aria-label={`Пополнять ${item.name}`}
          onClick={onToggle}
          className={cn(
            'grid h-[26px] w-[26px] shrink-0 place-items-center rounded-[9px] border-2 text-[13px] text-white transition active:scale-90',
            item.sel ? 'border-accent bg-accent' : 'border-line bg-card',
          )}
        >
          {item.sel ? '✓' : ''}
        </button>
      </div>

      <div className="mt-3">
        <div className="relative h-2 overflow-hidden rounded-full bg-line2">
          <span className="absolute left-0 top-0 h-full rounded-full bg-accent" style={{ width: `${solidPct}%` }} />
          {addPct > 0 && (
            <span
              className="absolute top-0 h-full rounded-full bg-accent/40"
              style={{ left: `${havePct}%`, width: `${addPct}%` }}
            />
          )}
        </div>
        <div className="mt-1.5 flex items-center justify-between text-[11px] text-muted">
          <span>
            Сейчас <b className="font-bold text-ink">{fmt(item.paid ? item.amount : item.have)} ₸</b>
          </span>
          <span className={cn('font-bold', statusCls)}>{status}</span>
          <span>
            Цель <b className="font-bold text-ink">{fmt(item.amount)} ₸</b>
          </span>
        </div>
      </div>
    </div>
  )
}
