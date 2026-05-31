import { clsx, type ClassValue } from 'clsx'

/** Conditional className join (clsx). */
export const cn = (...inputs: ClassValue[]): string => clsx(inputs)
