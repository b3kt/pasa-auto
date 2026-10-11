# Customer portal (com.github.b3kt.appui)

Quasar/Vue.js app for the customer self-service portal. It is a **standalone frontend project**: it
shares no source with the staff app in `../webui` and talks to the same backend through the
`/api/customer/*` endpoints.

| | Staff app | Customer portal |
|---|---|---|
| Source | `src/main/webui` | `src/main/appui` |
| Dev server | http://localhost:5173 | http://localhost:5174/app/ |
| Served at | `/` | `/app/` |

## Install the dependencies

```bash
npm install
```

## Start in development mode

```bash
npm run dev
```

The app is served from `/app/` (the same `publicPath` it uses in production), so open
http://localhost:5174/app/. API calls go to http://localhost:8080 - keep the Quarkus backend
running (`./mvnw quarkus:dev`) alongside it. Quinoa's dev server only proxies the staff app, so
this app is not reachable through http://localhost:5173.

## Lint / format / build

```bash
npm run lint
npm run format
npm run build          # -> dist/spa
```

## How it is deployed

Quarkus Quinoa supports a single `ui-dir`, so this app is not built by Quinoa directly. The staff
app's `build:all` script builds it and copies the result into the staff bundle as the `app/`
subdirectory, which Quinoa then registers as static resources:

```
src/main/appui/dist/spa/*   ->   src/main/webui/dist/spa/app/*
```

That build runs as part of `./mvnw package` (see `quarkus.quinoa.package-manager-command.build` in
`src/main/resources/application.properties`). To build and deploy both apps by hand:

```bash
cd src/main/webui
npm run build:all
```

`scripts/deploy-appui.mjs` in the staff app performs the copy.

**Note:** request `/app/` with the trailing slash. The bundle's `index.html` is registered as a
static resource, and Quarkus does not redirect a directory request without one.

## Routes

Hash mode under the `/app/` base, so a deep link or a page refresh never needs a server-side
fallback to `app/index.html`. `meta.customerAuth` in `src/router/routes.js` marks the routes that
need a customer session.

| Path | Page |
|------|------|
| `/` | Dashboard |
| `/login` | Sign in |
| `/register` | Create an account |
| `/profile` | Contact details |
| `/link-account` | Link to an existing customer record |
| `/vehicles` | Registered vehicles |
| `/bookings` | Service bookings |
| `/spk` | Service status |
| `/unavailable` | Portal switched off by the Owner |

## Session

`src/stores/auth-store.js` keeps the access token in memory only; it is never written to
`localStorage`, so page scripts cannot read it back. A logged-in session is restored on start-up by
`src/boot/auth.js`, which exchanges the HttpOnly refresh cookie scoped to `/api/customer/auth` for a
fresh access token. Only the non-sensitive `customer_auth_user` profile is persisted, and its key is
deliberately **not** renamed to `auth_user`: the refresh cookie is what keeps the session alive, so
renaming would strand every customer who signed in before the portal was split out of the staff app.

The Owner can switch the portal off without a redeploy (staff app: `admin/customer-access`). The
router guard checks that flag on every navigation and the 503 interceptor in `src/boot/axios.js`
catches it mid-page; both fail closed.

## Internationalization

`src/i18n` is wired up (locale in `localStorage.locale`, default `id-ID`) but the message catalogs
are empty: the portal copy is still hardcoded English. Add keys there when it is translated.
