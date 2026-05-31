import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import Keycloak from 'keycloak-js'
import { authConfig } from './config'
import { tokenStore } from '../../shared/auth/tokenStore'

interface AuthCtx {
  enabled: boolean
  ready: boolean
  authenticated: boolean
  username: string | null
  login: () => void
  logout: () => void
}

const Ctx = createContext<AuthCtx>({
  enabled: false,
  ready: true,
  authenticated: false,
  username: null,
  login: () => {},
  logout: () => {},
})

/**
 * Initializes Keycloak (Authorization Code + PKCE, `check-sso` so it never
 * forces a login redirect) and keeps `tokenStore` in sync so the apiClient
 * sends a fresh bearer. When auth is disabled it's a transparent pass-through.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ ready: boolean; authenticated: boolean; username: string | null }>({
    ready: !authConfig.enabled,
    authenticated: false,
    username: null,
  })
  const kc = useRef<Keycloak | null>(null)

  useEffect(() => {
    if (!authConfig.enabled || kc.current) return
    const keycloak = new Keycloak({ url: authConfig.url, realm: authConfig.realm, clientId: authConfig.clientId })
    kc.current = keycloak

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).then(() => tokenStore.set(keycloak.token ?? null)).catch(() => tokenStore.clear())
    }

    keycloak
      .init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      .then((authenticated) => {
        if (authenticated) tokenStore.set(keycloak.token ?? null)
        setState({
          ready: true,
          authenticated,
          username: (keycloak.tokenParsed?.preferred_username as string) ?? null,
        })
      })
      .catch(() => {
        // Keycloak unreachable — degrade gracefully to unauthenticated.
        setState({ ready: true, authenticated: false, username: null })
      })
  }, [])

  const value: AuthCtx = {
    enabled: authConfig.enabled,
    ready: state.ready,
    authenticated: state.authenticated,
    username: state.username,
    login: () => kc.current?.login(),
    logout: () => {
      tokenStore.clear()
      kc.current?.logout({ redirectUri: window.location.origin })
    },
  }

  if (!state.ready) {
    return (
      <div className="grid min-h-dvh place-items-center bg-bg text-muted">
        <span className="h-7 w-7 animate-spin rounded-full border-2 border-line border-t-accent" />
      </div>
    )
  }

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useAuth(): AuthCtx {
  return useContext(Ctx)
}
