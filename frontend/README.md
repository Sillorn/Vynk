# Vynk frontend

Angular 22 single-page app (standalone components, Angular Material, signals + RxJS) built with
`@angular/build`. It talks to the Spring Boot backend under `/api/v1` and is installable as a PWA.

## Prerequisites

- Node 22+ and npm (see `packageManager` in `package.json`)
- The backend running on `127.0.0.1:8080` (see `../backend`, `./mvnw spring-boot:run`)

## Commands

Run from `frontend/`:

| Command                | What it does                                                   |
| ---------------------- | -------------------------------------------------------------- |
| `npm ci`               | Install dependencies                                           |
| `npm start`            | `ng serve` on <http://localhost:4200>, proxying `/api/**`      |
| `npm run build`        | Production build into `dist/frontend/browser`                  |
| `npm run test`         | Vitest (jsdom) in watch mode                                   |
| `npm run test:ci`      | Vitest, single run (use this in CI)                            |
| `npm run lint`         | `vp lint` (Vite+ / oxlint)                                     |
| `npm run format`       | `vp fmt src --write`                                           |
| `npm run format:check` | `vp fmt src --check`                                           |

CI parity: `npm run format:check && npm run build && npm run test:ci`.

The dev server proxies `/api/**` to `http://127.0.0.1:8080` using `proxy.conf.json`. Restart
`ng serve` after changing it. Authentication uses an HttpOnly session cookie plus an XSRF token
cookie, so always use the dev server origin rather than calling the backend directly.

## Structure

```
src/app/
  core/       services, guards, interceptors (auth, social, profiles, PWA update)
  features/   routed pages: auth, profiles, settings, social (feed, network, messages, notifications)
  shared/     reusable pieces (http-error helper, avatar, validators, not-found page)
public/       static assets: manifest, icons, service worker (sw.js)
```

Conventions:

- Every page is lazy-loaded in `app.routes.ts`; `authGuard` protects signed-in pages and redirects
  anonymous users to `/login?returnUrl=…`.
- Components use `ChangeDetectionStrategy.OnPush` and signals.
- API failures are turned into messages with `describeHttpError` (`shared/http-error.ts`), which
  surfaces the RFC 9457 `detail` field when present.
- Initial bundle budget is 500 kB (warning). Keep heavy Material modules (snackbar, lists,
  dialogs) out of the app shell or load them on demand.

## Real-time updates

`EventStreamService` keeps one shared `EventSource` (`/api/v1/events`) per tab with exponential
retry. Events are only hints: pages and `UnreadService` re-fetch from REST, including after a
reconnect, so events missed while offline are recovered.

## PWA

- `public/manifest.webmanifest` declares separate `any` (rounded) and `maskable` (full-bleed) PNG
  icons plus an SVG; `apple-touch-icon.png` is linked from `index.html`.
- `public/sw.js` caches the app shell only. `/api/**` and the event stream always go to the
  network. Bump `CACHE` in `sw.js` when changing precached files.
- The service worker registers only in production builds over a secure context
  (`PwaUpdateService`). A new version waits until the user taps **Reload** in the snackbar; the
  worker never calls `skipWaiting()` on its own.
- Serve `sw.js` with `Cache-Control: no-cache` so updates are discovered promptly.

Regenerating icons from the SVG sources in `public/icons/` (ImageMagick):

```bash
cd public/icons
magick -background none -density 384 icon-512.svg -resize 192x192 icon-192.png
magick -background none -density 384 icon-512.svg -resize 512x512 icon-512.png
magick -background none -density 384 icon-maskable.svg -resize 192x192 icon-maskable-192.png
magick -background none -density 384 icon-maskable.svg -resize 512x512 icon-maskable-512.png
magick -background none -density 384 icon-maskable.svg -resize 180x180 apple-touch-icon.png
```

## Testing

Unit tests live next to the code as `*.spec.ts` and run on Vitest with jsdom (not Karma). Guards,
the XSRF interceptor, the event stream, unread/PWA services, the login redirect and the messages
page have coverage; use `vi.fn()`/`vi.stubGlobal` for browser APIs such as `EventSource`.
