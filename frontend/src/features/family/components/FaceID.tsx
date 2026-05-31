import { useEffect, useState } from 'react'

/** Face ID scan → recognized → onComplete. Visual-only (matches the prototype). */
export default function FaceID({ open, onComplete }: { open: boolean; onComplete: () => void }) {
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (!open) {
      setDone(false)
      return
    }
    const t1 = setTimeout(() => setDone(true), 1900)
    const t2 = setTimeout(onComplete, 2850)
    return () => {
      clearTimeout(t1)
      clearTimeout(t2)
    }
  }, [open, onComplete])

  if (!open) return null

  return (
    <div className="absolute inset-0 z-[70] flex flex-col items-center justify-center gap-5 bg-bg/95 backdrop-blur">
      <div className="relative grid h-[180px] w-[180px] place-items-center">
        <span className="absolute inset-0 rounded-[36px] border-2 border-accent/30" />
        {!done && (
          <span className="absolute inset-x-3 top-3 h-0.5 animate-[scan_1.6s_ease-in-out_infinite] bg-accent shadow-[0_0_12px_var(--accent)]" />
        )}
        <span className={'text-[64px] transition-opacity ' + (done ? 'opacity-0' : 'opacity-100')}>🙂</span>
        {done && (
          <svg viewBox="0 0 100 100" className="absolute h-24 w-24 animate-[popIn_.4s_ease]">
            <circle cx="50" cy="50" r="46" fill="none" stroke="var(--accent)" strokeWidth="6" />
            <path d="M30 52 L44 66 L72 36" fill="none" stroke="var(--accent)" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        )}
      </div>
      <div className="text-[16px] font-extrabold">{done ? 'Лицо распознано' : 'Сканирование лица…'}</div>
      <div className="px-10 text-center text-[13px] text-muted">
        {done ? 'Идентификация подтверждена' : 'Подтвердите распределение зарплаты с помощью Face ID'}
      </div>
    </div>
  )
}
