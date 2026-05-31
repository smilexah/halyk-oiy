import type { ReactNode } from 'react'
import { cn } from '../../../shared/lib/cn'

export function SectionTitle({ children, right }: { children: ReactNode; right?: ReactNode }) {
  return (
    <div className="mt-6 mb-2.5 flex items-center justify-between text-[15px] font-extrabold">
      <span>{children}</span>
      {right}
    </div>
  )
}

export function AiTag({ children }: { children: ReactNode }) {
  return <span className="rounded-full bg-accent-soft px-2 py-1 text-[10.5px] font-bold text-accent">{children}</span>
}

export function MoreLink({ children, onClick }: { children: ReactNode; onClick?: () => void }) {
  return (
    <button onClick={onClick} className="text-[12px] font-bold text-accent">
      {children}
    </button>
  )
}

export function Hint({ icon, children }: { icon: string; children: ReactNode }) {
  return (
    <p className="mt-3 flex items-start gap-2.5 rounded-card bg-card p-3.5 text-[12px] font-medium leading-snug text-muted shadow-card">
      <span className="text-base">{icon}</span>
      <span>{children}</span>
    </p>
  )
}

export function ProgressBar({ pct, className }: { pct: number; className?: string }) {
  return (
    <div className={cn('h-1.5 overflow-hidden rounded-full bg-line2', className)}>
      <span className="block h-full rounded-full bg-accent" style={{ width: `${Math.min(100, pct)}%` }} />
    </div>
  )
}

const BADGE: Record<string, string> = {
  owner: 'bg-accent-soft text-accent',
  kaspi: 'bg-gold-soft text-gold-d',
  child: 'bg-line2 text-muted',
}

export function Badge({ kind, children }: { kind: string; children: ReactNode }) {
  return <span className={cn('rounded-full px-2 py-1 text-[10.5px] font-bold', BADGE[kind] ?? BADGE.child)}>{children}</span>
}
