# syntax=docker/dockerfile:1

FROM node:24-alpine AS deps
WORKDIR /app
ENV PNPM_HOME=/pnpm \
    PATH=/pnpm:$PATH
RUN corepack enable && corepack prepare pnpm@11.1.3 --activate
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

FROM node:24-alpine AS builder
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1 \
    PNPM_HOME=/pnpm \
    PATH=/pnpm:$PATH
RUN corepack enable && corepack prepare pnpm@11.1.3 --activate

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Next.js inlines NEXT_PUBLIC_* values at build time.
ARG NEXT_PUBLIC_API_BASE_URL=""
ARG NEXT_PUBLIC_DEFAULT_TENANT="demo"
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_PUBLIC_DEFAULT_TENANT=${NEXT_PUBLIC_DEFAULT_TENANT}

# public/ contains PWA icons, brand assets, service worker and offline fallback.
RUN mkdir -p public && pnpm run build

FROM node:24-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

RUN addgroup -S nodejs && adduser -S nextjs -G nodejs

COPY --from=builder --chown=nextjs:nodejs /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

CMD ["node", "server.js"]
