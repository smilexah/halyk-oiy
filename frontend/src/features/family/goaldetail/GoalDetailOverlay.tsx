import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { GOALS, PERM_PI, PERM_LABEL, type GoalContact, type GoalDef, type Perm } from './goals'
import Sheet from '../../../shared/ui/Sheet'
import { useFamily, type GoalKey } from '../FamilyContext'
import { useToast } from '../../../shared/ui/toast'
import { useOverlay } from '../../../shared/ui/overlay'
import { fmt, parseAmount } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'

const deepClone = (g: GoalDef): GoalDef => ({
  ...g,
  members: g.members.map((m) => ({ ...m })),
  rules: g.rules.map((r) => ({ ...r })),
  contacts: g.contacts.map((c) => ({ ...c })),
})

export default function GoalDetailOverlay({ goalKey }: { goalKey: GoalKey }) {
  const { closeGoal } = useFamily()
  const { showToast } = useToast()
  const { showSuccess } = useOverlay()

  const [goal, setGoal] = useState<GoalDef>(() => deepClone(GOALS[goalKey]))
  const [topupOpen, setTopupOpen] = useState(false)
  const [memberOpen, setMemberOpen] = useState(false)

  const pct = Math.min(100, Math.round((goal.collected / goal.target) * 100))
  const left = Math.max(0, goal.target - goal.collected)

  const toggleRule = (i: number) =>
    setGoal((g) => {
      const rules = g.rules.map((r, j) => (j === i ? { ...r, on: !r.on } : r))
      showToast(rules[i].on ? `✅ Правило включено: ${rules[i].t}` : 'Правило выключено')
      return { ...g, rules }
    })

  const topup = (v: number) => {
    setTopupOpen(false)
    setGoal((g) => {
      const collected = Math.min(g.target, g.collected + v)
      const members = g.members.map((m, i) => (i === 0 ? { ...m, amount: m.amount + v } : m))
      const newPct = Math.round((collected / g.target) * 100)
      setTimeout(
        () => showSuccess({ title: 'Цель пополнена', text: `+${fmt(v)} ₸ на «${g.name}» ${g.emoji} Уже ${newPct}% — вы на шаг ближе к отпуску!` }),
        250,
      )
      return { ...g, collected, members }
    })
  }

  const addMember = (contact: GoalContact | null, name: string, perm: Exclude<Perm, 'owner'>) => {
    const display = name || contact?.name
    if (!display) {
      showToast('Выберите контакт')
      return
    }
    setMemberOpen(false)
    setGoal((g) => ({
      ...g,
      members: [...g.members, { av: contact?.av ?? '🧑🏻', name: display, perm, permLabel: PERM_LABEL[perm], amount: 0 }],
    }))
    setTimeout(
      () => showSuccess({ title: 'Приглашение отправлено', text: `${display} получит ссылку. После входа доступ к цели: «${PERM_LABEL[perm]}».` }),
      250,
    )
  }

  return (
    <div className="absolute inset-0 z-[55] flex flex-col bg-bg">
      {/* header */}
      <div className="flex items-center gap-3 border-b border-line px-[18px] pb-3 pt-[calc(16px+env(safe-area-inset-top))]">
        <button onClick={closeGoal} className="grid h-9 w-9 place-items-center rounded-full bg-card shadow-card" aria-label="Назад">
          <ArrowLeft size={18} />
        </button>
        <div className="flex-1">
          <div className="text-[16px] font-extrabold">{goal.name}</div>
          <div className="text-[11.5px] text-muted">{goal.titleSub}</div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-[18px] pb-4 [scrollbar-width:none]">
        {/* hero */}
        <div className="mt-4 rounded-card bg-balance p-5 text-white shadow-glow">
          <div className="text-3xl">{goal.emoji}</div>
          <div className="mt-2 text-[20px] font-extrabold">{goal.name}</div>
          <div className="text-[12px] opacity-85">{goal.heroSub}</div>
          <div className="mt-3 text-[26px] font-extrabold">
            {fmt(goal.collected)} <span className="text-[15px] font-bold opacity-80">/ {fmt(goal.target)} ₸</span>
          </div>
          <div className="mt-2 h-2 overflow-hidden rounded-full bg-white/25">
            <span className="block h-full rounded-full bg-white" style={{ width: `${pct}%` }} />
          </div>
          <div className="mt-1.5 flex justify-between text-[11.5px] opacity-90">
            <span>{pct}% собрано</span>
            <span>осталось {fmt(left)} ₸</span>
          </div>
        </div>

        {/* stats */}
        <div className="mt-3 grid grid-cols-3 gap-2">
          {[
            ['Собрано', `${fmt(goal.collected)} ₸`, true],
            ['В месяц', goal.perMonth, false],
            ['Срок', goal.term, false],
          ].map(([k, v, acc]) => (
            <div key={k as string} className="rounded-card bg-card p-3 text-center shadow-card">
              <div className="text-[10.5px] font-semibold uppercase tracking-wide text-muted">{k}</div>
              <div className={cn('mt-1 text-[14px] font-extrabold', acc && 'text-accent')}>{v}</div>
            </div>
          ))}
        </div>

        {/* ai */}
        <div className="mt-3.5 flex items-start gap-2.5 rounded-card bg-green-soft p-3.5">
          <span className="text-lg">🤖</span>
          <span className="text-[12.5px] leading-snug" dangerouslySetInnerHTML={{ __html: goal.ai }} />
        </div>

        {/* members */}
        <div className="mt-6 mb-2.5 flex items-center justify-between text-[15px] font-extrabold">
          <span>{goal.memberSec}</span>
          <button onClick={() => showToast('⚙️ Права: просмотр · пополнять · пополнять и снимать')} className="text-[12px] font-bold text-accent">
            Права доступа
          </button>
        </div>
        <div className="overflow-hidden rounded-card bg-card shadow-card">
          {goal.members.map((m, i) => (
            <div key={i} className="flex items-center gap-3 border-b border-line2 p-3.5 last:border-0">
              <span className="grid h-10 w-10 place-items-center rounded-full bg-bg text-xl">{m.av}</span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1.5 text-[13.5px] font-bold">
                  {m.name}
                  {m.you && <span className="rounded bg-accent-soft px-1.5 py-0.5 text-[10px] font-bold text-accent">вы</span>}
                </div>
                <div className="text-[11.5px] text-muted">
                  {PERM_PI[m.perm]} {m.permLabel}
                </div>
              </div>
              <div className="text-right">
                <div className="text-[13.5px] font-extrabold">{fmt(m.amount)} ₸</div>
                <div className="text-[10.5px] text-muted">внёс</div>
              </div>
            </div>
          ))}
          <button onClick={() => setMemberOpen(true)} className="flex w-full items-center gap-3 p-3.5 text-left active:bg-line2">
            <span className="grid h-10 w-10 place-items-center rounded-full bg-accent-soft text-lg text-accent">＋</span>
            <span className="flex-1">
              <span className="block text-[13.5px] font-bold">Добавить из контактов</span>
              <span className="block text-[11px] text-muted">{goalKey === 'turkey' ? 'Друзья и кто угодно · права на доступ' : 'Выдать права: просмотр · пополнять · снимать'}</span>
            </span>
            <span className="text-muted">›</span>
          </button>
        </div>

        {/* rules */}
        <div className="mt-6 mb-2.5 text-[15px] font-extrabold">Проактивные правила агента</div>
        <div className="overflow-hidden rounded-card bg-card shadow-card">
          {goal.rules.map((r, i) => (
            <div key={i} className="flex items-center gap-3 border-b border-line2 p-3.5 last:border-0">
              <span className="text-lg">{r.e}</span>
              <div className="flex-1">
                <div className="text-[13px]">
                  <b>{r.who}:</b> {r.t}
                </div>
                <div className={cn('text-[11.5px]', r.pos ? 'text-pos' : 'text-muted')}>{r.sub}</div>
              </div>
              <button
                onClick={() => toggleRule(i)}
                className={cn('relative h-6 w-11 rounded-full transition', r.on ? 'bg-accent' : 'bg-line')}
                role="switch"
                aria-checked={r.on}
              >
                <span className={cn('absolute top-0.5 h-5 w-5 rounded-full bg-white transition-all', r.on ? 'left-[22px]' : 'left-0.5')} />
              </button>
            </div>
          ))}
        </div>

        {/* offer */}
        <div className="mt-3.5 flex items-center gap-3 rounded-card bg-gold-soft p-3.5">
          <span className="text-xl">🎁</span>
          <div className="flex-1">
            <div className="text-[13px] font-bold text-gold-d">Идёте по графику!</div>
            <div className="text-[11.5px] text-muted">{goal.offer}</div>
          </div>
          <button onClick={() => showToast('Промокод сохранён 🎉')} className="rounded-full bg-gold px-3 py-1.5 text-[12px] font-bold text-white">
            Забрать
          </button>
        </div>
      </div>

      {/* footer */}
      <div className="flex gap-2.5 border-t border-line bg-card px-[18px] pb-[calc(14px+env(safe-area-inset-bottom))] pt-3">
        <button onClick={() => setMemberOpen(true)} className="rounded-[15px] border-2 border-pos px-5 py-4 text-[14px] font-extrabold text-pos">
          ＋ Участник
        </button>
        <button onClick={() => setTopupOpen(true)} className="flex-1 rounded-[15px] bg-accent px-4 py-4 text-[15.5px] font-extrabold text-white glow-sm active:scale-[.975]">
          Пополнить цель
        </button>
      </div>

      <TopupSheet open={topupOpen} onOpenChange={setTopupOpen} goalName={goal.name} onConfirm={topup} />
      <MemberSheet open={memberOpen} onOpenChange={setMemberOpen} goal={goal} onConfirm={addMember} />
    </div>
  )
}

/* ── top-up sheet ──────────────────────────────────────────────── */
function TopupSheet({
  open,
  onOpenChange,
  goalName,
  onConfirm,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  goalName: string
  onConfirm: (v: number) => void
}) {
  const [amount, setAmount] = useState(25000)
  const [focused, setFocused] = useState(false)
  const CHIPS = [10000, 25000, 50000, 100000]

  return (
    <Sheet open={open} onOpenChange={onOpenChange} title={`Пополнить «${goalName}»`}>
      <p className="mt-1 text-[12.5px] text-muted">Деньги уходят на сберегательный счёт цели под 16.5%. Списание с вашей карты Halyk.</p>
      <div className="mt-4 grid grid-cols-4 gap-2">
        {CHIPS.map((c) => (
          <button
            key={c}
            onClick={() => setAmount(c)}
            className={cn('rounded-xl border py-2.5 text-[12.5px] font-bold', amount === c ? 'border-accent bg-accent-soft text-accent' : 'border-line text-muted')}
          >
            {fmt(c)}
          </button>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-center gap-1.5 rounded-card bg-bg py-3">
        <input
          inputMode="numeric"
          value={focused ? String(amount) : fmt(amount)}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          onChange={(e) => setAmount(parseAmount(e.target.value))}
          className="w-[140px] bg-transparent text-center text-[28px] font-extrabold tabular-nums outline-none"
        />
        <span className="text-[20px] font-bold text-muted">₸</span>
      </div>
      <button
        onClick={() => amount > 0 && onConfirm(amount)}
        className="mt-4 w-full rounded-[15px] bg-accent px-4 py-4 text-[15.5px] font-extrabold text-white glow-sm active:scale-[.975]"
      >
        Пополнить на {fmt(amount)} ₸
      </button>
    </Sheet>
  )
}

/* ── add-member sheet ──────────────────────────────────────────── */
function MemberSheet({
  open,
  onOpenChange,
  goal,
  onConfirm,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  goal: GoalDef
  onConfirm: (contact: GoalContact | null, name: string, perm: Exclude<Perm, 'owner'>) => void
}) {
  const [picked, setPicked] = useState<GoalContact | null>(null)
  const [name, setName] = useState('')
  const [perm, setPerm] = useState<Exclude<Perm, 'owner'>>('view')

  const PERMS: { v: Exclude<Perm, 'owner'>; e: string; a: string; b: string }[] = [
    { v: 'view', e: '👁️', a: 'Только просмотр', b: 'Видит прогресс, не трогает деньги' },
    { v: 'topup', e: '➕', a: 'Может пополнять', b: 'Вносит деньги в цель' },
    { v: 'full', e: '🔁', a: 'Пополнять и снимать', b: 'Полный доступ к средствам цели' },
  ]

  return (
    <Sheet open={open} onOpenChange={onOpenChange} title="Добавить участника">
      <p className="mt-1 text-[12.5px] text-muted">{goal.memberSub}</p>
      <div className="mt-3 text-[11px] font-bold uppercase tracking-wide text-muted">Контакт</div>
      <div className="mt-2 space-y-1.5">
        {goal.contacts.map((c, i) => (
          <button
            key={i}
            onClick={() => {
              setPicked(c)
              setName(c.name)
            }}
            className={cn('flex w-full items-center gap-3 rounded-card border p-2.5 text-left', picked?.name === c.name ? 'border-accent bg-accent-soft' : 'border-line')}
          >
            <span className="grid h-9 w-9 place-items-center rounded-full bg-bg text-lg">{c.av}</span>
            <span className="flex-1">
              <span className="block text-[13px] font-bold">{c.name}</span>
              <span className="block text-[11px] text-muted">{c.phone}</span>
            </span>
            <span className={cn('h-4 w-4 rounded-full border-2', picked?.name === c.name ? 'border-accent bg-accent' : 'border-line')} />
          </button>
        ))}
      </div>

      <div className="mt-3 text-[11px] font-bold uppercase tracking-wide text-muted">Отображать как</div>
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Имя участника"
        className="mt-2 w-full rounded-xl border border-line bg-bg px-3.5 py-3 text-[14px] outline-none focus:border-accent"
      />

      <div className="mt-3 text-[11px] font-bold uppercase tracking-wide text-muted">Права доступа</div>
      <div className="mt-2 space-y-1.5">
        {PERMS.map((p) => (
          <button
            key={p.v}
            onClick={() => setPerm(p.v)}
            className={cn('flex w-full items-center gap-3 rounded-card border p-2.5 text-left', perm === p.v ? 'border-accent bg-accent-soft' : 'border-line')}
          >
            <span className="text-lg">{p.e}</span>
            <span className="flex-1">
              <span className="block text-[13px] font-bold">{p.a}</span>
              <span className="block text-[11px] text-muted">{p.b}</span>
            </span>
            <span className={cn('h-4 w-4 rounded-full border-2', perm === p.v ? 'border-accent bg-accent' : 'border-line')} />
          </button>
        ))}
      </div>

      <button
        onClick={() => onConfirm(picked, name.trim(), perm)}
        className="mt-4 w-full rounded-[15px] bg-accent px-4 py-4 text-[15.5px] font-extrabold text-white glow-sm active:scale-[.975]"
      >
        Отправить приглашение
      </button>
    </Sheet>
  )
}
