/**
 * Holds the current access token (JWT) in memory. Wired to Keycloak OIDC
 * later; until then it stays null and the apiClient sends no Authorization
 * header. Kept out of React state on purpose so non-React code (the api
 * client) can read it synchronously.
 */
let accessToken: string | null = null

export const tokenStore = {
  get: (): string | null => accessToken,
  set: (token: string | null): void => {
    accessToken = token
  },
  clear: (): void => {
    accessToken = null
  },
}
