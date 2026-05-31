import type { HTMLAttributes } from 'react'
import { cn } from '../lib/cn'

/** Standard surface card: themed background, rounded, soft shadow. */
export default function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('overflow-hidden rounded-card bg-card shadow-card', className)}
      {...props}
    />
  )
}
