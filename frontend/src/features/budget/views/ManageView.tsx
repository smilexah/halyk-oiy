import Button from '../../../shared/ui/Button'
import { fmt } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import MandatoryCard from '../components/MandatoryCard'
import RecoCard from '../components/RecoCard'
import { freeMoney, mainBalance, needOf, type MandatoryItem, type RecoItem } from '../model'

interface Props {
  mandatory: MandatoryItem[]
  reco: RecoItem[]
  recoFunded: boolean
  toggleMandatory: (id: string) => void
  payMandatory: () => void
  setRecoLimit: (id: string, v: number) => void
  confirmPlan: () => void
  replay: () => void
}

export default function ManageView({
  mandatory,
  reco,
  recoFunded,
  toggleMandatory,
  payMandatory,
  setRecoLimit,
  confirmPlan,
  replay,
}: Props) {
  const depPending = mandatory.filter((c) => c.sel && needOf(c) > 0 && !c.paid).reduce((s, c) => s + needOf(c), 0)
  const free = freeMoney(mandatory, reco)

  const blockHd = (title: string, pill: string, gold?: boolean) => (
    <div className="mb-2.5 flex items-center justify-between">
      <span className="text-[15px] font-extrabold">{title}</span>
      <span className={cn('rounded-full px-2.5 py-1 text-[10.5px] font-bold', gold ? 'bg-gold-soft text-gold-d' : 'bg-green-soft text-pos')}>
        {pill}
      </span>
    </div>
  )

  return (
    <div className="mt-3.5">
      {/* main account balance (sticky) */}
      <div className="sticky top-[58px] z-20 my-0.5 overflow-hidden rounded-[18px] bg-balance px-[18px] py-4 text-white shadow-glow">
        <div className="text-[12px] font-semibold opacity-90">Баланс основного счёта</div>
        <div className="text-[30px] font-extrabold tracking-[-0.02em]">
          {fmt(mainBalance(mandatory, reco))} <span className="text-xl font-bold opacity-85">₸</span>
        </div>
        <div className="text-right text-[11.5px] opacity-85">Основная карта ··5617</div>
      </div>

      {/* Block 1 — mandatory (own account) */}
      <div className="mt-4">
        {blockHd('Обязательные платежи', 'отдельный счёт')}
        {mandatory.map((c) => (
          <MandatoryCard key={c.id} item={c} onToggle={() => toggleMandatory(c.id)} />
        ))}
        <div
          className={cn(
            'sticky bottom-[calc(70px+env(safe-area-inset-bottom))] z-[25] -mx-[18px] mt-1 bg-bg px-[18px] pb-2 pt-2',
            depPending <= 0 && 'text-center',
          )}
        >
          <button
            onClick={payMandatory}
            disabled={depPending <= 0}
            className={cn(
              'rounded-[13px] px-4 py-3.5 text-[14px] font-extrabold transition active:scale-[.98]',
              depPending <= 0
                ? 'inline-block w-auto rounded-full bg-green-soft px-6 py-2.5 text-pos'
                : 'w-full bg-accent text-white glow-sm',
            )}
          >
            {depPending <= 0 ? '✓ Обязательное пополнено' : `Пополнить выбранное · ${fmt(depPending)} ₸`}
          </button>
        </div>
      </div>

      {/* Block 2 — monthly needs (main account) */}
      <div className="mt-5">
        {blockHd('Ежемесячные потребности', 'основной счёт', true)}
        {reco.map((c) => (
          <RecoCard key={c.id} item={c} funded={recoFunded} onLimitChange={(v) => setRecoLimit(c.id, v)} />
        ))}

        <div
          className={cn(
            'mt-2 flex items-center gap-3 rounded-card p-4',
            free < 0 ? 'bg-neg text-white' : 'bg-green-soft',
          )}
        >
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-white/30 text-lg">
            {free > 0 ? '🎯' : free === 0 ? '✅' : '⚠️'}
          </span>
          <div className="flex-1">
            <div className="text-[13.5px] font-bold">
              {free > 0 ? `Свободно ${fmt(free)} ₸` : free === 0 ? 'Распределено полностью' : `Превышение на ${fmt(-free)} ₸`}
            </div>
            <div className={cn('text-[11.5px]', free < 0 ? 'opacity-90' : 'text-muted')}>
              {free > 0
                ? 'после потребностей — можно отложить в цель'
                : free === 0
                  ? 'потребности укладываются в остаток'
                  : 'потребности больше остатка — уменьшите лимиты'}
            </div>
          </div>
        </div>

        <div className="sticky bottom-[calc(70px+env(safe-area-inset-bottom))] z-[25] -mx-[18px] mt-3.5 bg-bg px-[18px] pb-2 pt-2">
          <Button onClick={confirmPlan}>Посмотреть рекомендаций</Button>
        </div>
      </div>

      <p className="mt-2.5 text-center text-[11px] leading-snug text-muted">
        Обязательные платежи оплачиваются кнопкой «Пополнить».{' '}
        <button onClick={replay} className="text-accent underline">
          ↺ первый вход
        </button>
      </p>
    </div>
  )
}
