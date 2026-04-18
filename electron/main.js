const {
  app, BrowserWindow, Menu, Tray, nativeImage,
  dialog, ipcMain,
} = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const net = require('net');

// ──────────────── State ────────────────
let backendProcess = null;
let mainWindow = null;
let managerWindow = null;
let tray = null;
let backendState = 'stopped'; // 'stopped' | 'starting' | 'running' | 'stopping'
let backendPid = null;
let backendStartTime = null;
let healthCheckTimer = null;
let logBuffer = [];
let logFileStream = null;

const QUARKUS_PORT = 8080;
const MAX_LOG_LINES = 2000;
const HEALTH_CHECK_INTERVAL_MS = 1500;
const HEALTH_CHECK_TIMEOUT_MS = 60000;

// ──────────────── Config ────────────────
const CONFIG_PATH = path.join(app.getPath('userData'), 'config.json');
let config = { logToFile: false, logFilePath: '' };

function loadConfig() {
  try {
    if (fs.existsSync(CONFIG_PATH)) {
      Object.assign(config, JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8')));
    }
  } catch { /* ignore */ }
}

function saveConfig() {
  try {
    fs.mkdirSync(path.dirname(CONFIG_PATH), { recursive: true });
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(config, null, 2));
  } catch (e) {
    console.error('Failed to save config:', e.message);
  }
}

// ──────────────── Binary ────────────────
function findBackendBinary() {
  const packaged = path.join(process.resourcesPath, 'pasa-auto-runner');
  if (fs.existsSync(packaged)) return packaged;
  const dev = path.join(__dirname, 'bin', 'pasa-auto-runner');
  if (fs.existsSync(dev)) return dev;
  return null;
}

// ──────────────── Port check ────────────────
// Returns true if something is already bound to the port (conflict).
function checkPortInUse(port) {
  return new Promise((resolve) => {
    const server = net.createServer();
    server.once('error', () => resolve(true));
    server.once('listening', () => server.close(() => resolve(false)));
    server.listen(port, '127.0.0.1');
  });
}

// ──────────────── Health check ────────────────
// Returns true if the port is accepting connections (Quarkus is up).
function checkServerReady() {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    socket.setTimeout(500);
    socket.once('connect', () => { socket.destroy(); resolve(true); });
    socket.once('error', () => { socket.destroy(); resolve(false); });
    socket.once('timeout', () => { socket.destroy(); resolve(false); });
    socket.connect(QUARKUS_PORT, '127.0.0.1');
  });
}

function startHealthCheck() {
  let elapsed = 0;

  function schedule() {
    healthCheckTimer = setTimeout(async () => {
      elapsed += HEALTH_CHECK_INTERVAL_MS;
      if (elapsed >= HEALTH_CHECK_TIMEOUT_MS) {
        appendLog('[manager] Health check timed out after 60 s — stopping backend');
        stopHealthCheck();
        stopBackend();
        return;
      }
      const ready = await checkServerReady();
      if (ready) {
        stopHealthCheck();
        setBackendState('running');
        appendLog(`[manager] Backend is ready on port ${QUARKUS_PORT}`);
        if (mainWindow && !mainWindow.isDestroyed()) mainWindow.reload();
      } else {
        schedule();
      }
    }, HEALTH_CHECK_INTERVAL_MS);
  }

  schedule();
}

function stopHealthCheck() {
  if (healthCheckTimer) { clearTimeout(healthCheckTimer); healthCheckTimer = null; }
}

// ──────────────── Logging ────────────────
function openLogFileStream() {
  if (logFileStream) {
    try { logFileStream.end(); } catch {}
    logFileStream = null;
  }
  if (config.logToFile && config.logFilePath) {
    try {
      fs.mkdirSync(path.dirname(config.logFilePath), { recursive: true });
      logFileStream = fs.createWriteStream(config.logFilePath, { flags: 'a' });
    } catch (e) {
      console.error('Failed to open log file stream:', e.message);
    }
  }
}

function appendLog(text) {
  const entry = { time: new Date().toISOString(), text };
  logBuffer.push(entry);
  if (logBuffer.length > MAX_LOG_LINES) logBuffer.shift();
  if (logFileStream) {
    try { logFileStream.write(`[${entry.time}] ${entry.text}\n`); } catch {}
  }
  if (managerWindow && !managerWindow.isDestroyed()) {
    managerWindow.webContents.send('backend:log-line', entry);
  }
}

// ──────────────── Backend state ────────────────
function getStatus() {
  return { state: backendState, pid: backendPid, startTime: backendStartTime };
}

function setBackendState(state) {
  backendState = state;
  updateTray();
  if (managerWindow && !managerWindow.isDestroyed()) {
    managerWindow.webContents.send('backend:status-changed', getStatus());
  }
}

async function startBackend() {
  if (backendState !== 'stopped') {
    return { success: false, error: 'Backend is not stopped' };
  }

  const portInUse = await checkPortInUse(QUARKUS_PORT);
  if (portInUse) {
    const msg = `Port ${QUARKUS_PORT} is already in use by another process.\nStop it before starting PazaAuto.`;
    dialog.showErrorBox('Port Conflict', msg);
    return { success: false, error: msg };
  }

  const binaryPath = findBackendBinary();
  if (!binaryPath) {
    const msg = 'Native binary not found at bin/pasa-auto-runner.\nPlease build the project first.';
    dialog.showErrorBox('Binary Not Found', msg);
    return { success: false, error: msg };
  }

  try { fs.chmodSync(binaryPath, 0o755); } catch {}

  openLogFileStream();

  const proc = spawn(binaryPath, [], {
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true,
  });

  backendProcess = proc;
  backendPid = proc.pid;
  backendStartTime = Date.now();
  setBackendState('starting');
  appendLog(`[manager] Starting backend: ${binaryPath} (PID ${proc.pid})`);

  proc.stdout.on('data', (data) => {
    data.toString().split('\n').filter((l) => l.trim()).forEach(appendLog);
  });
  proc.stderr.on('data', (data) => {
    data.toString().split('\n').filter((l) => l.trim()).forEach(appendLog);
  });
  proc.once('exit', (code) => {
    appendLog(`[manager] Process exited (code ${code ?? 'null'})`);
    backendProcess = null;
    backendPid = null;
    backendStartTime = null;
    stopHealthCheck();
    setBackendState('stopped');
    if (logFileStream) { try { logFileStream.end(); } catch {} logFileStream = null; }
  });
  proc.once('error', (err) => {
    appendLog(`[manager] Process error: ${err.message}`);
    backendProcess = null;
    backendPid = null;
    backendStartTime = null;
    stopHealthCheck();
    setBackendState('stopped');
  });

  startHealthCheck();
  return { success: true };
}

function stopBackend() {
  if (!backendProcess || backendState === 'stopped' || backendState === 'stopping') return;
  stopHealthCheck();
  setBackendState('stopping');
  appendLog('[manager] Sending SIGTERM to backend…');
  backendProcess.kill('SIGTERM');
  const forceKill = setTimeout(() => {
    if (backendProcess) {
      appendLog('[manager] Force killing backend (SIGKILL)…');
      backendProcess.kill('SIGKILL');
    }
  }, 5000);
  backendProcess.once('exit', () => clearTimeout(forceKill));
}

// ──────────────── IPC ────────────────
function setupIPC() {
  ipcMain.handle('backend:status', () => getStatus());
  ipcMain.handle('backend:get-logs', () => logBuffer);
  ipcMain.handle('backend:start', () => startBackend());
  ipcMain.handle('backend:stop', () => { stopBackend(); return { success: true }; });

  ipcMain.handle('config:get', () => config);
  ipcMain.handle('config:set', (_, newConfig) => {
    Object.assign(config, newConfig);
    saveConfig();
    openLogFileStream();
    return config;
  });
  ipcMain.handle('config:get-default-log-path', () =>
    path.join(app.getPath('logs'), 'pasa-auto-backend.log'),
  );

  ipcMain.handle('dialog:pick-log-file', async () => {
    const result = await dialog.showSaveDialog(managerWindow, {
      title: 'Choose Log File Location',
      defaultPath: path.join(app.getPath('logs'), 'pasa-auto-backend.log'),
      filters: [
        { name: 'Log Files', extensions: ['log', 'txt'] },
        { name: 'All Files', extensions: ['*'] },
      ],
    });
    return result.canceled ? null : result.filePath;
  });

  ipcMain.handle('log:save-to-file', async (_, content) => {
    const result = await dialog.showSaveDialog(managerWindow, {
      title: 'Save Console Log',
      defaultPath: path.join(app.getPath('downloads'), `pasa-auto-${Date.now()}.log`),
      filters: [{ name: 'Log Files', extensions: ['log', 'txt'] }],
    });
    if (result.canceled) return { success: false };
    try {
      fs.writeFileSync(result.filePath, content);
      return { success: true, path: result.filePath };
    } catch (e) {
      return { success: false, error: e.message };
    }
  });
}

// ──────────────── Tray ────────────────
function getStateIcon(state) {
  const icons = {
    running: 'icon-green.png',
    starting: 'icon-yellow.png',
    stopping: 'icon-yellow.png',
    stopped: 'icon-red.png',
  };
  const name = icons[state] || 'icon-red.png';
  const iconPath = path.join(__dirname, 'resources', name);
  const fallback = path.join(__dirname, 'resources', 'icon-red.png');
  return nativeImage
    .createFromPath(fs.existsSync(iconPath) ? iconPath : fallback)
    .resize({ width: 16, height: 16 });
}

function updateTray() {
  if (!tray) return;
  tray.setImage(getStateIcon(backendState));
  const labels = {
    stopped: 'Stopped', starting: 'Starting…', running: 'Running', stopping: 'Stopping…',
  };
  tray.setToolTip(`PazaAuto: ${labels[backendState] || backendState}`);
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: 'Open Manager', click: () => createManagerWindow() },
    { type: 'separator' },
    { label: 'Start Backend', enabled: backendState === 'stopped', click: () => startBackend() },
    { label: 'Stop Backend', enabled: backendState === 'running', click: () => stopBackend() },
    { type: 'separator' },
    { label: 'Quit', role: 'quit' },
  ]));
}

function createTray() {
  tray = new Tray(getStateIcon('stopped'));
  tray.on('double-click', () => createManagerWindow());
  updateTray();
}

// ──────────────── App menu ────────────────
function createAppMenu() {
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    {
      label: 'Service',
      submenu: [
        { label: 'Open Manager', click: () => createManagerWindow() },
        { type: 'separator' },
        { label: 'Start Backend', click: () => startBackend() },
        { label: 'Stop Backend', click: () => stopBackend() },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'View',
      submenu: [{ role: 'reload' }, { role: 'forceReload' }, { role: 'toggleDevTools' }],
    },
  ]));
}

// ──────────────── Windows ────────────────
function createManagerWindow() {
  if (managerWindow && !managerWindow.isDestroyed()) {
    managerWindow.focus();
    return;
  }
  managerWindow = new BrowserWindow({
    width: 900,
    height: 650,
    minWidth: 700,
    minHeight: 500,
    title: 'PazaAuto Manager',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
  });
  managerWindow.loadFile(path.join(__dirname, 'manager.html'));
  managerWindow.on('closed', () => { managerWindow = null; });
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 900,
    title: 'PazaAuto',
    webPreferences: { nodeIntegration: false, contextIsolation: true },
  });
  mainWindow.loadURL(`http://localhost:${QUARKUS_PORT}/`);
  mainWindow.on('closed', () => { mainWindow = null; });
}

// ──────────────── App lifecycle ────────────────
app.whenReady().then(() => {
  loadConfig();
  setupIPC();
  createAppMenu();
  createTray();
  createManagerWindow();
  createMainWindow();
  startBackend();
});

app.on('before-quit', () => {
  stopHealthCheck();
  if (backendProcess) backendProcess.kill();
  if (logFileStream) { try { logFileStream.end(); } catch {} }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('activate', () => {
  if (!mainWindow) createMainWindow();
});
