import type { ReactNode } from 'react'
import { cn } from '../lib/cn'

/**
 * Per-page content wrapper. Guarantees identical horizontal padding and the
 * bottom inset that clears the tab bar — this is the single place screen width
 * is defined, so every page lines up. Pages render <Screen> and never set
 * their own width/padding.
 */
export default function Screen({
  children,
  className,
  noTabBarInset,
}: {
  children: ReactNode
  className?: string
  /** Drop the bottom inset for screens without a tab bar (e.g. modals/full views). */
  noTabBarInset?: boolean
}) {
  return (
    <div
      className={cn(
        'px-[18px]',
        noTabBarInset ? 'pb-6' : 'pb-[calc(96px+env(safe-area-inset-bottom))]',
        'animate-[fade_.28s_ease]',
        className,
      )}
    >
      {children}
    </div>
  )
}
