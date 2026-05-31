import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Moon, Sun } from 'lucide-react'
import Screen from '../../shared/ui/Screen'
import Section from '../../shared/ui/Section'
import { SERVICES } from './services'
import { fmt } from '../../shared/lib/format'
import { useToast } from '../../shared/ui/toast'
import { useTheme } from '../theme/ThemeProvider'
import { useOnboarding } from '../budget/onboarding'
import { useGoals } from '../../shared/api/hooks'
import BudgetPanel from '../budget/BudgetPanel'
import type { ScreenId } from '../shell/nav'

interface GoalRow {
  emoji: string
  name: string
  sub: string
  pct: number | null
  tag: string | null
}
const DEMO_GOAL_ROWS: GoalRow[] = [
  { emoji: '🏖️', name: 'Турция 2026', sub: '540 000 ₸ из 900 000 ₸ · вскладчину с друзьями', pct: 60, tag: 'личная · 60%' },
  { emoji: '💚', name: 'Семейный кошелёк', sub: '200 000 ₸/мес · доступ у Динары (Kaspi → Halyk)', pct: null, tag: null },
]

const BALANCE = 146590

const BANNERS = [
  { cls: 'from-[#0E7E5E] to-[#0A5E47]', tag: '✈️ Halyk Travel', k: '50% бонусами', t: ['Межгород', 'дешевле'], b: 'Автобусы по всему Казахстану', toast: '✈️ Halyk Travel — скидки на билеты' },
  { cls: 'from-[#7A5AF8] to-[#5B3FD6]', tag: '🛒 HalykMarket', k: 'до 28.06', t: ['+10%', 'кешбэк'], b: 'На технику и быт', toast: '🛒 HalykMarket — кешбэк до 10%' },
  { cls: 'from-[#F0A93B] to-[#E07C12]', tag: '🏦 Депозит', k: 'сберегательный', t: ['16,5%', 'годовых'], b: 'Откройте за 1 минуту', toast: '🏦 Депозит до 16,5% годовых' },
]

export default function HomeScreen({ onNavigate }: { onNavigate: (id: ScreenId) => void }) {
  const [sub, setSub] = useState<'ov' | 'fin'>('ov')
  const { showToast } = useToast()
  const { theme, toggleTheme } = useTheme()
  const { finTick } = useOnboarding()
  const navigate = useNavigate()

  // Live goals when authenticated; otherwise the demo rows.
  const { data: liveGoals } = useGoals()
  const goalRows: GoalRow[] = liveGoals?.length
    ? liveGoals.slice(0, 3).map((g) => ({
        emoji: '🎯',
        name: g.name,
        sub: `${fmt(g.allocatedAmount)} ₸ из ${fmt(g.targetAmount)} ₸`,
        pct: Math.round(g.progressPercent),
        tag: `${Math.round(g.progressPercent)}%`,
      }))
    : DEMO_GOAL_ROWS

  // After the first-run wizard finishes, land on «Мои финансы».
  useEffect(() => {
    if (finTick > 0) setSub('fin')
  }, [finTick])

  return (
    <Screen>
      {/* sticky top bar */}
      <div className="sticky top-0 z-30 -mx-[18px] flex items-center gap-[9px] bg-bg/95 px-[18px] pt-[calc(14px+env(safe-area-inset-top))] pb-2.5 backdrop-blur">
        <button
          onClick={() => onNavigate('profile')}
          className="grid h-[38px] w-[38px] shrink-0 place-items-center rounded-full bg-balance text-[13px] font-extrabold text-white active:scale-95"
          aria-label="Профиль"
        >
          АН
        </button>
        <button
          onClick={() => showToast('🔍 Поиск по продуктам и операциям')}
          className="flex h-10 flex-1 items-center gap-2 rounded-[13px] bg-card px-3.5 text-[14px] font-medium text-muted shadow-card"
        >
          <Search size={17} className="text-muted" />
          Поиск
        </button>
        <button
          onClick={() => showToast('🟢 8 240 бонусов Halyk')}
          className="flex h-10 items-center gap-1.5 rounded-[13px] bg-card px-3 text-[13px] font-extrabold shadow-card"
        >
          <span className="grid h-5 w-5 place-items-center rounded-full bg-green text-[12px] font-extrabold text-white">б</span>
          8 240
        </button>
        <button
          onClick={toggleTheme}
          className="grid h-10 w-10 place-items-center rounded-[13px] bg-card text-accent shadow-card active:scale-95"
          aria-label="Переключить тему"
        >
          {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
        </button>
      </div>

      {/* sub tabs */}
      <div className="my-1 mb-4 flex gap-1.5 rounded-[14px] bg-line2 p-[5px]">
        {(['ov', 'fin'] as const).map((s) => (
          <button
            key={s}
            onClick={() => setSub(s)}
            className={
              'flex flex-1 items-center justify-center gap-1.5 rounded-[10px] px-1.5 py-2.5 text-[13px] font-bold transition ' +
              (sub === s ? 'bg-card text-ink shadow-card' : 'text-muted')
            }
          >
            {s === 'ov' ? 'Обзор' : 'Мои финансы'}
            {s === 'fin' && (
              <span className="rounded-full bg-gold px-1.5 py-0.5 text-[9px] font-black text-white">1</span>
            )}
          </button>
        ))}
      </div>

      {sub === 'ov' ? (
        <>
          {/* balance */}
          <div className="relative overflow-hidden rounded-[22px] bg-balance px-[22px] pb-5 pt-[22px] text-white shadow-glow">
            <span className="absolute -right-10 -top-14 h-[200px] w-[200px] rounded-full bg-white/[0.13]" />
            <div className="absolute right-[22px] top-[22px] text-xs font-semibold tracking-wider opacity-85">•••• 4417</div>
            <div className="relative text-[13px] font-semibold opacity-90">Текущий баланс</div>
            <div className="relative mt-1.5 text-[38px] font-extrabold tracking-[-0.02em]">
              {fmt(BALANCE)} <span className="text-2xl font-bold opacity-85">₸</span>
            </div>
            <div className="relative mt-[18px] flex gap-2.5">
              <span className="rounded-[11px] bg-white/[0.18] px-[11px] py-[7px] text-[11.5px] font-semibold backdrop-blur">💳 Народная карта</span>
              <span className="rounded-[11px] bg-white/[0.18] px-[11px] py-[7px] text-[11.5px] font-semibold backdrop-blur">↑ +2,4% за месяц</span>
            </div>
          </div>

          {/* banners */}
          <div className="-mx-[18px] mt-4 flex snap-x snap-mandatory scroll-px-[18px] gap-[11px] overflow-x-auto px-[18px] pb-1 [scrollbar-width:none]">
            {BANNERS.map((b) => (
              <button
                key={b.tag}
                onClick={() => showToast(b.toast)}
                className={`relative flex min-h-[128px] shrink-0 basis-[84%] snap-start flex-col overflow-hidden rounded-[18px] bg-gradient-to-br ${b.cls} p-[17px] text-left text-white`}
              >
                <span className="absolute right-3.5 top-3.5 rounded-full bg-white/20 px-2 py-1 text-[10.5px] font-bold">{b.tag}</span>
                <span className="text-[11px] font-bold uppercase tracking-wider opacity-90">{b.k}</span>
                <span className="mt-auto text-[23px] font-extrabold leading-[1.05] tracking-[-0.02em]">
                  {b.t[0]}
                  <br />
                  {b.t[1]}
                </span>
                <span className="mt-1.5 text-[12px] font-medium opacity-90">{b.b}</span>
                <span className="absolute -bottom-10 -right-8 h-[130px] w-[130px] rounded-full bg-white/10" />
              </button>
            ))}
          </div>

          {/* service grid */}
          <div className="mt-4 grid grid-cols-4 gap-y-[18px] rounded-[20px] bg-card px-2 py-[18px] shadow-card">
            {SERVICES.map((s) => (
              <button key={s.l} onClick={() => showToast(s.l)} className="flex flex-col items-center gap-2 active:scale-90">
                <span className="relative grid h-[30px] w-[30px] place-items-center text-green">
                  <s.Icon size={26} strokeWidth={1.7} />
                  {s.bz && (
                    <span className={'absolute -right-3 -top-1.5 rounded-full px-1 text-[8px] font-black text-white ' + (s.bz === 'NEW' ? 'bg-gold' : 'bg-[#E0322E]')}>
                      {s.bz}
                    </span>
                  )}
                </span>
                <span className="text-center text-[11px] font-semibold leading-[1.15]">{s.l}</span>
              </button>
            ))}
          </div>

          {/* Maqsat */}
          <Section title="Halyk Maqsat" tag="Цели · Семья" />
          <p className="mx-0.5 -mt-1 mb-3 text-[12.5px] font-medium leading-relaxed text-muted">
            Общие цели, семейный кошелёк и лимиты детям. Добавляйте участников из контактов и раздавайте права.
          </p>

          <div className="mt-3.5 overflow-hidden rounded-[20px] bg-card shadow-card">
            {goalRows.map((g, i) => (
              <button key={i} onClick={() => navigate('/family')} className={'flex w-full items-center gap-[13px] px-4 py-[15px] text-left active:bg-line2' + (i > 0 ? ' border-t border-line2' : '')}>
                <span className={'grid h-11 w-11 shrink-0 place-items-center rounded-[14px] text-xl ' + (i === 0 ? 'bg-gold-soft' : 'bg-green-soft')}>{g.emoji}</span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2 text-[13.5px] font-bold">
                    {g.name}
                    {g.tag && <span className="rounded-full bg-gold-soft px-2 py-0.5 text-[10px] font-bold text-gold-d">{g.tag}</span>}
                  </span>
                  <span className="mt-0.5 block text-[11.5px] text-muted">{g.sub}</span>
                  {g.pct != null && (
                    <span className="mt-1.5 block h-1.5 overflow-hidden rounded-full bg-line2">
                      <span className="block h-full rounded-full bg-accent" style={{ width: `${g.pct}%` }} />
                    </span>
                  )}
                </span>
                <span className="text-muted">→</span>
              </button>
            ))}
            <button onClick={() => showToast('🎯 Создание цели и расчёт бюджета — в Maqsat & Family')} className="flex w-full items-center gap-[13px] border-t border-line2 px-4 py-[15px] text-left active:bg-line2">
              <span className="grid h-11 w-11 shrink-0 place-items-center rounded-[14px] bg-line2 text-xl">＋</span>
              <span className="min-w-0 flex-1">
                <span className="block text-[13.5px] font-bold">Создать цель или группу</span>
                <span className="mt-0.5 block text-[11.5px] text-muted">Агент рассчитает бюджет по вашим данным</span>
              </span>
              <span className="text-muted">→</span>
            </button>
          </div>

          <button onClick={() => navigate('/family')} className="mt-3.5 flex w-full items-center gap-3 rounded-[18px] bg-balance p-4 text-left text-white shadow-glow">
            <span className="flex -space-x-2 text-lg">
              <span>👩🏻</span>
              <span>👧🏻</span>
              <span>🧒🏻</span>
            </span>
            <span className="flex-1">
              <span className="flex items-center gap-2 text-[14px] font-extrabold">
                Maqsat &amp; Family <span className="rounded-full bg-white/25 px-1.5 py-0.5 text-[9px] font-black">NEW</span>
              </span>
              <span className="block text-[12px] opacity-90">Открыть семейный хаб целиком</span>
            </span>
            <span>→</span>
          </button>
        </>
      ) : (
        <BudgetPanel />
      )}
    </Screen>
  )
}
