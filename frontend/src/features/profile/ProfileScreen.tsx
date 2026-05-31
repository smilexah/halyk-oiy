import { useNavigate } from 'react-router-dom'
import Screen from '../../shared/ui/Screen'
import ScreenHeader from '../../shared/ui/ScreenHeader'
import Card from '../../shared/ui/Card'
import { useToast } from '../../shared/ui/toast'
import { useAuth } from '../auth/AuthProvider'

const MENU: [string, string][] = [
  ['🤖', 'Настройки AI-агента'], ['🔒', 'Безопасность'], ['🔔', 'Уведомления'],
  ['🌍', 'Язык · Тіл'], ['❓', 'Поддержка'],
]

export default function ProfileScreen() {
  const { showToast } = useToast()
  const { enabled, authenticated, username, login, logout } = useAuth()
  const navigate = useNavigate()

  const rowCls = 'flex w-full items-center gap-3.5 px-4 py-[15px] text-left active:bg-line2'

  const onExit = () => {
    if (!enabled) {
      showToast('Выйти')
      return
    }
    if (authenticated) logout()
    else login()
  }

  return (
    <Screen>
      <ScreenHeader title="Профиль" />

      <Card className="mt-[18px] flex items-center gap-[15px] p-[18px]">
        <div className="grid h-[60px] w-[60px] place-items-center rounded-[20px] bg-balance text-[26px] font-extrabold text-white">А</div>
        <div>
          <div className="text-[17px] font-extrabold">{enabled && username ? username : 'Асхат Нурланов'}</div>
          <div className="mt-0.5 text-[12.5px] text-muted">+7 777 ••• 44 17 · Премиум</div>
        </div>
      </Card>

      <Card className="mt-3.5 divide-y divide-line2">
        <button onClick={() => navigate('/family')} className={rowCls}>
          <span className="text-xl">👪</span>
          <span className="flex-1 text-[14px] font-semibold">Maqsat &amp; Family</span>
          <span className="text-[13px] font-extrabold text-accent">NEW ›</span>
        </button>
        {MENU.map(([e, label]) => (
          <button key={label} onClick={() => showToast(label)} className={rowCls}>
            <span className="text-xl">{e}</span>
            <span className="flex-1 text-[14px] font-semibold">{label}</span>
            <span className="text-muted">›</span>
          </button>
        ))}
        <button onClick={onExit} className={rowCls}>
          <span className="text-xl">🚪</span>
          <span className="flex-1 text-[14px] font-semibold">
            {enabled && !authenticated ? 'Войти' : 'Выйти'}
          </span>
          <span className="text-muted">›</span>
        </button>
      </Card>
    </Screen>
  )
}
