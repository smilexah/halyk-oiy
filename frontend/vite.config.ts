import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Frontend dev server. All API traffic goes through the Maqsat gateway (8080),
// which is the only externally-reachable backend entry point. Keycloak (8081)
// is proxied too so the OIDC login flow works from the same origin in dev.
// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/auth': { target: 'http://localhost:8081', changeOrigin: true },
    },
  },
})
