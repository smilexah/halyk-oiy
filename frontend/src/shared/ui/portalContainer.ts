import { useSyncExternalStore } from 'react'

/**
 * Holds the currently-mounted device frame's `.app` element so overlays
 * (sheets, success, first-run, toast) portal *into the frame* and get clipped
 * by it — instead of escaping to the viewport. Only one DeviceFrame is mounted
 * at a time (one route), so a singleton is enough.
 */
let el: HTMLElement | null = null
const subs = new Set<() => void>()

export const portalContainer = {
  get: (): HTMLElement | null => el,
  set: (next: HTMLElement | null): void => {
    el = next
    subs.forEach((f) => f())
  },
  subscribe: (f: () => void): (() => void) => {
    subs.add(f)
    return () => subs.delete(f)
  },
}

/** Subscribe to the active portal container (re-renders when it mounts/unmounts). */
export function usePortalContainer(): HTMLElement | null {
  return useSyncExternalStore(
    portalContainer.subscribe,
    portalContainer.get,
    () => null,
  )
}
