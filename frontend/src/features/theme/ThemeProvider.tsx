import { createContext, useContext, useEffect, type ReactNode } from 'react'
import { useLocalStorage } from '../../shared/hooks/useLocalStorage'

export type Theme = 'light' | 'dark'
export type Accent = 'green' | 'gold'

interface ThemeCtx {
  theme: Theme
  accent: Accent
  setTheme: (t: Theme) => void
  setAccent: (a: Accent) => void
  toggleTheme: () => void
}

const Ctx = createContext<ThemeCtx | null>(null)

/**
 * Writes `data-theme` / `data-accent` onto <html> (design tokens key off them)
 * and persists to the same `halyk-theme` / `halyk-accent` localStorage keys the
 * prototype used — so both apps share the chosen theme.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useLocalStorage<Theme>('halyk-theme', 'light')
  const [accent, setAccent] = useLocalStorage<Accent>('halyk-accent', 'green')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  useEffect(() => {
    document.documentElement.setAttribute('data-accent', accent)
  }, [accent])

  return (
    <Ctx.Provider
      value={{
        theme,
        accent,
        setTheme,
        setAccent,
        toggleTheme: () => setTheme(theme === 'dark' ? 'light' : 'dark'),
      }}
    >
      {children}
    </Ctx.Provider>
  )
}

export function useTheme(): ThemeCtx {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider')
  return ctx
}
