// Starts the staff app (5173) and the customer portal (5174) together, so a single Quarkus dev
// startup serves both: the staff app at / and the customer portal at /app/ (the staff dev server
// proxies /app to the customer portal). Quinoa launches this via
// quarkus.quinoa.package-manager-command.dev; see src/main/resources/application.properties.
//
// The local "quasar" binary is spawned directly (no "npm run" hop): npm and its "sh -c" wrapper do
// not forward signals, which would leave the actual dev servers orphaned on quit.
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const webuiDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const appuiDir = path.resolve(webuiDir, '../appui')

const isWindows = process.platform === 'win32'
const quasarBin = (dir) => path.join(dir, 'node_modules', '.bin', isWindows ? 'quasar.cmd' : 'quasar')

const children = []
let shuttingDown = false

// Each dev server is spawned into its own process group (detached) so the whole tree dies with it.
function killTree(signal) {
  for (const child of children) {
    if (child.exitCode !== null || child.signalCode !== null) continue
    try {
      child.kill(signal)
    } catch {
      // ignore
    }
    if (!isWindows && child.pid) {
      try {
        process.kill(-child.pid, signal)
      } catch {
        // ignore
      }
    }
  }
}

function shutdown(code = 0) {
  if (shuttingDown) return
  shuttingDown = true
  killTree('SIGTERM')
  // Force-kill anything still alive, then exit. The timer is kept referenced so the process can
  // never hang waiting on an uncooperative child.
  setTimeout(() => {
    killTree('SIGKILL')
    process.exit(code)
  }, 2000)
}

function start(cwd, args) {
  const child = spawn(quasarBin(cwd), args, {
    cwd,
    stdio: 'inherit',
    detached: !isWindows
  })
  child.on('error', (err) => {
    console.error(`[dev-all] failed to start quasar in ${cwd}:`, err)
    shutdown(1)
  })
  child.on('exit', (code, signal) => {
    if (!shuttingDown) {
      console.error(`[dev-all] "${cwd}" dev server exited (code=${code} signal=${signal})`)
      shutdown(code ?? 1)
    }
  })
  children.push(child)
}

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => shutdown(0))
}

start(webuiDir, ['dev', '-p', '5173'])
start(appuiDir, ['dev'])
