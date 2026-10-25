# vivent-front

Next.js frontend for Vivent.

Detailed frontend documentation is maintained in:

```text
../docs/FRONTEND.md
```

Any frontend change that modifies routes, feature structure, API calls, query keys, permission gating, forms, translations, theme behavior or validation commands must update that file in the same task.

## Current Stack

```text
Next.js 16.2.6
React 19.2.4
TypeScript
Tailwind CSS v4
pnpm 11.1.3
Radix UI
Lucide React
TanStack Query
TanStack Table
React Hook Form
Zod
@zxing/browser
next-intl
Custom ThemeProvider
sonner
PWA manifest + online-first service worker
```

## Runtime Notes

Use Node.js 24 LTS compatible runtime:

```text
engines.node = >=24 <25
packageManager = pnpm@11.1.3
```

Use pnpm through Corepack:

```bash
corepack enable
corepack prepare pnpm@11.1.3 --activate
pnpm install
```

The app stores auth tokens in `sessionStorage`.

The app uses `src/components/providers/theme-provider.tsx` for theme handling. Do not switch back to `next-themes` runtime without validating hydration and build behavior.

The production build registers `public/sw.js` through `src/components/providers/pwa-service-worker.tsx`. The service worker caches static assets and `offline.html`, but leaves `/api/*` requests network-only so private inventory, auth, FIFO and report data stays backend-owned.

For mobile development on the local LAN, `next.config.ts` allows `192.168.50.94` in `allowedDevOrigins`. Start dev with a LAN listener, for example `pnpm dev --hostname 0.0.0.0`, then open `http://192.168.50.94:3000` from the phone. When `NEXT_PUBLIC_API_BASE_URL` is not set and the app is opened from a private LAN IP, the API client automatically calls the backend on the same host at port `8080`, for example `http://192.168.50.94:8080`.

In production, when `NEXT_PUBLIC_API_BASE_URL` is left empty, the API client uses same-origin requests such as `/api/v1/...`. This is the correct setup behind nginx for domains like `https://demo.corpoviva.com`.

PWA install/service-worker behavior still requires a production build on HTTPS or another secure context.

The mobile POS camera scanner uses `@zxing/browser` and the device camera. Camera access on phones requires HTTPS or another secure browser context; plain HTTP LAN development can load the UI but may not open the camera.

## Validation

From `vivent-front/`:

```bash
pnpm audit
pnpm run lint
pnpm exec tsc --noEmit --pretty false
NEXT_PRIVATE_BUILD_WORKER=1 pnpm run build
```

In the current local Next.js setup, the plain workerized build can hang at `Running TypeScript ...`. `pnpm exec tsc --noEmit --pretty false` and the build with `NEXT_PRIVATE_BUILD_WORKER=1` are the validated path.

## Implemented Routes

```text
/
/login
/dashboard
/inventory
/purchases
/purchases/new
/purchases/[id]
/sales
/sales/new
/sales/[id]
/transfers
/transfers/new
/transfers/[id]
/adjustments
/adjustments/new
/adjustments/[id]
/adjustments/approvals
/admin/catalog/categories
/admin/catalog/presentations
/admin/catalog/products
/admin/catalog/units
/admin/company/branches
/admin/company/profile
/admin/company/settings
/admin/security/users
/admin/security/roles
/admin/security/permissions
/admin/security/audit-logs
/admin/subscription/current-plan
/admin/subscription/limits
/admin/third-parties/customers
/admin/third-parties/suppliers
/cash-closings
/closures
/closures/new
/closures/[id]
/reports
/reports/adjustments
/reports/cash-closings
/reports/closures
/reports/inventory
/reports/kardex
/reports/movements
/reports/products
/reports/purchases
/reports/sales
/reports/transfers
/tenant-not-found
/manifest.webmanifest
/master/login
/master/companies
/master/companies/new
/master/companies/[id]
/master/audit-logs
```

The `/master/*` routes are the private SaaS operator panel. They use the platform cookie session (`src/lib/api/platform-http.ts`) and do not share the tenant session.

See `../docs/FRONTEND.md` for detailed route and feature behavior.
