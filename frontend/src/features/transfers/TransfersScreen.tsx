import { useState } from 'react'
import Screen from '../../shared/ui/Screen'
import ScreenHeader from '../../shared/ui/ScreenHeader'
import Card from '../../shared/ui/Card'
import Button from '../../shared/ui/Button'
import { fmt, parseAmount } from '../../shared/lib/format'
import { useToast } from '../../shared/ui/toast'
import { cn } from '../../shared/lib/cn'

interface Contact { id: string; e: string; n: string }
const CONTACTS: Contact[] = [
  { id: 'zh', e: '👧', n: 'Жанеке' },
  { id: 'am', e: '👶', n: 'Аминош' },
  { id: 'ma', e: '🧒', n: 'Мадеке' },
  { id: 'wf', e: '💐', n: 'Айгуль' },
]

const RECENT = [
  { e: '👧', n: 'Жанеке', d: 'Вчера · Перевод дочери', v: '−20 000 ₸', pos: false },
  { e: '🧒', n: 'Мадеке', d: '3 дня назад · Перевод сыну', v: '−15 000 ₸', pos: false },
  { e: '💼', n: 'ТОО «Алтын Курылыс»', d: 'Сегодня · Зарплата', v: '+146 590 ₸', pos: true },
]

export default function TransfersScreen() {
  const { showToast } = useToast()
  const [sel, setSel] = useState<Contact | null>(null)
  const [amount, setAmount] = useState(20000)
  const [focused, setFocused] = useState(false)

  return (
    <Screen>
      <ScreenHeader title="Переводы" subtitle="Кому отправим деньги?" />

      <div className="-mx-0.5 flex gap-3.5 overflow-x-auto px-0.5 pb-1.5 pt-[18px] [scrollbar-width:none]">
        {CONTACTS.map((p) => {
          const on = sel?.id === p.id
          return (
            <button key={p.id} onClick={() => setSel(p)} className="flex w-[62px] shrink-0 flex-col items-center gap-[7px]">
              <span
                className={cn(
                  'grid h-[58px] w-[58px] place-items-center rounded-[18px] bg-card text-[25px] shadow-card transition active:scale-95',
                  on && 'ring-[2.5px] ring-gold',
                )}
              >
                {p.e}
              </span>
              <span className={cn('text-[11px] font-semibold', on ? 'text-ink' : 'text-muted')}>{p.n}</span>
            </button>
          )
        })}
        <button onClick={() => showToast('💡 Добавление получателя — скоро')} className="flex w-[62px] shrink-0 flex-col items-center gap-[7px]">
          <span className="grid h-[58px] w-[58px] place-items-center rounded-[18px] bg-gold-soft text-[28px] font-light text-gold">+</span>
          <span className="text-[11px] font-semibold text-muted">Новый</span>
        </button>
      </div>

      <Card className="mt-[18px] p-5">
        <div className="text-xs font-bold uppercase tracking-wider text-muted">Сумма перевода</div>
        <div className="my-3.5 mb-1.5 flex items-center justify-center gap-1.5">
          <input
            type="text"
            inputMode="numeric"
            value={focused ? String(amount) : fmt(amount)}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            onChange={(e) => setAmount(parseAmount(e.target.value))}
            className="w-auto max-w-[220px] bg-transparent text-center text-[42px] font-extrabold tracking-[-0.03em] tabular-nums outline-none"
          />
          <span className="text-[30px] font-bold text-muted">₸</span>
        </div>
        <div className="min-h-5 text-center text-[13.5px] font-medium text-muted">
          {sel ? (<>Получатель: <b className="font-bold text-ink">{sel.n}</b></>) : 'Выберите получателя выше'}
        </div>
        <div className="my-4 mb-1 flex justify-center gap-2">
          {[5000, 10000, 20000].map((q) => (
            <button
              key={q}
              onClick={() => setAmount((a) => a + q)}
              className="rounded-full border border-line px-3.5 py-[7px] text-[12.5px] font-bold text-muted transition hover:border-gold hover:text-gold active:scale-95"
            >
              +{fmt(q)}
            </button>
          ))}
        </div>
        <Button className="mt-4" disabled={!sel} onClick={() => sel && showToast(`✅ ${fmt(amount)} ₸ отправлено: ${sel.n}`)}>
          Перевести
        </Button>
      </Card>

      <div className="mt-[22px]">
        <h3 className="mx-0.5 mb-2.5 text-[14px] font-extrabold">Недавние переводы</h3>
        {RECENT.map((r, i) => (
          <div key={i} className="flex items-center gap-3 border-b border-line2 py-[13px] last:border-0">
            <span className="grid h-[42px] w-[42px] place-items-center rounded-[13px] bg-gold-soft text-[19px]">{r.e}</span>
            <div className="flex-1">
              <div className="text-[13.5px] font-bold">{r.n}</div>
              <div className="mt-0.5 text-[11.5px] text-muted">{r.d}</div>
            </div>
            <div className={cn('text-[14px] font-extrabold tabular-nums', r.pos ? 'text-pos' : 'text-ink')}>{r.v}</div>
          </div>
        ))}
      </div>
    </Screen>
  )
}
