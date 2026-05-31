import Screen from '../../shared/ui/Screen'
import ScreenHeader from '../../shared/ui/ScreenHeader'
import { useToast } from '../../shared/ui/toast'

const PAYS: [string, string][] = [
  ['🏠', 'Коммуналка'], ['📱', 'Связь'], ['🌐', 'Интернет'], ['🎓', 'Образование'],
  ['💡', 'Свет'], ['💧', 'Вода'], ['🔥', 'Газ'], ['🚗', 'Штрафы'],
  ['📺', 'ТВ'], ['🏦', 'Кредиты'], ['🛡️', 'Страховка'], ['➕', 'Ещё'],
]

const AUTOPAY = [
  { e: '🏠', n: 'Коммуналка и квитанции', d: 'Ежемесячно · 1 число', v: '32 590 ₸' },
  { e: '📱', n: 'Мобильная связь', d: 'Ежемесячно · 5 число', v: '4 990 ₸' },
]

export default function PaymentsScreen() {
  const { showToast } = useToast()

  return (
    <Screen>
      <ScreenHeader title="Платежи" subtitle="Оплата услуг и счетов" />

      <div className="mt-[18px] grid grid-cols-4 gap-2.5">
        {PAYS.map(([e, label]) => (
          <button
            key={label}
            onClick={() => showToast(`${label} — скоро`)}
            className="flex flex-col items-center gap-2 rounded-[16px] bg-card py-3.5 text-[11px] font-semibold shadow-card active:scale-95"
          >
            <span className="text-2xl">{e}</span>
            <span>{label}</span>
          </button>
        ))}
      </div>

      <div className="mt-[22px]">
        <h3 className="mx-0.5 mb-2.5 text-[14px] font-extrabold">Автоплатежи</h3>
        {AUTOPAY.map((r) => (
          <div key={r.n} className="flex items-center gap-3 border-b border-line2 py-[13px] last:border-0">
            <span className="grid h-[42px] w-[42px] place-items-center rounded-[13px] bg-gold-soft text-[19px]">{r.e}</span>
            <div className="flex-1">
              <div className="text-[13.5px] font-bold">{r.n}</div>
              <div className="mt-0.5 text-[11.5px] text-muted">{r.d}</div>
            </div>
            <div className="text-[14px] font-extrabold tabular-nums">{r.v}</div>
          </div>
        ))}
      </div>
    </Screen>
  )
}
