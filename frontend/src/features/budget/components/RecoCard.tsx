import { useState } from 'react'
import { fmt, parseAmount } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import type { RecoItem } from '../model'

export default function RecoCard({
  item,
  funded,
  onLimitChange,
}: {
  item: RecoItem
  /** after the wizard: bars are filled to the limit («Выделено») */
  funded: boolean
  onLimitChange: (limit: number) => void
}) {
  const [focused, setFocused] = useState(false)
  const spent = funded ? item.limit : item.spent
  const pct = Math.min(100, Math.round((spent / item.limit) * 100))
  const over = spent > item.limit
  const left = item.limit - spent

  return (
    <div className="mb-2.5 rounded-card bg-card p-3.5 shadow-card">
      <div className="flex items-center gap-3">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gold-soft text-lg">{item.icon}</span>
        <div className="min-w-0 flex-1">
          <div className="text-[13.5px] font-bold">{item.name}</div>
          <div className="text-[11.5px] text-muted">ср. {fmt(item.avg)} ₸/мес · при превышении — уведомление</div>
        </div>
        <div className="flex shrink-0 items-baseline gap-1 rounded-lg bg-bg px-2 py-1.5 focus-within:ring-2 focus-within:ring-accent">
          <input
            inputMode="numeric"
            aria-label="Лимит"
            value={focused ? String(item.limit) : fmt(item.limit)}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            onChange={(e) => onLimitChange(parseAmount(e.target.value))}
            className="w-[68px] bg-transparent text-right text-[14px] font-extrabold tabular-nums outline-none"
          />
          <span className="text-[12px] font-bold text-muted">₸</span>
        </div>
      </div>

      <div className="mt-3">
        <div className="h-2 overflow-hidden rounded-full bg-line2">
          <span className={cn('block h-full rounded-full', over ? 'bg-neg' : 'bg-accent')} style={{ width: `${pct}%` }} />
        </div>
        <div className="mt-1.5 flex items-center justify-between text-[11px] text-muted">
          <span>
            {funded ? 'Выделено' : 'Потрачено'} <b className="font-bold text-ink">{fmt(spent)} ₸</b>
          </span>
          <span className={cn('font-bold', over ? 'text-neg' : 'text-pos')}>
            {over ? `превышение ${fmt(-left)} ₸` : funded ? '100% · готово' : `осталось ${fmt(left)} ₸`}
          </span>
        </div>
      </div>
    </div>
  )
}
