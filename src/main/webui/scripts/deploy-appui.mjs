// Copies the customer portal bundle (../appui) into the admin bundle as the app/ subdirectory, so
// a single Quinoa build serves the staff app at / and the customer app at /app/.
//
// Runs as part of "npm run build:all", which quarkus-quinoa invokes (see
// quarkus.quinoa.package-manager-command.build in src/main/resources/application.properties).
// Quinoa registers every file it finds under the build dir, so the nested app/ tree is served from
// /app/** while / keeps serving the admin index.html.
import { cp, rm, stat } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const webuiDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const appuiDir = path.resolve(webuiDir, '../appui')

// The customer portal is built in PWA mode (quasar build -m pwa), so its bundle lands in dist/pwa.
// The staff app is still an SPA build, so it lands in dist/spa.
const sourceDir = path.join(appuiDir, 'dist/pwa')
const targetDir = path.join(webuiDir, 'dist/spa/app')

try {
  await stat(path.join(sourceDir, 'index.html'))
} catch {
  console.error(
    `[deploy-appui] ${sourceDir} has no index.html - the customer app was not built.\n` +
    'Run "npm ci && npm run build" in src/main/appui first.'
  )
  process.exit(1)
}

// Drop the previous copy first: a stale hashed asset left behind would still be served, and the
// admin build (quasar build) only cleans its own dist/spa root, not this subdirectory.
await rm(targetDir, { recursive: true, force: true })
await cp(sourceDir, targetDir, { recursive: true })

console.log(`[deploy-appui] customer portal copied to dist/spa/app (served at /app/)`)
