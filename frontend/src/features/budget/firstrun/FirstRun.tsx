import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { usePortalContainer } from '../../../shared/ui/portalContainer'
import {
  FR_KIDS,
  FR_MANDATORY,
  FR_MONTH,
  FR_SALARY,
  FR_STEPS,
  freePool,
  segPct,
  splitSegs,
  sumMandatory,
  sumKids,
  type FrKid,
  type FrMandatory,
} from './model'
import { fmt, parseAmount } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import type { SuccessData } from '../../../shared/ui/overlay'

type Stage = 'lock' | 'analyzing' | 'wizard' | 'face'
const clone = <T,>(a: T[]): T[] => a.map((x) => ({ ...x }))

interface AcctData {
  title: string
  sub: string
  icon: string
  name: string
  type: string
  amount: number
  leftLbl: string
  rightLbl: string
}

export default function FirstRun({
  startAt,
  onClose,
  onComplete,
}: {
  startAt: 'lock' | 'wizard'
  onClose: () => void
  onComplete: (summary: SuccessData) => void
}) {
  const container = usePortalContainer()
  const [mandatory, setMandatory] = useState<FrMandatory[]>(() => clone(FR_MANDATORY))
  const [kids, setKids] = useState<FrKid[]>(() => clone(FR_KIDS))
  const [stage, setStage] = useState<Stage>(startAt === 'lock' ? 'lock' : 'analyzing')
  const [step, setStep] = useState(0) // index into FR_STEPS
  const [acct, setAcct] = useState<{ data: AcctData; next: () => void } | null>(null)

  useEffect(() => {
    if (stage !== 'analyzing') return
    const t = setTimeout(() => setStage('wizard'), 1500)
    return () => clearTimeout(t)
  }, [stage])

  const goNext = () => {
    if (step >= FR_STEPS.length - 1) setStage('face')
    else setStep((s) => s + 1)
  }

  const finish = () => {
    const opened: { n: string; v: number }[] = [{ n: 'Единый счёт · обязательное', v: sumMandatory(mandatory) }]
    kids.filter((k) => !k.skip).forEach((k) => opened.push({ n: `Счёт «${k.name}» · лимит ${fmt(k.limit)} ₸/день`, v: k.topup }))
    const total = sumMandatory(mandatory) + sumKids(kids)
    onComplete({
      title: 'Счета открыты и пополнены',
      text: 'Обязательные платежи и счета детям настроены. Дальше я веду бюджет сам — управляйте им в «Мои финансы».',
      summary: [
        { k: `Открыто и пополнено · ${FR_MONTH}`, v: String(opened.length), variant: 'hd' },
        ...opened.map((o) => ({ k: o.n, v: `${fmt(o.v)} ₸` })),
        { k: 'Свободные деньги', v: `${fmt(freePool(mandatory, kids))} ₸` },
        { k: 'Списано из зарплаты', v: `${fmt(total)} ₸`, variant: 'tot' as const },
      ],
    })
  }

  const kind = FR_STEPS[step]
  const segs = splitSegs(mandatory, kids)

  const body = (
    <div className={(container ? 'absolute' : 'fixed mx-auto max-w-[440px]') + ' inset-0 z-[80] overflow-hidden bg-bg'}>
      {stage === 'lock' && <Lock onOpen={() => setStage('analyzing')} />}
      {stage === 'analyzing' && <Analyzing />}

      {stage === 'wizard' && (
        <div className="flex h-full flex-col bg-bg">
          {/* progress + close */}
          <div className="px-[18px] pt-[calc(14px+env(safe-area-inset-top))]">
            <div className="flex gap-1.5">
              {FR_STEPS.map((_, i) => (
                <span key={i} className="h-1 flex-1 overflow-hidden rounded-full bg-line">
                  <span className={cn('block h-full rounded-full bg-accent', i < step ? 'w-full' : i === step ? 'w-1/2' : 'w-0')} />
                </span>
              ))}
            </div>
            <div className="mt-3 flex items-center justify-between">
              <button onClick={onClose} className="grid h-9 w-9 place-items-center rounded-full bg-line2 text-[17px]">✕</button>
              <span className="rounded-full bg-line2 px-3 py-1.5 text-[13px] font-extrabold text-muted">{step + 1}/{FR_STEPS.length}</span>
            </div>
          </div>

          {/* body */}
          <div className="flex-1 overflow-y-auto px-[18px] pb-3.5 pt-2 [scrollbar-width:none]">
            {kind === 'mandatory' && (
              <>
                <h1 className="mt-1.5 text-[25px] font-extrabold leading-[1.18] tracking-[-0.025em]">
                  Все важные счета — под контролем. Настройте один раз и забудьте.
                </h1>
                <div className="mt-5 flex items-center gap-3 rounded-[15px] bg-green-soft p-3.5">
                  <span className="grid h-[34px] w-[34px] place-items-center rounded-xl bg-green text-base">💳</span>
                  <span className="text-[14px] font-extrabold">Зарплата</span>
                  <span className="ml-auto text-[17px] font-extrabold text-pos">{fmt(FR_SALARY)} ₸</span>
                </div>
                <div className="mt-3.5 space-y-2.5">
                  {mandatory.map((c) => (
                    <div key={c.id} className={cn('rounded-2xl bg-card p-3.5 shadow-card transition', !c.on && 'opacity-50')}>
                      <div className="flex items-center gap-3">
                        <span className="grid h-11 w-11 place-items-center rounded-[13px] bg-green-soft text-xl">{c.emoji}</span>
                        <div className="min-w-0 flex-1">
                          <div className="truncate text-[14.5px] font-extrabold">{c.name}</div>
                          <div className="text-[11.5px] font-semibold text-muted">{c.sub}</div>
                        </div>
                        <div className="text-[16px] font-extrabold">{fmt(c.amount)} ₸</div>
                        <button
                          onClick={() => setMandatory((list) => list.map((x) => (x.id === c.id ? { ...x, on: !x.on } : x)))}
                          className={cn('grid h-7 w-7 place-items-center rounded-full border-2 text-white', c.on ? 'border-accent bg-accent' : 'border-line')}
                        >
                          {c.on ? '✓' : ''}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}

            {kind === 'summary' && (
              <>
                <h1 className="mt-1.5 text-[25px] font-extrabold leading-[1.18] tracking-[-0.025em]">
                  Обязательное — оплачено. Управляйте свободными деньгами.
                </h1>
                <div className="mt-5 rounded-[18px] bg-card p-4 shadow-card">
                  <div className="flex items-baseline justify-between">
                    <span className="text-[26px] font-extrabold tracking-[-0.025em]">{fmt(freePool(mandatory, kids))} ₸</span>
                    <span className="text-[13px] font-bold text-muted">всего свободно</span>
                  </div>
                  <div className="mt-3.5 flex h-[30px] overflow-hidden rounded-[13px] bg-line2">
                    {segs.map((s) => (
                      <span key={s.id} style={{ flex: Math.max(s.amt, 1), background: s.color }} />
                    ))}
                  </div>
                  <div className="mt-3.5 space-y-2.5">
                    {segs.map((s) => (
                      <div key={s.id} className="flex items-center gap-2.5 text-[13.5px] font-bold">
                        <span className="h-2.5 w-2.5 rounded-full" style={{ background: s.color }} />
                        {s.name}
                        <span className="ml-auto w-11 text-right text-[12px] font-semibold text-muted">{segPct(s.amt, mandatory, kids)}%</span>
                        <span className="w-20 text-right">{fmt(s.amt)} ₸</span>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="mt-3.5 flex items-center gap-3 rounded-2xl bg-balance p-4 text-white shadow-glow">
                  <span className="grid h-[42px] w-[42px] place-items-center rounded-[13px] bg-white/20 text-xl">📈</span>
                  <div>
                    <div className="text-[14px] font-extrabold">Свободно {fmt(segs.find((s) => s.id === 'free')?.amt ?? 0)} ₸</div>
                    <div className="text-[12px] opacity-90">Куда направим? Депозит 16,5% · копилка на цель</div>
                  </div>
                </div>
              </>
            )}

            {(kind === 'kid0' || kind === 'kid1') && (
              <Kid kids={kids} setKids={setKids} idx={kind === 'kid0' ? 0 : 1} segs={segs} mandatory={mandatory} />
            )}
          </div>

          {/* footer */}
          <div className="bg-bg pb-[calc(14px+env(safe-area-inset-bottom))] pt-3 shadow-[0_-8px_22px_rgba(0,0,0,0.05)]">
            {kind === 'mandatory' && (
              <>
                <div className="mb-2.5 flex justify-end px-[18px]">
                  <span className="rounded-full bg-line2 px-4 py-2 text-[13px] font-bold text-muted">
                    Остаток: <b className="text-ink">{fmt(FR_SALARY - sumMandatory(mandatory))} ₸</b>
                  </span>
                </div>
                <FootBtn
                  onClick={() => {
                    const cnt = mandatory.filter((c) => c.on).length
                    setAcct({
                      data: {
                        title: 'Единый счёт создан',
                        sub: `Обязательные платежи объединены на один счёт и пополнены из зарплаты на ${FR_MONTH}.`,
                        icon: '🧾',
                        name: 'Единый счёт · обязательное',
                        type: 'депозитный',
                        amount: sumMandatory(mandatory),
                        leftLbl: `${cnt} ${cnt === 1 ? 'платёж' : 'платежа'} привязано`,
                        rightLbl: 'пополнено на 100%',
                      },
                      next: goNext,
                    })
                  }}
                >
                  Открыть счёт и пополнить
                </FootBtn>
              </>
            )}
            {kind === 'summary' && (
              <>
                <FootBtn onClick={goNext}>Продолжить</FootBtn>
                <div className="mt-2 text-center text-[10.5px] font-extrabold tracking-[0.13em] text-muted/70">ДАЛЕЕ — СЧЕТА ДЕТЯМ</div>
              </>
            )}
            {(kind === 'kid0' || kind === 'kid1') && (
              <KidFooter
                kids={kids}
                setKids={setKids}
                idx={kind === 'kid0' ? 0 : 1}
                onOpen={(k) =>
                  setAcct({
                    data: {
                      title: `Счёт «${k.name}» открыт`,
                      sub: `Карта с дневным лимитом ${fmt(k.limit)} ₸. ${k.name} тратит сама, превышение вы подтверждаете.`,
                      icon: k.emoji,
                      name: `Счёт «${k.name}»`,
                      type: 'детская карта',
                      amount: k.topup,
                      leftLbl: `лимит ${fmt(k.limit)} ₸/день`,
                      rightLbl: 'пополнено на 100%',
                    },
                    next: goNext,
                  })
                }
                onSkip={goNext}
              />
            )}
          </div>
        </div>
      )}

      {stage === 'face' && <Face onDone={finish} />}

      {acct && (
        <AccountCreated
          data={acct.data}
          onContinue={() => {
            const n = acct.next
            setAcct(null)
            n()
          }}
        />
      )}
    </div>
  )

  return createPortal(body, container ?? document.body)
}

/* ── kid step body ──────────────────────────────────────────── */
function Kid({
  kids,
  setKids,
  idx,
  segs,
  mandatory,
}: {
  kids: FrKid[]
  setKids: React.Dispatch<React.SetStateAction<FrKid[]>>
  idx: number
  segs: { name: string; color: string; amt: number }[]
  mandatory: FrMandatory[]
}) {
  const k = kids[idx]
  const LIMITS = [1000, 2000, 5000, 10000]
  const set = (patch: Partial<FrKid>) => setKids((list) => list.map((x, i) => (i === idx ? { ...x, ...patch } : x)))

  // Impact chart: base needs/free segments + a red «Вычет» segment for this kid's top-up.
  const topup = k.skip ? 0 : k.topup
  const base = segs.map((s) => ({ name: s.name, color: s.color, amt: s.amt, deduct: false }))
  const impactSegs =
    topup > 0 ? [...base, { name: `Вычет · ${k.name}`, amt: topup, color: '#C2443B', deduct: true }] : base
  const impactTotal = impactSegs.reduce((s, x) => s + x.amt, 0)

  return (
    <>
      <h1 className="mt-1.5 text-[25px] font-extrabold leading-[1.18] tracking-[-0.025em]">{k.why}</h1>
      <div className="mt-5 flex flex-col items-center">
        <div className="grid h-[84px] w-[84px] place-items-center rounded-full bg-green-soft text-[42px] shadow-glow">{k.emoji}</div>
        <div className="mt-3 text-[18px] font-extrabold">{k.name}</div>
        <div className="text-[12px] font-semibold text-muted">{k.role}</div>
      </div>
      <Field label="Сумма пополнения">
        <MoneyInput value={k.topup} onChange={(v) => set({ topup: v })} />
      </Field>
      <Field label="Дневной лимит трат">
        <MoneyInput value={k.limit} onChange={(v) => set({ limit: v })} />
        <div className="mt-2.5 flex flex-wrap gap-2">
          {LIMITS.map((l) => (
            <button
              key={l}
              onClick={() => set({ limit: l })}
              className={cn('rounded-xl border-[1.5px] px-3.5 py-2 text-[13px] font-bold', k.limit === l ? 'border-accent bg-accent text-white' : 'border-line text-muted')}
            >
              {fmt(l)} ₸
            </button>
          ))}
        </div>
      </Field>
      <div className="mt-3.5 rounded-2xl bg-card p-3.5 shadow-card">
        <div className="text-[12px] font-bold text-muted">Влияние на свободный бюджет</div>
        <div className="mt-2.5 flex h-4 overflow-hidden rounded-lg bg-line2">
          {impactSegs.map((s) => (
            <span key={s.name} style={{ flex: Math.max(s.amt, 1), background: s.color }} />
          ))}
        </div>
        <div className="mt-3 grid grid-cols-2 gap-x-3 gap-y-1.5">
          {impactSegs.map((s) => (
            <div key={s.name} className="flex items-center gap-1.5 text-[11.5px] font-semibold">
              <span className="h-2.5 w-2.5 rounded-full" style={{ background: s.color }} />
              {s.name}
              <span className="ml-auto text-[11px] font-extrabold text-ink">
                {impactTotal > 0 ? Math.round((s.amt / impactTotal) * 100) : 0}%
              </span>
              <span className="font-bold text-muted">
                {s.deduct ? '−' : ''}
                {fmt(s.amt)}
              </span>
            </div>
          ))}
        </div>
      </div>
      {/* keep mandatory referenced for impact recompute */}
      <span className="hidden">{sumMandatory(mandatory)}</span>
    </>
  )
}

function KidFooter({
  kids,
  setKids,
  idx,
  onOpen,
  onSkip,
}: {
  kids: FrKid[]
  setKids: React.Dispatch<React.SetStateAction<FrKid[]>>
  idx: number
  onOpen: (k: FrKid) => void
  onSkip: () => void
}) {
  return (
    <>
      <FootBtn
        onClick={() => {
          setKids((list) => list.map((x, i) => (i === idx ? { ...x, skip: false } : x)))
          onOpen(kids[idx])
        }}
      >
        Открыть счёт и пополнить
      </FootBtn>
      <button
        onClick={() => {
          setKids((list) => list.map((x, i) => (i === idx ? { ...x, skip: true } : x)))
          onSkip()
        }}
        className="mt-2 w-full py-1.5 text-[13px] font-bold text-muted"
      >
        Не сейчас — остальное в депозит
      </button>
    </>
  )
}

function FootBtn({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button onClick={onClick} className="w-full rounded-[15px] bg-accent px-4 py-4 text-[15.5px] font-extrabold text-white glow-sm active:scale-[.975]" style={{ marginInline: 18, width: 'calc(100% - 36px)' }}>
      {children}
    </button>
  )
}

/* ── account-created overlay ────────────────────────────────── */
function AccountCreated({ data, onContinue }: { data: AcctData; onContinue: () => void }) {
  const [fill, setFill] = useState(0)
  const last4 = useState(() => String(1000 + Math.floor(Math.random() * 8999)).slice(-4))[0]
  useEffect(() => {
    const t = setTimeout(() => setFill(100), 160)
    return () => clearTimeout(t)
  }, [])
  return (
    <div className="absolute inset-0 z-[90] flex flex-col justify-end bg-[rgba(8,16,13,0.55)] p-[18px] backdrop-blur-md animate-[fade_.3s_ease]">
      <div className="rounded-[24px] bg-bg p-5 pb-[calc(16px+env(safe-area-inset-bottom))] shadow-float animate-[sheetUp_.42s_cubic-bezier(.2,1,.3,1)]">
        <div className="mx-auto mb-3 grid h-[54px] w-[54px] place-items-center rounded-full bg-green-soft text-2xl">✓</div>
        <div className="text-center text-[19px] font-extrabold tracking-[-0.02em]">{data.title}</div>
        <div className="mt-1 text-center text-[12.5px] font-semibold leading-snug text-muted">{data.sub}</div>

        <div className="relative mt-4 overflow-hidden rounded-[20px] p-[17px] text-white shadow-glow [background:linear-gradient(135deg,#0b3a2c,#0f5a41_52%,#1b7d5c)]">
          <span className="absolute -right-8 -top-10 h-[150px] w-[150px] rounded-full bg-white/[0.09]" />
          <div className="relative flex items-center gap-2.5">
            <span className="grid h-[34px] w-[34px] place-items-center rounded-[10px] bg-white/[0.18] text-[17px]">{data.icon}</span>
            <div className="flex-1">
              <div className="text-[13.5px] font-extrabold">{data.name}</div>
              <div className="text-[11px] font-semibold opacity-80">•• {last4} · Halyk</div>
            </div>
            <span className="rounded-full border border-white/35 px-2.5 py-1 text-[9.5px] font-black uppercase tracking-wider opacity-85">{data.type}</span>
          </div>
          <div className="relative mt-[15px] text-[28px] font-extrabold tracking-[-0.03em]">
            {fmt(data.amount)} <span className="text-[17px] opacity-85">₸</span>
          </div>
          <div className="relative mt-3">
            <div className="h-[7px] overflow-hidden rounded-[5px] bg-white/25">
              <span className="block h-full rounded-[5px] bg-white transition-[width] duration-700" style={{ width: `${fill}%` }} />
            </div>
            <div className="mt-1.5 flex justify-between gap-2 text-[10.5px] font-bold opacity-90">
              <span>{data.leftLbl}</span>
              <span>{data.rightLbl}</span>
            </div>
          </div>
        </div>

        <button onClick={onContinue} className="mt-[18px] w-full rounded-[15px] bg-accent px-4 py-4 text-[15.5px] font-extrabold text-white glow-sm active:scale-[.975]">
          Продолжить
        </button>
      </div>
    </div>
  )
}

/* ── lockscreen ─────────────────────────────────────────────── */
function Lock({ onOpen }: { onOpen: () => void }) {
  return (
    <div
      onClick={onOpen}
      className="flex h-full cursor-pointer flex-col px-[18px] pb-[calc(20px+env(safe-area-inset-bottom))] pt-14 text-white [background:linear-gradient(160deg,#0b3a2c,#0f5a41_38%,#1b7d5c_64%,#3aa17a)]"
    >
      <div className="flex items-center gap-1.5 text-[12px] font-semibold opacity-90">🔒 заблокировано</div>
      <div className="mt-7 text-center">
        <div className="text-[74px] font-bold leading-none tracking-[-0.03em]">10:42</div>
        <div className="mt-2 text-[16px] font-semibold opacity-90">Понедельник, 2 июня</div>
      </div>
      <div className="mt-8 animate-[fade_.5s_ease] rounded-[22px] border border-white/50 bg-white/80 p-4 text-[#13201b] shadow-float backdrop-blur-xl">
        <div className="mb-2 flex items-center gap-2">
          <span className="grid h-5 w-5 place-items-center rounded bg-green text-[11px]">🏦</span>
          <span className="text-[11px] font-extrabold uppercase tracking-wider text-[#3a4a44]">Halyk Bank</span>
          <span className="ml-auto text-[11px] font-semibold text-[#6a7a73]">сейчас</span>
        </div>
        <div className="text-[15.5px] font-extrabold">Зарплата! 🎉</div>
        <div className="mt-1 text-[13px] font-medium leading-snug text-[#34433d]">
          Деньги уже на счету. Нажмите — распределю на главное, чтобы не уйти в минус к концу месяца.
        </div>
        <div className="mt-2.5 inline-flex items-center gap-1.5 text-[12px] font-extrabold text-green">
          Распределить с агентом <span>→</span>
        </div>
      </div>
      <div className="mt-auto text-center text-[11.5px] font-bold tracking-[0.14em] opacity-85">
        НАЖМИТЕ, ЧТОБЫ ОТКРЫТЬ
        <span className="mt-1.5 block animate-[fade_1.6s_ease-in-out_infinite]">⌃</span>
      </div>
    </div>
  )
}

function Analyzing() {
  return (
    <div className="flex h-full flex-col items-center justify-center px-8 text-center">
      <div className="grid h-[76px] w-[76px] animate-pulse place-items-center rounded-[22px] bg-green-soft text-4xl">🧠</div>
      <h2 className="mt-4 text-[20px] font-extrabold tracking-[-0.02em]">Готовлю план распределения…</h2>
      <p className="mt-2 text-[13px] font-medium leading-relaxed text-muted">
        Смотрю обязательные платежи, регулярные переводы детям и средние траты за 3 месяца.
      </p>
    </div>
  )
}

function Face({ onDone }: { onDone: () => void }) {
  const [ok, setOk] = useState(false)
  useEffect(() => {
    const t1 = setTimeout(() => setOk(true), 1500)
    const t2 = setTimeout(onDone, 2150)
    return () => {
      clearTimeout(t1)
      clearTimeout(t2)
    }
  }, [onDone])
  return (
    <div className="absolute inset-0 z-[95] flex flex-col items-center justify-center gap-5 bg-[rgba(8,16,13,0.72)] text-white backdrop-blur">
      <div className={cn('relative grid h-32 w-32 place-items-center overflow-hidden rounded-[34px] border-[3px]', ok ? 'border-[#3aa17a]' : 'border-white/25')}>
        {!ok && <span className="absolute inset-x-2 top-3 h-0.5 animate-[scan_1.1s_ease-in-out_infinite] rounded bg-[#3aa17a] shadow-[0_0_12px_#3aa17a]" />}
        <span className="text-[58px]">{ok ? '✅' : '🙂'}</span>
      </div>
      <h3 className="text-[17px] font-extrabold">{ok ? 'Готово' : 'Подтвердите по Face ID'}</h3>
      <p className="-mt-3 text-[12.5px] opacity-80">Открываем счета и пополняем</p>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="mt-3.5 rounded-2xl bg-card p-3.5 shadow-card">
      <label className="text-[12px] font-bold text-muted">{label}</label>
      {children}
    </div>
  )
}
function MoneyInput({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  const [focused, setFocused] = useState(false)
  return (
    <div className="mt-2 flex items-center gap-1.5 rounded-xl bg-line2 px-3.5 py-3 focus-within:ring-2 focus-within:ring-accent">
      <input
        inputMode="numeric"
        value={focused ? String(value) : fmt(value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        onChange={(e) => onChange(parseAmount(e.target.value))}
        className="min-w-0 flex-1 bg-transparent text-[19px] font-extrabold tabular-nums outline-none"
      />
      <span className="text-[15px] font-bold text-muted">₸</span>
    </div>
  )
}
