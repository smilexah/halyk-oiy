import type { ButtonHTMLAttributes } from 'react'
import { cn } from '../lib/cn'

type Variant = 'primary' | 'ghost' | 'soft'

const base =
  'inline-flex w-full items-center justify-center gap-2 rounded-[15px] px-4 py-4 text-[15.5px] font-extrabold tracking-[-0.01em] transition active:scale-[.975] disabled:cursor-not-allowed disabled:opacity-50'

const variants: Record<Variant, string> = {
  primary: 'bg-accent text-white glow-sm disabled:shadow-none',
  ghost: 'border-2 border-pos bg-transparent text-pos',
  soft: 'bg-accent-soft text-accent',
}

export default function Button({
  variant = 'primary',
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return <button className={cn(base, variants[variant], className)} {...props} />
}
