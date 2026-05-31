import { SCEN } from './scenarios'
import { useFamily } from '../FamilyContext'
import { fmt } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'

/** Папа's salary hero + mode switch (first / power). Opens the distribution overlay. */
export default function SalaryBlock() {
  const { mode, setMode, setDistributeOpen } = useFamily()
  const scen = SCEN[mode]
  const h = scen.hero

  return (
    <>
      <div className="mt-1 flex gap-1.5 rounded-[14px] bg-line2 p-[5px]">
        {(['first', 'power'] as const).map((m) => (
          <button
            key={m}
            onClick={() => setMode(m)}
            className={cn(
              'flex flex-1 items-center justify-center gap-1.5 rounded-[10px] px-1.5 py-2.5 text-[13px] font-bold transition',
              mode === m ? 'bg-card text-ink shadow-card' : 'text-muted',
            )}
          >
            <span>{m === 'first' ? '✨' : '⚡'}</span>
            {m === 'first' ? 'Первый раз' : 'Опытный'}
          </button>
        ))}
      </div>
      <div className="mt-1.5 text-center text-[11px] text-muted">{scen.cap}</div>

      <div className="mt-3 rounded-card bg-balance p-5 text-white shadow-glow">
        {h.nba && (
          <div className="mb-2 inline-flex items-center gap-1.5 rounded-full bg-white/20 px-2.5 py-1 text-[10.5px] font-bold">
            ⚡ NBA готов
          </div>
        )}
        <div className="text-[14px] font-medium" dangerouslySetInnerHTML={{ __html: h.greet }} />
        <div className="mt-1 text-[30px] font-extrabold">
          +{fmt(scen.salary)} <span className="text-xl font-bold opacity-85">₸</span>
        </div>
        <div className="mt-3 flex items-start gap-2.5 rounded-card bg-white/15 p-3">
          <span className="text-lg">🤖</span>
          <span className="text-[12.5px] leading-snug" dangerouslySetInnerHTML={{ __html: h.tagline }} />
        </div>
        <div className="mt-2.5 inline-flex items-center gap-1.5 rounded-full bg-white/20 px-2.5 py-1 text-[10.5px] font-semibold">
          <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-white" />
          {h.pill}
        </div>
        <button
          onClick={() => setDistributeOpen(true)}
          className="mt-3.5 w-full rounded-[15px] bg-white px-4 py-3.5 text-[14.5px] font-extrabold text-accent active:scale-[.98]"
        >
          {h.cta}
        </button>
      </div>
    </>
  )
}
