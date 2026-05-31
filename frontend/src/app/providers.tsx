import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '../shared/api/queryClient'
import { ThemeProvider } from '../features/theme/ThemeProvider'
import { ToastProvider } from '../shared/ui/toast'
import { OverlayProvider } from '../shared/ui/overlay'
import { AuthProvider } from '../features/auth/AuthProvider'

/** Global app providers: auth (Keycloak), server-state (React Query), theme, toasts, overlays. */
export default function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <ToastProvider>
            <OverlayProvider>{children}</OverlayProvider>
          </ToastProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </AuthProvider>
  )
}
