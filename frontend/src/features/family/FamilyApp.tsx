import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Info } from 'lucide-react'
import DeviceFrame from '../../shared/ui/DeviceFrame'
import Sheet from '../../shared/ui/Sheet'
import Button from '../../shared/ui/Button'
import { useOverlay } from '../../shared/ui/overlay'
import { cn } from '../../shared/lib/cn'
import { FamilyProvider, useFamily } from './FamilyContext'
import type { Role } from './data'
import PapaPane from './panes/PapaPane'
import MamaPane from './panes/MamaPane'
import ChildPane from './panes/ChildPane'
import PushHost from './components/PushHost'
import DistributeOverlay from './distribute/DistributeOverlay'
import GoalDetailOverlay from './goaldetail/GoalDetailOverlay'

const ROLES: { role: Role; av: string; nm: string; rl: string; crown?: boolean }[] = [
  { role: 'papa', av: '👨🏻', nm: 'Асхат', rl: 'Папа', crown: true },
  { role: 'mama', av: '👩🏻', nm: 'Динара', rl: 'Мама' },
  { role: 'child', av: '🧒🏻', nm: 'Мадеке', rl: 'Сын · 14' },
]

function FamilyShell() {
  const navigate = useNavigate()
  const { showSuccess } = useOverlay()
  const { role, setRole, distributeOpen, goalKey } = useFamily()
  const [inviteOpen, setInviteOpen] = useState(false)
  const [plaqueOpen, setPlaqueOpen] = useState(false)
  const [inviteRole, setInviteRole] = useState<'coparent' | 'child'>('coparent')

  return (
    <DeviceFrame>
      {/* top bar */}
      <header className="flex items-center gap-3 px-[18px] pb-3 pt-[calc(16px+env(safe-area-inset-top))]">
        <button onClick={() => navigate('/')} className="grid h-9 w-9 place-items-center rounded-full bg-card shadow-card" aria-label="Назад в Halyk">
          <ChevronLeft size={20} />
        </button>
        <div className="flex-1">
          <div className="text-[17px] font-extrabold">
            Maqsat <span className="text-gold">&amp; Family</span>
          </div>
          <div className="text-[11.5px] text-muted">Семья Нурлановых · 5 участников</div>
        </div>
        <button onClick={() => setPlaqueOpen(true)} className="grid h-9 w-9 place-items-center rounded-full bg-card text-muted shadow-card" aria-label="Как это работает">
          <Info size={18} />
        </button>
      </header>

      {/* role switcher */}
      <div className="flex gap-2 px-[18px] pb-3">
        {ROLES.map((r) => (
          <button
            key={r.role}
            onClick={() => setRole(r.role)}
            className={cn(
              'relative flex flex-1 flex-col items-center gap-0.5 rounded-2xl border-2 py-2.5 transition',
              role === r.role ? 'border-accent bg-accent-soft' : 'border-transparent bg-card shadow-card',
            )}
          >
            {r.crown && <span className="absolute -top-1.5 right-2 text-[11px]">👑</span>}
            <span className="text-xl">{r.av}</span>
            <span className="text-[12px] font-bold">{r.nm}</span>
            <span className="text-[10px] text-muted">{r.rl}</span>
          </button>
        ))}
      </div>

      {/* body */}
      <main className="flex-1 overflow-y-auto overflow-x-hidden px-[18px] pb-6 [scrollbar-width:none]">
        {role === 'papa' && <PapaPane onInvite={() => setInviteOpen(true)} />}
        {role === 'mama' && <MamaPane />}
        {role === 'child' && <ChildPane />}
      </main>

      <PushHost />

      {distributeOpen && <DistributeOverlay />}
      {goalKey && <GoalDetailOverlay goalKey={goalKey} />}

      {/* invite sheet */}
      <Sheet open={inviteOpen} onOpenChange={setInviteOpen} title="Пригласить в семью">
        <p className="mt-1 text-[12.5px] text-muted">Отправьте ссылку родственнику. Передавать логин и пароль не нужно — каждый входит под собой.</p>
        <div className="mt-3 space-y-1.5">
          {([
            { v: 'coparent', i: '👩🏻', a: 'Со-родитель', b: 'Доступ к кошельку, цели, лимитам' },
            { v: 'child', i: '🧒🏻', a: 'Ребёнок', b: 'Карта с лимитом, под контролем' },
          ] as const).map((o) => (
            <button
              key={o.v}
              onClick={() => setInviteRole(o.v)}
              className={cn('flex w-full items-center gap-3 rounded-card border p-3 text-left', inviteRole === o.v ? 'border-accent bg-accent-soft' : 'border-line')}
            >
              <span className="text-xl">{o.i}</span>
              <span className="flex-1">
                <span className="block text-[13px] font-bold">{o.a}</span>
                <span className="block text-[11px] text-muted">{o.b}</span>
              </span>
              <span className={cn('h-4 w-4 rounded-full border-2', inviteRole === o.v ? 'border-accent bg-accent' : 'border-line')} />
            </button>
          ))}
        </div>
        <div className="mt-4 flex items-start gap-2.5 rounded-card bg-bg p-3 text-[11.5px] text-muted">
          <span>🔒</span>
          <span>
            <b className="text-ink">Виртуальный слой.</b> Приглашённый получает виртуальную карту с маской лимитов через API — ваш счёт не меняется.
          </span>
        </div>
        <Button
          className="mt-4"
          onClick={() => {
            setInviteOpen(false)
            setTimeout(() => showSuccess({ title: 'Ссылка готова', text: 'Отправьте её родственнику в WhatsApp или SMS. Он войдёт под собой и получит виртуальную карту.' }), 250)
          }}
        >
          Сгенерировать ссылку-приглашение
        </Button>
      </Sheet>

      {/* plaque sheet */}
      <Sheet open={plaqueOpen} onOpenChange={setPlaqueOpen} title="Как это работает">
        <p className="mt-1 text-[12.5px] text-muted">Архитектура «виртуального слоя» — без переписывания процессинга банка.</p>
        <div className="mt-3 space-y-2">
          {[
            ['💳', <>Деньги физически остаются на основном счёте Асхата.</>],
            ['🎭', <>Карты семьи — виртуальные карты с программной маской лимитов через API.</>],
            ['⚡', <>Авторизация транзакции проверяется на уровне приложения — процессинг не меняется.</>],
            ['🤝', <>AI-медиатор переводит конфликты из эмоций в рациональные рекомендации.</>],
          ].map(([e, t], i) => (
            <div key={i} className="flex items-start gap-2.5 rounded-card bg-bg p-3 text-[12.5px]">
              <span className="text-base">{e as string}</span>
              <span>{t}</span>
            </div>
          ))}
        </div>
        <Button variant="soft" className="mt-4" onClick={() => setPlaqueOpen(false)}>
          Понятно
        </Button>
      </Sheet>
    </DeviceFrame>
  )
}

export default function FamilyApp() {
  return (
    <FamilyProvider>
      <FamilyShell />
    </FamilyProvider>
  )
}
