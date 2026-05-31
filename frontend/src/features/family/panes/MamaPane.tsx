import { OFFERS } from '../data'
import { useToast } from '../../../shared/ui/toast'
import { useOverlay } from '../../../shared/ui/overlay'
import { SectionTitle, AiTag, Hint } from '../components/bits'
import { useOffers } from '../../../shared/api/hooks'
import { offerToCard } from '../../../shared/api/view'

export default function MamaPane() {
  const { showToast } = useToast()
  const { showSuccess } = useOverlay()
  const { data: liveOffers } = useOffers()
  const offers = liveOffers?.length ? liveOffers.map(offerToCard) : OFFERS

  return (
    <div className="animate-[fade_.28s_ease]">
      <div className="rounded-card bg-balance p-5 text-white shadow-glow">
        <div className="text-[12.5px] font-semibold opacity-90">💚 Семейный кошелёк · доступ</div>
        <div className="text-[30px] font-extrabold">
          115 800 <span className="text-xl font-bold opacity-85">₸</span>
        </div>
        <div className="text-[11.5px] opacity-85">выделил Асхат · продукты и быт</div>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/25">
          <span className="block h-full rounded-full bg-white" style={{ width: '58%' }} />
        </div>
        <div className="mt-1.5 flex justify-between text-[11px] opacity-85">
          <span>Лимит 200 000 ₸ / мес</span>
          <span>Обновится 1 июня</span>
        </div>
      </div>

      <Hint icon="🔗">
        Вы присоединились по приглашению Асхата и привязали свою карту <b>Kaspi Gold</b>. Деньги тратятся из семейного кошелька Halyk.
      </Hint>

      {/* salary greeting */}
      <div className="mt-3.5 rounded-card bg-card p-4 shadow-card">
        <div className="text-[13px]">
          Привет, <b>Динара</b> 👋 пришла зарплата <b>+185 000 ₸</b>
        </div>
        <div className="mt-2 flex items-start gap-2.5 rounded-card bg-green-soft p-3">
          <span className="text-lg">🤖</span>
          <span className="text-[12.5px] leading-snug">
            Общие счета Асхат уже закрывает — пополнять необязательно. Хотите часть отправить в семейную цель или создать <b>личную</b>?
          </span>
        </div>
        <div className="mt-3 flex gap-2">
          <button onClick={() => showToast('💚 +30 000 ₸ в семейный кошелёк')} className="flex-1 rounded-xl bg-accent-soft px-3 py-2.5 text-[12.5px] font-bold text-accent">
            ＋ В общий кошелёк · 30 000 ₸
          </button>
          <button onClick={() => showToast('Хорошо — Асхат закроет общие счета')} className="rounded-xl border border-line px-4 py-2.5 text-[12.5px] font-bold text-muted">
            Пропустить
          </button>
        </div>
      </div>

      {/* personal goal */}
      <SectionTitle right={<AiTag>только ваша</AiTag>}>Личная цель</SectionTitle>
      <div className="rounded-card bg-card p-4 shadow-card">
        <div className="flex items-center gap-3">
          <span className="grid h-12 w-12 place-items-center rounded-2xl bg-gold-soft text-2xl">👗</span>
          <div>
            <div className="text-[14px] font-extrabold">Платье для Аминош</div>
            <div className="text-[11.5px] text-muted">Личная цель · не видна остальным</div>
          </div>
        </div>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-line2">
          <span className="block h-full rounded-full bg-accent" style={{ width: '35%' }} />
        </div>
        <div className="mt-1.5 flex justify-between text-[12px]">
          <span>
            <b>14 000 ₸</b> <span className="text-muted">/ 40 000 ₸</span>
          </span>
          <span className="font-extrabold text-accent">35%</span>
        </div>
        <button
          onClick={() => showSuccess({ title: 'Отложено в личную цель', text: '+12 000 ₸ на «Платье для Аминош» 👗 Цель видите только вы.' })}
          className="mt-3 w-full rounded-xl bg-accent px-4 py-3 text-[13.5px] font-extrabold text-white glow-sm active:scale-[.98]"
        >
          Отложить из зарплаты
        </button>
      </div>

      {/* offers */}
      <SectionTitle right={<AiTag>AI</AiTag>}>Спец-предложения агента</SectionTitle>
      <div className="space-y-2">
        {offers.map((o) => (
          <div key={o.a} className="flex items-center gap-3 rounded-card bg-card p-3.5 shadow-card">
            <span className="text-xl">{o.e}</span>
            <div className="flex-1">
              <div className="text-[13px] font-bold">{o.a}</div>
              <div className="text-[11.5px] text-muted">{o.b}</div>
            </div>
            <button onClick={() => showToast('Предложение активировано')} className="rounded-full bg-accent-soft px-3 py-1.5 text-[12px] font-bold text-accent">
              {o.cta}
            </button>
          </div>
        ))}
      </div>

      {/* contribution to paris */}
      <SectionTitle>Ваш вклад в цель «Париж 2027»</SectionTitle>
      <div className="rounded-card bg-card p-4 shadow-card">
        <div className="flex items-center gap-3">
          <span className="grid h-12 w-12 place-items-center rounded-2xl bg-balance text-2xl">🗼</span>
          <div>
            <div className="text-[14px] font-extrabold">Париж 2027</div>
            <div className="text-[11.5px] text-muted">Собрано 740 000 ₸ из 1 200 000 ₸</div>
          </div>
        </div>
        <div className="mt-3 space-y-1.5">
          <div className="flex items-center gap-2 rounded-xl bg-bg p-3 text-[12.5px]">
            <span>💸</span>
            <span className="flex-1">
              Ваше правило: <b>весь кэшбэк</b> идёт в цель
            </span>
            <span className="rounded bg-accent-soft px-1.5 py-0.5 text-[10px] font-bold text-accent">ВКЛ</span>
          </div>
          <div className="flex items-center gap-2 rounded-xl bg-bg p-3 text-[12.5px]">
            <span>📈</span>
            <span>
              Накоплено кэшбэком: <b>8 400 ₸</b>
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}
