import { useState } from 'react'
import Button from '../../../shared/ui/Button'
import { fmt } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import MandatoryCard from '../components/MandatoryCard'
import RecoCard from '../components/RecoCard'
import AiChatSheet from '../components/AiChatSheet'
import {
  INCOME,
  MONTH,
  afterMandatory,
  freeMoney,
  needOf,
  type MandatoryItem,
  type RecoItem,
} from '../model'

interface Props {
  mandatory: MandatoryItem[]
  reco: RecoItem[]
  toggleMandatory: (id: string) => void
  payMandatory: () => void
  setRecoLimit: (id: string, v: number) => void
  adjustReco: (id: string, delta: number) => void
  confirmPlan: () => void
  replay: () => void
}

export default function ManageView({
  mandatory,
  reco,
  toggleMandatory,
  payMandatory,
  setRecoLimit,
  adjustReco,
  confirmPlan,
  replay,
}: Props) {
  const [chatOpen, setChatOpen] = useState(false)

  const remain = afterMandatory(mandatory)
  const depPending = mandatory.filter((c) => c.sel && needOf(c) > 0 && !c.paid).reduce((s, c) => s + needOf(c), 0)
  const free = freeMoney(mandatory, reco)

  const blockHd = (title: string, pill: string, gold?: boolean) => (
    <div className="mb-2 mt-[22px] flex items-center justify-between first:mt-0">
      <span className="text-[15px] font-extrabold">{title}</span>
      <span className={cn('rounded-full px-2.5 py-1 text-[10.5px] font-bold', gold ? 'bg-gold-soft text-gold-d' : 'bg-green-soft text-pos')}>
        {pill}
      </span>
    </div>
  )

  return (
    <div className="mt-3.5">
      {/* agent description */}
      <div className="flex items-start gap-3 rounded-card bg-green-soft p-3.5">
        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-card text-lg">🤖</span>
        <p className="text-[12.5px] leading-snug">
          Я распределил вашу зарплату. <b>Обязательные платежи</b> закрываются с единого счёта, а остальное — <b>рекомендации</b>, куда можно потратить без отдельных счетов.
        </p>
      </div>

      {/* salary */}
      <div className="mt-3.5 rounded-card bg-balance px-5 py-4 text-white shadow-glow">
        <div className="text-[12px] font-semibold opacity-85">Зарплата · {MONTH}</div>
        <div className="text-[30px] font-extrabold tracking-[-0.02em]">
          {fmt(INCOME)} <span className="text-xl font-bold opacity-85">₸</span>
        </div>
        <div className="text-[11.5px] opacity-85">ТОО «Алтын Курылыс» · поступила сегодня</div>
      </div>

      {/* Block 1 — mandatory */}
      {blockHd('Обязательные платежи', 'единый счёт')}
      <p className="mb-3 text-[12px] font-medium leading-snug text-muted">
        Эти платежи нужно оплатить в любом случае. Для них открыт один общий счёт. Отметьте, какие пополнить из зарплаты.
      </p>
      {mandatory.map((c) => (
        <MandatoryCard key={c.id} item={c} onToggle={() => toggleMandatory(c.id)} />
      ))}

      <div className="mt-2 rounded-card bg-balance p-4 text-white shadow-glow">
        <div className="flex items-end justify-between">
          <div className="text-[12.5px] font-semibold opacity-90">
            Останется на балансе
            <span className="block text-[10.5px] opacity-75">после обязательных платежей</span>
          </div>
          <div className="text-[24px] font-extrabold">
            {fmt(remain)}
            <span className="ml-0.5 text-base opacity-85">₸</span>
          </div>
        </div>
        <button
          onClick={payMandatory}
          disabled={depPending <= 0}
          className="mt-3 w-full rounded-xl bg-white/20 px-4 py-3 text-[13.5px] font-extrabold backdrop-blur transition active:scale-[.98] disabled:opacity-80"
        >
          {depPending <= 0 ? '✓ Обязательное пополнено' : `Пополнить выбранное · ${fmt(depPending)} ₸`}
        </button>
      </div>

      {/* Block 2 — recommendations */}
      {blockHd('Рекомендации · куда потратить', 'без счёта', true)}
      <p className="mb-3 text-[12px] font-medium leading-snug text-muted">
        На основе ваших трат за 3 месяца. Отдельные счета не создаются — это ориентир. При выходе за лимит придёт уведомление, но платёж пройдёт.
      </p>
      {reco.map((c) => (
        <RecoCard key={c.id} item={c} onLimitChange={(v) => setRecoLimit(c.id, v)} />
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
              ? 'после рекомендаций — можно отложить в цель'
              : free === 0
                ? 'рекомендации укладываются в остаток'
                : 'рекомендации больше остатка — уменьшите лимиты'}
          </div>
        </div>
      </div>

      {/* AI assistant */}
      <button
        onClick={() => setChatOpen(true)}
        className="mt-3.5 flex w-full items-center gap-3 rounded-card border border-line bg-card p-3.5 text-left active:scale-[.99]"
      >
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-accent-soft text-lg">💬</span>
        <span className="flex-1">
          <span className="block text-[13px] font-bold">Сказать ассистенту, что изменить</span>
          <span className="block text-[11.5px] text-muted">«хочу меньше на еду», «урезать такси»…</span>
        </span>
        <span className="text-muted">→</span>
      </button>

      <div className="mt-3.5">
        <Button onClick={confirmPlan}>Подтвердить план рекомендаций</Button>
      </div>
      <p className="mt-2.5 text-center text-[11px] leading-snug text-muted">
        Подтверждение касается только рекомендаций. Обязательные платежи оплачиваются кнопкой «Пополнить».{' '}
        <button onClick={replay} className="text-accent underline">
          ↺ первый вход
        </button>
      </p>

      <AiChatSheet
        open={chatOpen}
        onOpenChange={setChatOpen}
        reco={reco}
        currentFree={free}
        adjustReco={adjustReco}
      />
    </div>
  )
}
