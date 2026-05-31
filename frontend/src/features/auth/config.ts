/**
 * Keycloak / auth configuration from Vite env vars. Auth is OFF by default so
 * the app runs without a backend; flip VITE_AUTH_ENABLED=true once Keycloak is
 * reachable. The realm is `maqsat` (see backend docs); create a public SPA
 * client (e.g. `maqsat-web`) with PKCE + this origin in redirect URIs.
 */
export const authConfig = {
  enabled: import.meta.env.VITE_AUTH_ENABLED === 'true',
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'maqsat',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'maqsat-app',
}
