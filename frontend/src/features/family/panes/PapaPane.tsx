import { MEMBERS, KIDS } from '../data'
import { useFamily } from '../FamilyContext'
import { useToast } from '../../../shared/ui/toast'
import { fmt } from '../../../shared/lib/format'
import SalaryBlock from '../distribute/SalaryBlock'
import { SectionTitle, AiTag, MoreLink, Hint, Badge } from '../components/bits'

export default function PapaPane({ onInvite }: { onInvite: () => void }) {
  const { vcards, setDistributeOpen, openGoal } = useFamily()
  const { showToast } = useToast()

  return (
    <div className="animate-[fade_.28s_ease]">
      {/* wallet */}
      <div className="rounded-card bg-balance p-5 text-white shadow-glow">
        <div className="flex -space-x-2 text-lg">
          <span>👩🏻</span>
          <span>👧🏻</span>
          <span>🧒🏻</span>
        </div>
        <div className="mt-2 text-[12.5px] font-semibold opacity-90">💚 Семейный кошелёк · общий</div>
        <div className="text-[30px] font-extrabold">
          115 800 <span className="text-xl font-bold opacity-85">₸</span>
        </div>
        <div className="text-[11.5px] opacity-85">из 200 000 ₸ · выделено на месяц</div>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/25">
          <span className="block h-full rounded-full bg-white" style={{ width: '58%' }} />
        </div>
        <div className="mt-1.5 flex justify-between text-[11px] opacity-85">
          <span>Потрачено 84 200 ₸</span>
          <span>Доступ: Динара</span>
        </div>
      </div>

      <div className="mt-4">
        <SalaryBlock />
      </div>

      {/* members */}
      <SectionTitle right={<MoreLink onClick={() => showToast('⚙️ Управление ролями и правами (RBAC)')}>Управление</MoreLink>}>
        Участники группы
      </SectionTitle>
      <div className="overflow-hidden rounded-card bg-card shadow-card">
        {MEMBERS.map((m) => (
          <div key={m.name} className="flex items-center gap-3 border-b border-line2 p-3.5 last:border-0">
            <span className="grid h-10 w-10 place-items-center rounded-full bg-bg text-xl">{m.av}</span>
            <div className="flex-1">
              <div className="text-[13.5px] font-bold">{m.name}</div>
              <div className="text-[11.5px] text-muted">{m.role}</div>
            </div>
            <Badge kind={m.badge}>{m.badgeText}</Badge>
          </div>
        ))}
        <button onClick={onInvite} className="flex w-full items-center gap-3 p-3.5 text-left active:bg-line2">
          <span className="grid h-10 w-10 place-items-center rounded-full bg-accent-soft text-lg text-accent">＋</span>
          <span className="flex-1">
            <span className="block text-[13.5px] font-bold">Пригласить в семью</span>
            <span className="block text-[11px] text-muted">Без передачи логина и пароля</span>
          </span>
          <span className="text-muted">›</span>
        </button>
      </div>

      {/* virtual cards */}
      <SectionTitle right={<AiTag>AI · NBA</AiTag>}>Цели и виртуальные карты</SectionTitle>
      <Hint icon="🔒">
        Карты под цели. Деньги остаются на счёте Асхата — это <b>виртуальный слой</b> с маской лимитов.
      </Hint>
      <div className="-mx-[18px] mt-3 flex gap-3 overflow-x-auto px-[18px] pb-1 [scrollbar-width:none]">
        {vcards.map((c) => (
          <div
            key={c.num}
            className={
              'relative flex h-[124px] w-[200px] shrink-0 flex-col justify-between overflow-hidden rounded-[16px] p-4 text-white shadow-glow ' +
              (c.cls === 'gold' ? '[background:linear-gradient(135deg,#F8BE3C,#E08900)]' : 'bg-balance')
            }
          >
            <div className="flex items-center justify-between text-[10.5px] font-semibold opacity-90">
              <span className="h-5 w-7 rounded bg-white/30" />
              <span>{c.linked ? 'квитанции ✓' : 'цель'}</span>
            </div>
            <div>
              <div className="text-[14px] font-extrabold">{c.name}</div>
              <div className="text-[11px] opacity-80">•••• {c.num}</div>
            </div>
            <div className="flex items-end justify-between">
              <div>
                <div className="text-[9px] uppercase opacity-75">Баланс</div>
                <div className="text-[14px] font-extrabold">{c.bal} ₸</div>
              </div>
            </div>
          </div>
        ))}
        <button
          onClick={() => setDistributeOpen(true)}
          className="flex h-[124px] w-[150px] shrink-0 flex-col items-center justify-center gap-2 rounded-[16px] border-2 border-dashed border-line text-muted active:scale-95"
        >
          <span className="text-2xl">＋</span>
          <span className="text-center text-[12px] font-semibold">
            Открыть карту
            <br />
            под цель
          </span>
        </button>
      </div>

      {/* family goal */}
      <SectionTitle>Семейная цель</SectionTitle>
      <button onClick={() => openGoal('paris')} className="block w-full overflow-hidden rounded-card bg-card p-4 text-left shadow-card active:scale-[.99]">
        <div className="flex items-center gap-3">
          <span className="grid h-12 w-12 place-items-center rounded-2xl bg-balance text-2xl">🗼</span>
          <div>
            <div className="text-[14px] font-extrabold">Париж 2027</div>
            <div className="text-[11.5px] text-muted">Отпуск · 2 взрослых + 1 ребёнок</div>
          </div>
        </div>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-line2">
          <span className="block h-full rounded-full bg-accent" style={{ width: '62%' }} />
        </div>
        <div className="mt-1.5 flex justify-between text-[12px]">
          <span>
            <b>{fmt(740000)} ₸</b> <span className="text-muted">/ {fmt(1200000)} ₸</span>
          </span>
          <span className="font-extrabold text-accent">62%</span>
        </div>
      </button>

      {/* kids limits */}
      <SectionTitle right={<AiTag>AI-контроль</AiTag>}>Карты детей · лимиты</SectionTitle>
      <div className="overflow-hidden rounded-card bg-card shadow-card">
        {KIDS.map((k) => (
          <div key={k.name} className="flex items-center gap-3 border-b border-line2 p-3.5 last:border-0">
            <span className="grid h-10 w-10 place-items-center rounded-full bg-bg text-xl">{k.av}</span>
            <div className="flex-1">
              <div className="text-[13.5px] font-bold">{k.name}</div>
              <div className="text-[11.5px] text-muted">
                Карманные <b className="text-ink">{k.pocket} ₸</b>/мес · {k.note}
              </div>
              <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-line2">
                <span className="block h-full rounded-full bg-accent" style={{ width: `${k.usedPct}%` }} />
              </div>
            </div>
            <div className="text-right">
              <div className="text-[14px] font-extrabold">{fmt(k.limit)} ₸</div>
              <div className="text-[10px] text-muted">лимит/день</div>
            </div>
          </div>
        ))}
      </div>

      {/* mediator */}
      <SectionTitle right={<AiTag>Еженедельно</AiTag>}>AI-медиатор бюджета</SectionTitle>
      <div className="rounded-card bg-card p-4 shadow-card">
        <div className="flex items-center gap-2.5">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-green-soft text-lg">🤖</span>
          <div className="flex-1">
            <div className="text-[13.5px] font-extrabold">Резюме недели</div>
            <div className="text-[11px] text-muted">Независимый анализ · без обвинений</div>
          </div>
          <span className="rounded-full bg-accent-soft px-2 py-1 text-[10px] font-bold text-accent">NEW</span>
        </div>
        <p className="mt-3 text-[12.5px] leading-snug">
          Категория <b>«Рестораны»</b> выросла на <span className="font-bold text-neg">+25%</span> выше прогноза. Чтобы успеть накопить на <b>Париж 2027</b>, рекомендую снизить лимит на развлечения на <b>10%</b>.
        </p>
        <div className="mt-3 flex items-center gap-2 rounded-xl bg-bg p-3 text-[12px]">
          <span>🎯</span>
          <span>
            Новый лимит «Развлечения»: <b>45 000 ₸</b> → <b>40 500 ₸</b>
          </span>
        </div>
        <div className="mt-3 flex gap-2">
          <button onClick={() => showToast('✅ Лимит «Развлечения» снижен на 10%')} className="flex-1 rounded-xl bg-accent px-3 py-2.5 text-[12.5px] font-bold text-white">
            Принять рекомендацию
          </button>
          <button onClick={() => showToast('Напомним через неделю')} className="rounded-xl border border-line px-4 py-2.5 text-[12.5px] font-bold text-muted">
            Позже
          </button>
        </div>
      </div>
    </div>
  )
}
