import Button from '../../../shared/ui/Button'
import { fmt } from '../../../shared/lib/format'
import { INCOME } from '../model'

const FEATS = [
  { fi: '🧾', t: 'Обязательное — на один единый счёт' },
  { fi: '👧', t: 'Счета детям — снимают сами, лимиты у вас' },
  { fi: '📊', t: 'Свободные деньги — вы решаете, куда' },
]

export default function OnboardView({ onStart }: { onStart: () => void }) {
  return (
    <div className="mt-3.5 rounded-card bg-card p-6 text-center shadow-card">
      <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-green-soft text-3xl">💰</div>
      <h2 className="mt-4 text-[22px] font-extrabold leading-tight tracking-[-0.02em]">
        Пришла зарплата
        <br />
        {fmt(INCOME)} ₸
      </h2>
      <p className="mx-auto mt-3 max-w-[320px] text-[13px] font-medium leading-relaxed text-muted">
        Я подготовил план: открою счёт для обязательных платежей, счета детям с лимитами и покажу, сколько
        останется на жизнь. Настроим один раз — дальше я веду сам.
      </p>
      <div className="my-5 space-y-2 text-left">
        {FEATS.map((f) => (
          <div key={f.fi} className="flex items-center gap-3 rounded-2xl bg-bg px-3.5 py-3 text-[13px] font-semibold">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-green-soft text-lg">{f.fi}</span>
            {f.t}
          </div>
        ))}
      </div>
      <Button onClick={onStart}>Распределить зарплату</Button>
      <div className="mt-3 text-[11.5px] font-medium text-muted">Агент проанализировал 184 операции за 3 месяца</div>
    </div>
  )
}
