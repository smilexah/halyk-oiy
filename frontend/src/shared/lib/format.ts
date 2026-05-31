/** Money formatter — ru-RU grouping, no decimals (matches the prototype). */
export const fmt = (n: number): string => Math.round(n).toLocaleString('ru-RU')

/** Money with the ₸ suffix. */
export const tg = (n: number): string => `${fmt(n)} ₸`

/** Parse a money-ish string ("20 000 ₸") back to a number. */
export const parseAmount = (s: string): number => parseInt(s.replace(/\D/g, ''), 10) || 0

/** Russian pluralization helper. */
export const plural = (n: number, one: string, few: string, many: string): string => {
  const a = n % 10
  const b = n % 100
  if (a === 1 && b !== 11) return one
  if (a >= 2 && a <= 4 && (b < 10 || b >= 20)) return few
  return many
}
