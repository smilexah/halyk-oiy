import { useState } from 'react'
import { X } from 'lucide-react'
import { useDistribute, type RuntimeCard } from './useDistribute'
import { pl } from './scenarios'
import FaceID from '../components/FaceID'
import { useFamily } from '../FamilyContext'
import { useToast } from '../../../shared/ui/toast'
import { useOverlay } from '../../../shared/ui/overlay'
import { fmt, parseAmount } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'

type D = ReturnType<typeof useDistribute>

export default function DistributeOverlay() {
  const { mode, setDistributeOpen, addFundedCards } = useFamily()
  const { showToast } = useToast()
  const { showSuccess } = useOverlay()
  const d = useDistribute(mode)
  const [faceOpen, setFaceOpen] = useState(false)

  const moved = d.scen.salary - d.remaining
  const pct = Math.max(0, Math.min(100, (moved / d.scen.salary) * 100))

  const commit = () => {
    const funded = d.fundable.filter((c) => c.status === 'funded')
    addFundedCards(
      funded.map((c) => ({
        name: (c.name ?? '').split(' · ')[0],
        num: c.num ?? '',
        cls: c.gold ? ('gold' as const) : ('' as const),
        type: c.rate ?? '',
        bal: fmt(c.amount),
        linked: !!c.linked,
      })),
    )
    setDistributeOpen(false)
    showSuccess({
      title: 'Зарплата распределена',
      text: `${funded.length} ${pl(funded.length, 'карта', 'карты', 'карт')} пополнены на ${fmt(moved)} ₸. На счёте осталось ${fmt(d.scen.salary - moved)} ₸.`,
    })
  }

  // group consecutive cards by `group`
  let lastGroup: string | undefined

  return (
    <div className="absolute inset-0 z-[55] flex flex-col bg-bg">
      {/* header */}
      <div className="flex items-center gap-3 border-b border-line px-[18px] pb-3 pt-[calc(16px+env(safe-area-inset-top))]">
        <button onClick={() => setDistributeOpen(false)} className="grid h-9 w-9 place-items-center rounded-full bg-card shadow-card" aria-label="Закрыть">
          <X size={18} />
        </button>
        <div>
          <div className="text-[16px] font-extrabold">Распределение зарплаты</div>
          <div className="text-[11.5px] text-muted">{d.scen.subt}</div>
        </div>
      </div>

      {/* counter */}
      <div className={cn('px-[18px] py-3.5', d.remaining < 0 && 'text-neg')}>
        <div className="text-[11.5px] font-semibold text-muted">Осталось распределить</div>
        <div className="text-[26px] font-extrabold">
          {fmt(d.remaining)} <span className="text-base opacity-70">₸</span>
        </div>
        <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-line2">
          <span className="block h-full rounded-full bg-accent" style={{ width: `${pct}%` }} />
        </div>
        <div className="mt-1 flex justify-between text-[11px] text-muted">
          <span>Распределено {fmt(moved)} ₸</span>
          <span>Зарплата {fmt(d.scen.salary)} ₸</span>
        </div>
      </div>

      {/* list */}
      <div className="flex-1 overflow-y-auto px-[18px] pb-4 [scrollbar-width:none]">
        <div className="flex items-start gap-2.5 rounded-card bg-green-soft p-3.5">
          <span className="text-lg">🤖</span>
          <p className="text-[12.5px] leading-snug" dangerouslySetInnerHTML={{ __html: `${d.scen.intro} <span class="block mt-1 text-[11px] text-muted">Зарплата ${fmt(d.scen.salary)} ₸ · остаётся на вашем счёте до Face ID</span>` }} />
        </div>

        {d.scen.nba && !d.nbaDismissed && (
          <div className="mt-3 rounded-card border border-accent/30 bg-card p-4 shadow-card">
            <div className="flex items-center gap-1.5 text-[10.5px] font-bold text-accent">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent" />
              {d.scen.nba.flag}
            </div>
            <div className="mt-1.5 text-[14px] font-extrabold">{d.scen.nba.title}</div>
            <div className="mt-1 text-[12.5px] text-muted">{d.scen.nba.body}</div>
            <div className="mt-3 flex gap-2">
              <button onClick={d.acceptNba} className="flex-1 rounded-xl bg-accent px-3 py-2.5 text-[12.5px] font-bold text-white">
                {d.scen.nba.yes}
              </button>
              <button onClick={d.dismissNba} className="rounded-xl border border-line px-3 py-2.5 text-[12.5px] font-bold text-muted">
                {d.scen.nba.no}
              </button>
            </div>
          </div>
        )}

        {d.scen.bulk && (
          <button onClick={d.fundAll} className="mt-3 w-full rounded-xl bg-accent px-4 py-3 text-[13.5px] font-extrabold text-white glow-sm active:scale-[.99]">
            ⚡ Пополнить всё разом
          </button>
        )}

        {d.plan.map((c) => {
          const showGroup = c.group && c.group !== lastGroup
          lastGroup = c.group
          return (
            <div key={c.id}>
              {showGroup && <div className="mt-4 mb-1.5 text-[11px] font-bold uppercase tracking-wide text-muted">{c.group}</div>}
              <DistCardView card={c} d={d} showToast={showToast} />
            </div>
          )
        })}

        <p className="mt-4 text-[11px] leading-snug text-muted">
          Деньги физически остаются на счёте Асхата. Списание — только после Face ID. Виртуальный слой с маской лимитов через API.
        </p>
      </div>

      {/* footer */}
      <div className="border-t border-line bg-card px-[18px] pb-[calc(14px+env(safe-area-inset-bottom))] pt-3">
        <div className="mb-2 flex items-center justify-between text-[12px] font-semibold text-muted">
          <span>{d.fundedCount ? `К списанию ${fmt(moved)} ₸ · остаток ${fmt(d.remaining)} ₸` : 'Откройте и пополните карты под цели'}</span>
          <span>
            <b className="text-ink">{d.fundedCount}</b> · {d.fundable.length}
          </span>
        </div>
        <button
          disabled={d.fundedCount === 0}
          onClick={() => setFaceOpen(true)}
          className="w-full rounded-[15px] bg-accent px-4 py-4 text-[15.5px] font-extrabold text-white glow-sm active:scale-[.975] disabled:opacity-50"
        >
          Подтвердить распределение
        </button>
      </div>

      <FaceID open={faceOpen} onComplete={commit} />
    </div>
  )
}

/* ── one card (clarify / choice / fundable) ─────────────────────────── */
function DistCardView({ card, d, showToast }: { card: RuntimeCard; d: D; showToast: (m: string) => void }) {
  const [focused, setFocused] = useState(false)

  if (card.kind === 'clarify') {
    if (card.status === 'clarified') {
      return (
        <Frame>
          <Top card={card} sub={`${card.answer} · авто-оплата`} />
          <Done text="Понятно — уже оплачивается автоматически" />
        </Frame>
      )
    }
    return (
      <Frame>
        <Bubble html={card.question ?? ''} />
        <div className="mt-2.5 flex flex-wrap gap-2">
          {card.chips?.map((ch, i) => (
            <button
              key={i}
              onClick={() => {
                d.answerClarify(card.id, i)
                showToast('🤖 Записал, спасибо')
              }}
              className="rounded-full border border-line px-3 py-1.5 text-[12px] font-semibold active:scale-95"
            >
              {ch.label}
            </button>
          ))}
        </div>
      </Frame>
    )
  }

  if (card.kind === 'choice') {
    if (card.status === 'chosen') {
      return (
        <Frame>
          <Bubble html={`Добавил <b>«${card.chosenName}»</b> — пополните карту ниже 👇`} />
        </Frame>
      )
    }
    return (
      <Frame>
        <Bubble html={card.question ?? ''} />
        <div className="mt-2.5 grid grid-cols-2 gap-2">
          {card.options?.map((o, i) => (
            <button
              key={i}
              onClick={() => {
                d.chooseOption(card.id, i)
                showToast('➕ Карта добавлена')
              }}
              className="rounded-card border border-line p-3 text-left active:scale-[.98]"
            >
              <div className="text-xl">{o.e}</div>
              <div className="mt-1 text-[12.5px] font-bold">{o.a}</div>
              <div className="text-[11px] text-muted">{o.b}</div>
            </button>
          ))}
        </div>
      </Frame>
    )
  }

  // normal fundable
  const over = card.amount > d.remaining
  const action = (() => {
    if (card.status === 'new')
      return <Mini variant="ghost" onClick={() => d.open(card.id)}>＋ Открыть карту</Mini>
    if (card.status === 'funded')
      return (
        <Done text={`Пополнено · ${fmt(card.amount)} ₸`}>
          <button onClick={() => d.cancel(card.id)} className="ml-auto text-[12px] font-bold text-muted underline">
            Отменить
          </button>
        </Done>
      )
    if (card.receipts && !card.linked) {
      if (!card.expanded) return <Mini variant="gold" onClick={() => d.expand(card.id)}>🧾 Привязать квитанции</Mini>
      const anyOn = card.receipts.some((r) => r.on)
      return (
        <div className="space-y-1.5">
          <div className="text-[11px] font-bold uppercase tracking-wide text-muted">Автосписания на эту карту</div>
          {card.receipts.map((r, i) => (
            <button
              key={i}
              onClick={() => d.toggleReceipt(card.id, i)}
              className={cn('flex w-full items-center gap-2.5 rounded-xl border p-2.5 text-left', r.on ? 'border-accent bg-accent-soft' : 'border-line')}
            >
              <span>{r.e}</span>
              <span className="flex-1 text-[12.5px] font-semibold">{r.a}</span>
              <span className={cn('grid h-5 w-5 place-items-center rounded-md text-white', r.on ? 'bg-accent' : 'bg-line')}>{r.on ? '✓' : ''}</span>
            </button>
          ))}
          <Mini variant="prim" disabled={!anyOn} onClick={() => { d.link(card.id); showToast('🔗 Квитанции привязаны') }}>
            Привязать выбранное
          </Mini>
        </div>
      )
    }
    return (
      <Mini variant="prim" disabled={over} onClick={() => { d.fund(card.id); showToast(`✅ ${card.goal ? 'Отложено' : 'Пополнено'} · ${fmt(card.amount)} ₸`) }}>
        {over ? 'Превышает остаток' : `${card.goal ? 'Отложить в цель · ' : 'Пополнить · '}${fmt(card.amount)} ₸`}
      </Mini>
    )
  })()

  return (
    <Frame gold={card.gold} funded={card.status === 'funded'}>
      <div className="flex items-center gap-3">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gold-soft text-lg">{card.icon}</span>
        <div className="min-w-0 flex-1">
          <div className="text-[13.5px] font-bold">{card.name}</div>
          <div className="text-[11.5px] text-muted">•••• {card.num} · {card.rate}</div>
        </div>
        <div className="flex shrink-0 items-baseline gap-1 rounded-lg bg-bg px-2 py-1.5 focus-within:ring-2 focus-within:ring-accent">
          <input
            inputMode="numeric"
            disabled={card.status === 'funded'}
            value={focused ? String(card.amount) : fmt(card.amount)}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            onChange={(e) => d.setAmount(card.id, parseAmount(e.target.value))}
            className="w-[64px] bg-transparent text-right text-[14px] font-extrabold tabular-nums outline-none disabled:opacity-60"
          />
          <span className="text-[12px] font-bold text-muted">₸</span>
        </div>
      </div>
      <div className="mt-3">{action}</div>
    </Frame>
  )
}

/* tiny presentational helpers */
function Frame({ children, gold, funded }: { children: React.ReactNode; gold?: boolean; funded?: boolean }) {
  return (
    <div
      className={cn(
        'mt-2 rounded-card bg-card p-3.5 shadow-card',
        gold && 'ring-1 ring-gold/40',
        funded && 'opacity-80',
      )}
    >
      {children}
    </div>
  )
}
function Top({ card, sub }: { card: RuntimeCard; sub: string }) {
  return (
    <div className="flex items-center gap-3">
      <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gold-soft text-lg">{card.icon}</span>
      <div className="min-w-0 flex-1">
        <div className="text-[13.5px] font-bold">{card.name}</div>
        <div className="text-[11.5px] text-muted">{sub}</div>
      </div>
      <div className="text-[15px] font-extrabold">
        {fmt(card.amount)} <span className="text-[12px] text-muted">₸</span>
      </div>
    </div>
  )
}
function Bubble({ html }: { html: string }) {
  return (
    <div className="flex items-start gap-2.5">
      <span className="text-lg">🤖</span>
      <span className="text-[12.5px] leading-snug" dangerouslySetInnerHTML={{ __html: html }} />
    </div>
  )
}
function Done({ text, children }: { text: string; children?: React.ReactNode }) {
  return (
    <div className="flex items-center gap-2 rounded-xl bg-green-soft px-3 py-2.5 text-[12.5px] font-bold text-pos">
      <span className="grid h-5 w-5 place-items-center rounded-full bg-accent text-white">✓</span>
      <span>{text}</span>
      {children}
    </div>
  )
}
function Mini({
  children,
  variant,
  disabled,
  onClick,
}: {
  children: React.ReactNode
  variant: 'prim' | 'ghost' | 'gold'
  disabled?: boolean
  onClick: () => void
}) {
  const cls = {
    prim: 'bg-accent text-white',
    ghost: 'border border-line text-ink',
    gold: 'bg-gold-soft text-gold-d',
  }[variant]
  return (
    <button onClick={onClick} disabled={disabled} className={cn('w-full rounded-xl px-3 py-2.5 text-[12.5px] font-bold active:scale-[.98] disabled:opacity-50', cls)}>
      {children}
    </button>
  )
}
