# Maqsat frontend — multi-stage: build with Node, serve static via nginx.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
# `npm install` (not `npm ci`): the lockfile is generated on Windows and omits
# linux-only optional native deps (@emnapi/* via lightningcss), which makes the
# strict `npm ci` fail in the Linux build image.
RUN npm install --no-audit --no-fund
COPY . .
# Build-time env (VITE_*) can be injected as build args when needed:
#   docker build --build-arg VITE_AUTH_ENABLED=true ...
ARG VITE_AUTH_ENABLED
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
CMD ["nginx", "-g", "daemon off;"]
