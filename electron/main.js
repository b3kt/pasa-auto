const { app, BrowserWindow, Menu, Tray, nativeImage, dialog, ipcMain, shell } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const http = require('http');

// Read .env early so CONFIG can use the port from it
function readDotEnvSync() {
  const envPath = path.join(__dirname, 'bin', '.env');
  if (!fs.existsSync(envPath)) return {};
  const vars = {};
  for (const line of fs.readFileSync(envPath, 'utf8').split('\n')) {
    const t = line.trim();
    if (!t || t.startsWith('#')) continue;
    const eq = t.indexOf('=');
    if (eq === -1) continue;
    const k = t.slice(0, eq).trim();
    const v = t.slice(eq + 1).trim().replace(/^["']|["']$/g, '');
    if (k) vars[k] = v;
  }
  return vars;
}

const _envVars = readDotEnvSync();

function resolveBackendUrl() {
  if (process.env.BACKEND_URL) return process.env.BACKEND_URL;
  if (_envVars.SERVER_URL) return _envVars.SERVER_URL;
  const port = _envVars.HTTP_PORT || '8080';
  return `http://localhost:${port}`;
}

const CONFIG = {
  backendUrl: resolveBackendUrl(),
  healthEndpoint: '/health',
  startupDelay: 5000,
  healthCheckInterval: 10000,
  getFrontendPath() {
    return path.join(__dirname, 'resources', 'ui', 'index.html');
  },
};

const state = {
  backendProcess: null,
  mainWindow: null,
  statusWindow: null,
  tray: null,
  serverStatus: 'stopped',
  startTime: null,
  healthCheckTimer: null,
  logs: [],
  maxLogs: 500,
};

// Return already-parsed .env vars and log the count
function loadDotEnv() {
  const envPath = path.join(__dirname, 'bin', '.env');
  if (!fs.existsSync(envPath)) {
    addLog('warn', 'No .env file found at electron/bin/.env');
    return {};
  }
  addLog('info', `Loaded ${Object.keys(_envVars).length} variable(s) from electron/bin/.env`);
  return _envVars;
}

// Resolve the native binary path
function findBinary() {
  const binaryName = process.platform === 'win32' ? 'pasa-auto-runner.exe' : 'pasa-auto-runner';

  // When packaged, electron-builder places extraResources under process.resourcesPath
  const packedBinary = path.join(process.resourcesPath, 'bin', binaryName);
  if (fs.existsSync(packedBinary)) return packedBinary;

  // Development: binary lives next to main.js in electron/bin/
  const devBinary = path.join(__dirname, 'bin', binaryName);
  if (fs.existsSync(devBinary)) return devBinary;

  return null;
}

function addLog(level, message) {
  const timestamp = new Date().toISOString();
  const entry = { timestamp, level, message };
  state.logs.push(entry);
  if (state.logs.length > state.maxLogs) state.logs.shift();

  if (state.mainWindow) state.mainWindow.webContents.send('log-entry', entry);
  if (state.statusWindow) state.statusWindow.webContents.send('log-entry', entry);
}

function checkServerHealth() {
  return new Promise((resolve) => {
    const url = new URL(CONFIG.healthEndpoint, CONFIG.backendUrl);
    const req = http.get(url.toString(), { timeout: 5000 }, (res) => {
      const isHealthy = res.statusCode >= 200 && res.statusCode < 300;
      resolve(isHealthy);
    });
    req.on('error', () => resolve(false));
    req.on('timeout', () => { req.destroy(); resolve(false); });
  });
}

async function startHealthCheck() {
  if (state.healthCheckTimer) clearInterval(state.healthCheckTimer);

  state.healthCheckTimer = setInterval(async () => {
    const isHealthy = await checkServerHealth();
    const newStatus = isHealthy ? 'running' : 'unhealthy';

    if (newStatus !== state.serverStatus) {
      state.serverStatus = newStatus;
      updateTray();
      updateMenu();
      notifyRenderer();
    }
  }, CONFIG.healthCheckInterval);

  // Immediate check
  const isHealthy = await checkServerHealth();
  const newStatus = isHealthy ? 'running' : 'starting';
  if (newStatus !== state.serverStatus) {
    state.serverStatus = newStatus;
    updateTray();
    updateMenu();
    notifyRenderer();
  }
}

function stopHealthCheck() {
  if (state.healthCheckTimer) {
    clearInterval(state.healthCheckTimer);
    state.healthCheckTimer = null;
  }
}

async function startBackend() {
  if (state.backendProcess) {
    addLog('warn', 'Server already running');
    return { success: false, message: 'Server already running' };
  }

  const binaryPath = findBinary();
  if (!binaryPath) {
    const error = 'Could not find pasa-auto-runner binary at electron/bin/pasa-auto-runner';
    addLog('error', error);
    dialog.showErrorBox('Binary Not Found', error);
    return { success: false, message: error };
  }

  // Ensure the binary is executable
  try {
    fs.chmodSync(binaryPath, 0o755);
  } catch (e) {
    addLog('warn', `Could not set executable bit: ${e.message}`);
  }

  addLog('info', `Starting server: ${binaryPath}`);

  const dotEnvVars = loadDotEnv();
  const spawnEnv = { ...process.env, ...dotEnvVars };

  state.backendProcess = spawn(binaryPath, [], {
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true,
    env: spawnEnv,
  });

  state.backendProcess.stdout.on('data', (data) => {
    const lines = data.toString().trim().split('\n');
    lines.forEach(line => line && addLog('debug', line));
  });

  state.backendProcess.stderr.on('data', (data) => {
    const lines = data.toString().trim().split('\n');
    lines.forEach(line => line && addLog('error', line));
  });

  state.backendProcess.on('exit', (code, signal) => {
    addLog('info', `Server exited (code=${code}, signal=${signal})`);
    state.backendProcess = null;
    state.serverStatus = 'stopped';
    state.startTime = null;
    stopHealthCheck();
    updateMenu();
    updateTray();
    notifyRenderer();
  });

  state.backendProcess.on('error', (err) => {
    addLog('error', `Failed to start server: ${err.message}`);
    state.backendProcess = null;
    state.serverStatus = 'error';
    stopHealthCheck();
    updateMenu();
    updateTray();
    notifyRenderer();
  });

  state.serverStatus = 'starting';
  state.startTime = Date.now();
  updateMenu();
  updateTray();
  notifyRenderer();

  addLog('info', `Waiting ${CONFIG.startupDelay / 1000}s for server startup...`);

  setTimeout(async () => {
    await startHealthCheck();
    if (state.serverStatus === 'running' && state.mainWindow) {
      state.mainWindow.reload();
    }
  }, CONFIG.startupDelay);

  return { success: true, message: 'Server starting...' };
}

function stopBackend() {
  if (!state.backendProcess) {
    addLog('warn', 'No server process to stop');
    return { success: false, message: 'No server process running' };
  }

  addLog('info', 'Stopping server...');
  stopHealthCheck();

  state.backendProcess.kill('SIGTERM');

  // Force kill if still running after 5s
  const forceKillTimer = setTimeout(() => {
    if (state.backendProcess) {
      addLog('warn', 'Force killing server (SIGKILL)...');
      state.backendProcess.kill('SIGKILL');
    }
  }, 5000);

  state.backendProcess.once('exit', () => clearTimeout(forceKillTimer));

  state.serverStatus = 'stopped';
  state.startTime = null;
  updateMenu();
  updateTray();
  notifyRenderer();

  return { success: true, message: 'Server stopping...' };
}

async function restartBackend() {
  addLog('info', 'Restarting server...');
  stopBackend();
  await new Promise(resolve => setTimeout(resolve, 3500));
  return startBackend();
}

function notifyRenderer() {
  const status = getServerStatus();
  if (state.mainWindow) state.mainWindow.webContents.send('server-status', status);
  if (state.statusWindow) state.statusWindow.webContents.send('server-status', status);
}

function getServerStatus() {
  return {
    status: state.serverStatus,
    uptime: state.startTime ? Date.now() - state.startTime : null,
    startTime: state.startTime,
    url: CONFIG.backendUrl,
    processId: state.backendProcess?.pid || null,
  };
}

function updateMenu() {
  const isRunning = !!state.backendProcess;

  const template = [
    {
      label: 'Service',
      submenu: [
        {
          label: `Status: ${state.serverStatus.toUpperCase()}`,
          enabled: false,
        },
        { type: 'separator' },
        {
          label: 'Server Manager',
          accelerator: 'CmdOrCtrl+Shift+S',
          click: () => createStatusWindow(),
        },
        { type: 'separator' },
        { role: 'reload' },
        { role: 'forceReload' },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'View',
      submenu: [
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
        { type: 'separator' },
        { role: 'toggleDevTools' },
      ],
    },
    {
      label: 'Window',
      submenu: [
        { role: 'minimize' },
        { role: 'zoom' },
        { type: 'separator' },
        { role: 'close' },
      ],
    },
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

function updateTray() {
  if (!state.tray) return;

  let iconName, tooltip;
  switch (state.serverStatus) {
    case 'running':
      iconName = 'icon-green.png';
      tooltip = `PasaAuto: Running\n${CONFIG.backendUrl}`;
      break;
    case 'starting':
      iconName = 'icon-red.png';
      tooltip = 'PasaAuto: Starting...';
      break;
    case 'unhealthy':
      iconName = 'icon-red.png';
      tooltip = 'PasaAuto: Unhealthy';
      break;
    default:
      iconName = 'icon-red.png';
      tooltip = 'PasaAuto: Stopped';
  }

  const iconPath = path.join(__dirname, 'resources', iconName);
  const icon = fs.existsSync(iconPath)
    ? nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 })
    : nativeImage.createEmpty();

  state.tray.setImage(icon);
  state.tray.setToolTip(tooltip);

  const contextMenu = Menu.buildFromTemplate([
    { label: `Status: ${state.serverStatus.toUpperCase()}`, enabled: false },
    { type: 'separator' },
    {
      label: 'Start',
      click: () => startBackend(),
      enabled: !state.backendProcess,
    },
    {
      label: 'Stop',
      click: () => stopBackend(),
      enabled: !!state.backendProcess,
    },
    {
      label: 'Restart',
      click: () => restartBackend(),
      enabled: !!state.backendProcess,
    },
    { type: 'separator' },
    { label: 'Server Manager', click: () => createStatusWindow() },
    { type: 'separator' },
    { label: 'Show App', click: () => state.mainWindow?.show() },
    { label: 'Quit', role: 'quit' },
  ]);

  state.tray.setContextMenu(contextMenu);
}

function createTray() {
  const iconPath = path.join(__dirname, 'resources', 'icon-red.png');
  let icon = fs.existsSync(iconPath)
    ? nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 })
    : nativeImage.createEmpty();

  state.tray = new Tray(icon);
  updateTray();

  state.tray.on('click', () => {
    if (state.mainWindow) {
      state.mainWindow.isVisible() ? state.mainWindow.focus() : state.mainWindow.show();
    }
  });
}

function createWindow() {
  state.mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
    icon: path.join(__dirname, 'resources', 'icon-green.png'),
    show: false,
  });

  const frontendPath = CONFIG.getFrontendPath();

  if (fs.existsSync(frontendPath)) {
    state.mainWindow.loadFile(frontendPath);
  } else {
    // Fallback: open backend URL directly in the window
    addLog('warn', 'Frontend UI not found, loading backend URL directly');
    state.mainWindow.loadURL(CONFIG.backendUrl);
  }

  state.mainWindow.once('ready-to-show', () => {
    state.mainWindow.show();
    addLog('info', 'Main window ready');
  });

  state.mainWindow.on('closed', () => {
    state.mainWindow = null;
  });

  state.mainWindow.webContents.on('did-fail-load', (_event, errorCode, errorDescription) => {
    addLog('error', `Failed to load: ${errorDescription} (${errorCode})`);
  });

  state.mainWindow.webContents.on('did-finish-load', () => {
    notifyRenderer();
  });
}

function createStatusWindow() {
  if (state.statusWindow) {
    state.statusWindow.focus();
    return;
  }

  state.statusWindow = new BrowserWindow({
    width: 650,
    height: 750,
    minWidth: 450,
    minHeight: 550,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
    icon: path.join(__dirname, 'resources', 'icon-green.png'),
    title: 'PasaAuto - Server Manager',
    modal: false,
  });

  state.statusWindow.loadFile(path.join(__dirname, 'status-panel.html'));

  state.statusWindow.on('closed', () => {
    state.statusWindow = null;
  });

  addLog('info', 'Server Manager opened');
}

function setupIPC() {
  ipcMain.handle('get-server-status', () => getServerStatus());
  ipcMain.handle('start-server', () => startBackend());
  ipcMain.handle('stop-server', () => stopBackend());
  ipcMain.handle('restart-server', () => restartBackend());
  ipcMain.handle('get-logs', () => state.logs);
  ipcMain.handle('clear-logs', () => { state.logs = []; return []; });
  ipcMain.handle('get-config', () => ({ backendUrl: CONFIG.backendUrl }));
  ipcMain.handle('open-server-manager', () => createStatusWindow());
}

app.whenReady().then(() => {
  setupIPC();
  updateMenu();
  createTray();
  createWindow();

  if (process.env.AUTO_START !== 'false') {
    startBackend();
  }
});

app.on('before-quit', () => {
  stopHealthCheck();
  if (state.backendProcess) {
    addLog('info', 'Shutting down server...');
    state.backendProcess.kill('SIGTERM');
  }
});

app.on('window-all-closed', () => {
  // Keep running in tray on all platforms when window is closed
  // (do not quit)
});

app.on('activate', () => {
  if (state.mainWindow === null) createWindow();
});