import { useState } from 'react'
import { Snowflake, Copy, SlidersHorizontal } from 'lucide-react'
import Screen from '../../shared/ui/Screen'
import ScreenHeader from '../../shared/ui/ScreenHeader'
import { useToast } from '../../shared/ui/toast'
import { cn } from '../../shared/lib/cn'

const OPS = [
  { e: '⛽', n: 'АЗС Helios', d: 'Сегодня · Топливо', v: '−9 200 ₸', pos: false },
  { e: '🛒', n: 'Magnum', d: 'Вчера · Продукты', v: '−14 380 ₸', pos: false },
  { e: '💃', n: 'Студия танцев «Tomiris»', d: '12 мая · Кружок', v: '−24 000 ₸', pos: false },
  { e: '💼', n: 'Зарплата', d: 'Сегодня', v: '+146 590 ₸', pos: true },
]

export default function CardsScreen() {
  const { showToast } = useToast()
  const [frozen, setFrozen] = useState(false)

  const toggleFreeze = () => {
    const next = !frozen
    setFrozen(next)
    showToast(next ? '❄️ Карта заморожена' : '✅ Карта активна')
  }

  const actionCls =
    'flex flex-1 flex-col items-center gap-1.5 rounded-[16px] bg-card py-3 text-[11px] font-semibold shadow-card active:scale-95'

  return (
    <Screen>
      <ScreenHeader title="Карты" subtitle="Народная карта · ₸" />

      {/* bank card */}
      <div
        className={cn(
          'relative mt-5 flex aspect-[1.586/1] flex-col justify-between overflow-hidden rounded-[22px] bg-balance p-6 text-white shadow-glow transition',
          frozen && 'brightness-[.85] grayscale-[.7]',
        )}
      >
        <span className="absolute -right-8 -top-12 h-[180px] w-[180px] rounded-full bg-white/[0.12]" />
        <div className="relative flex items-start justify-between">
          <div className="h-8 w-[42px] rounded-[7px] bg-gradient-to-br from-[#FCE8B6] to-[#D9A94E]" />
          <div className="text-[15px] font-extrabold tracking-[-0.02em]">Halyk</div>
        </div>
        <div className="relative text-[21px] font-bold tracking-[0.08em] tabular-nums">4417 •••• •••• 8302</div>
        <div className="relative flex items-end justify-between">
          <div>
            <div className="text-[9px] font-semibold uppercase tracking-wider opacity-75">Держатель</div>
            <div className="mt-0.5 text-[13px] font-bold">ASKHAT NURLANOV</div>
          </div>
          <div>
            <div className="text-[9px] font-semibold uppercase tracking-wider opacity-75">Срок</div>
            <div className="mt-0.5 text-[13px] font-bold">08 / 29</div>
          </div>
        </div>
        {frozen && (
          <div className="absolute inset-0 grid place-items-center bg-black/10 text-sm font-bold">
            <span>❄️ Карта заморожена</span>
          </div>
        )}
      </div>

      <div className="mt-4 flex gap-2.5">
        <button className={cn(actionCls, frozen && 'text-accent')} onClick={toggleFreeze}>
          <Snowflake size={20} className="text-accent" />
          <span>{frozen ? 'Разморозить' : 'Заморозить'}</span>
        </button>
        <button className={actionCls} onClick={() => showToast('Реквизиты скопированы')}>
          <Copy size={20} className="text-accent" />
          <span>Реквизиты</span>
        </button>
        <button className={actionCls} onClick={() => showToast('Лимиты обновлены')}>
          <SlidersHorizontal size={20} className="text-accent" />
          <span>Лимиты</span>
        </button>
      </div>

      <div className="mt-[22px]">
        <h3 className="mx-0.5 mb-2.5 text-[14px] font-extrabold">Операции по карте</h3>
        {OPS.map((r, i) => (
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
