import { useState } from 'react'
import { createPortal } from 'react-dom'
import { usePortalContainer } from '../../../shared/ui/portalContainer'
import { useToast } from '../../../shared/ui/toast'
import { RECO_STORIES } from './stories'

const DUR = 7000 // ms per story

/** Instagram-style AI-recommendation stories. Opened from the «Посмотреть
 *  рекомендации [N]» CTA in the budget manage view. Ported from recos.js.
 *  Mounted only while visible — the parent unmounts it on close, so state
 *  resets to the first story on every open without a reset effect. */
export default function RecoStories({ onClose }: { onClose: () => void }) {
  const container = usePortalContainer()
  const { showToast } = useToast()
  const [idx, setIdx] = useState(0)
  const [paused, setPaused] = useState(false)

  const go = (n: number) => {
    if (n >= RECO_STORIES.length) {
      onClose()
      return
    }
    setPaused(false)
    setIdx(Math.max(0, n))
  }

  const st = RECO_STORIES[idx]

  const onCta = () => {
    showToast(`✅ ${st.cta} — подключаю`)
    setPaused(true)
    setTimeout(() => go(idx + 1), 250)
  }

  const body = (
    <div
      className={
        (container ? 'absolute' : 'fixed mx-auto max-w-[440px]') +
        ' inset-0 z-[140] flex flex-col overflow-hidden text-white animate-[popIn_.28s_ease]'
      }
      style={{ background: st.grad }}
      onPointerDown={() => setPaused(true)}
      onPointerUp={() => setPaused(false)}
      onPointerLeave={() => setPaused(false)}
      onPointerCancel={() => setPaused(false)}
    >
      {/* soft radial sheen */}
      <span
        className="pointer-events-none absolute inset-0 z-0 opacity-50"
        style={{
          background:
            'radial-gradient(120% 55% at 18% 12%, rgba(255,255,255,.18), transparent 60%), radial-gradient(90% 45% at 92% 95%, rgba(0,0,0,.22), transparent 60%)',
        }}
      />

      {/* segmented progress bars */}
      <div className="relative z-[5] flex gap-[5px] px-3.5 pt-[calc(12px+env(safe-area-inset-top))]">
        {RECO_STORIES.map((_, i) => (
          <span key={i} className="h-[3px] flex-1 overflow-hidden rounded-[3px] bg-white/30">
            <i
              className="block h-full rounded-[3px] bg-white"
              style={
                i < idx
                  ? { width: '100%' }
                  : i === idx
                    ? {
                        width: 0,
                        animation: `rsbar ${DUR}ms linear forwards`,
                        animationPlayState: paused ? 'paused' : 'running',
                      }
                    : { width: 0 }
              }
              onAnimationEnd={i === idx ? () => go(idx + 1) : undefined}
            />
          </span>
        ))}
      </div>

      {/* brand + close */}
      <div className="relative z-[5] flex items-center gap-[9px] px-4 pt-[13px]">
        <div className="flex items-center gap-2 text-[12.5px] font-extrabold tracking-[-0.01em]">
          <span className="grid h-[26px] w-[26px] place-items-center rounded-[8px] bg-white/20 text-sm">🤖</span>
          AI-рекомендации
          <span className="text-[11.5px] font-semibold opacity-80">
            · {idx + 1} из {RECO_STORIES.length}
          </span>
        </div>
        <button
          onClick={onClose}
          aria-label="Закрыть"
          className="ml-auto grid h-8 w-8 place-items-center rounded-full bg-white/[0.16] text-base active:scale-90"
        >
          ✕
        </button>
      </div>

      {/* tap zones (navigation) */}
      <button
        aria-label="Назад"
        onClick={() => go(idx - 1)}
        className="absolute bottom-[120px] left-0 top-16 z-[3] w-[34%]"
      />
      <button
        aria-label="Дальше"
        onClick={() => go(idx + 1)}
        className="absolute bottom-[120px] right-0 top-16 z-[3] w-[66%]"
      />

      {/* slide */}
      <div className="relative z-[2] flex flex-1 flex-col justify-center px-[26px]">
        <div key={idx} className="animate-[fade_.4s_cubic-bezier(.2,.9,.3,1)]">
          <div className="grid h-[74px] w-[74px] place-items-center rounded-[22px] bg-white/[0.16] text-[38px] shadow-[0_10px_30px_rgba(0,0,0,0.18)]">
            {st.icon}
          </div>
          <div className="mt-6 text-[11px] font-black tracking-[0.12em] opacity-80">{st.eyebrow}</div>
          <div className="mt-[9px] text-[14.5px] font-semibold leading-[1.35] opacity-95">{st.stat}</div>
          <div className="mt-5 text-[60px] font-extrabold leading-[.95] tracking-[-0.035em] tabular-nums">
            {st.big}
            <span className="ml-1 text-[30px] tracking-normal opacity-80">{st.unit}</span>
          </div>
          <div className="mt-1.5 text-[13px] font-semibold opacity-80">{st.bigsub}</div>
          <div className="mt-[26px] text-[23px] font-extrabold leading-[1.18] tracking-[-0.02em]">{st.title}</div>
          <div
            className="mt-[11px] max-w-[340px] text-[14px] font-medium leading-[1.5] opacity-95 [&>b]:font-extrabold"
            dangerouslySetInnerHTML={{ __html: st.body }}
          />
          <div className="mt-5 inline-flex items-center gap-2 self-start rounded-[30px] border border-white/[0.28] bg-white/[0.16] px-[15px] py-[9px] text-[13px] font-extrabold tracking-[-0.01em]">
            <span className="h-[7px] w-[7px] rounded-full bg-white shadow-[0_0_0_4px_rgba(255,255,255,0.22)]" />
            {st.pill}
          </div>
        </div>
      </div>

      {/* footer CTA */}
      <div className="relative z-[5] px-[18px] pb-[calc(18px+env(safe-area-inset-bottom))] pt-3.5">
        <button
          onClick={onCta}
          className="w-full rounded-[15px] bg-white px-4 py-4 text-[15px] font-extrabold tracking-[-0.01em] shadow-[0_10px_26px_rgba(0,0,0,0.2)] transition active:scale-[.98]"
          style={{ color: st.dark ? '#7A4F00' : 'var(--green-d)' }}
        >
          {st.cta}
        </button>
        <button
          onClick={onClose}
          className="mt-2.5 block w-full text-[12.5px] font-bold text-white/80"
        >
          Пропустить всё
        </button>
      </div>
    </div>
  )

  return createPortal(body, container ?? document.body)
}
