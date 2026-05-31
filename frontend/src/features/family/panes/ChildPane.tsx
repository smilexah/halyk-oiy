import { CHILD_HISTORY } from '../data'
import { useFamily } from '../FamilyContext'
import { SectionTitle, Hint } from '../components/bits'
import { cn } from '../../../shared/lib/cn'

export default function ChildPane() {
  const { childToday, payResult, startSOS, requestApproval } = useFamily()

  return (
    <div className="animate-[fade_.28s_ease]">
      {/* junior card */}
      <div className="relative flex h-[180px] flex-col justify-between overflow-hidden rounded-[20px] bg-balance p-5 text-white shadow-glow">
        <div className="flex items-center justify-between">
          <span className="h-7 w-10 rounded bg-white/30" />
          <div className="text-right text-[11px]">
            Детская карта
            <b className="block text-[13px]">Halyk Junior</b>
          </div>
        </div>
        <div>
          <div className="text-[16px] font-extrabold">Мадеке Нурланов</div>
          <div className="text-[12px] opacity-80">•••• 6203</div>
        </div>
        <div className="flex items-end justify-between">
          <div>
            <div className="text-[10px] uppercase opacity-75">Доступно сегодня</div>
            <div className="text-[20px] font-extrabold">{childToday}</div>
          </div>
          <div className="text-[11px] opacity-80">Лимит 2 000 ₸/день</div>
        </div>
      </div>

      <Hint icon="👛">
        Карманные на месяц: <b>15 000 ₸</b> · потрачено 9 400 ₸. Дневной лимит устанавливает папа.
      </Hint>

      {/* payment zone */}
      <SectionTitle>Оплата</SectionTitle>
      <div className="rounded-card bg-card p-4 shadow-card">
        <div className="flex items-center gap-3">
          <span className="grid h-[46px] w-[46px] place-items-center rounded-[13px] bg-gold-soft text-[23px]">🍽️</span>
          <div className="flex-1">
            <div className="text-[14.5px] font-extrabold">Школьная столовая №14</div>
            <div className="mt-0.5 text-[12px] font-medium text-muted">Комплексный обед</div>
          </div>
          <div className="text-[18px] font-extrabold">2 500 ₸</div>
        </div>

        {payResult === 'idle' && (
          <button onClick={startSOS} className="mt-3.5 w-full rounded-[15px] bg-gold px-4 py-3.5 text-[14.5px] font-extrabold text-white active:scale-[.98]">
            Оплатить картой · 2 500 ₸
          </button>
        )}

        {payResult === 'declined' && (
          <div className="mt-3.5">
            <Declined icon="🚫" title="Недостаточно средств" sub="Дневной лимит 2 000 ₸ · обед 2 500 ₸ · не хватает 500 ₸" />
            <button onClick={requestApproval} className="mt-3 w-full rounded-[15px] bg-accent px-4 py-3.5 text-[14.5px] font-extrabold text-white glow-sm active:scale-[.98]">
              🙋 Запросить у папы 500 ₸
            </button>
          </div>
        )}

        {payResult === 'denied' && (
          <div className="mt-3.5">
            <Declined icon="🚫" title="Папа отклонил запрос" sub="Попробуйте позже или сегодня без обеда 😕" />
            <button onClick={requestApproval} className="mt-3 w-full rounded-[15px] bg-accent px-4 py-3.5 text-[14.5px] font-extrabold text-white glow-sm active:scale-[.98]">
              🙋 Запросить снова
            </button>
          </div>
        )}

        {payResult === 'paid' && (
          <div className="mt-3.5">
            <Declined ok icon="✅" title="Обед оплачен · 2 500 ₸" sub="Папа одобрил +500 ₸ разово. Спасибо!" />
          </div>
        )}
      </div>

      {/* history */}
      <SectionTitle>История по карте</SectionTitle>
      <div className="overflow-hidden rounded-card bg-card shadow-card">
        {CHILD_HISTORY.map((h, i) => (
          <div key={i} className="flex items-center gap-3 border-b border-line2 p-3.5 last:border-0">
            <span className="grid h-10 w-10 place-items-center rounded-full bg-bg text-lg">{h.e}</span>
            <div className="flex-1">
              <div className="text-[13.5px] font-bold">{h.n}</div>
              <div className="text-[11.5px] text-muted">{h.d}</div>
            </div>
            <span className={cn('text-[14px] font-extrabold', h.pos ? 'text-pos' : 'text-ink')}>{h.v}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function Declined({ icon, title, sub, ok }: { icon: string; title: string; sub: string; ok?: boolean }) {
  return (
    <div className={cn('flex items-center gap-3 rounded-card p-3.5', ok ? 'bg-green-soft' : 'bg-neg/10')}>
      <span className="text-xl">{icon}</span>
      <div className={cn('text-[13px] font-bold', ok ? 'text-pos' : 'text-neg')}>
        {title}
        <span className="block text-[11.5px] font-medium text-muted">{sub}</span>
      </div>
    </div>
  )
}
