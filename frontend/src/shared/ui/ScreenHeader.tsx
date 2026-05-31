import type { ReactNode } from 'react'

/** Consistent big screen title. Includes the top safe-area inset. */
export default function ScreenHeader({
  title,
  subtitle,
  action,
}: {
  title: string
  subtitle?: ReactNode
  action?: ReactNode
}) {
  return (
    <header className="flex items-end justify-between pt-[calc(24px+env(safe-area-inset-top))] pb-1.5">
      <div>
        <h1 className="text-[27px] font-extrabold tracking-[-0.03em]">{title}</h1>
        {subtitle && <p className="mt-0.5 text-[13px] font-medium text-muted">{subtitle}</p>}
      </div>
      {action}
    </header>
  )
}
