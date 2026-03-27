const { app, BrowserWindow, Menu, Tray, nativeImage, dialog, ipcMain } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const http = require('http');

const CONFIG = {
  backendUrl: process.env.BACKEND_URL || 'http://localhost:8080',
  healthEndpoint: '/q/health',
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
  serverUptime: null,
  startTime: null,
  healthCheckTimer: null,
  logs: [],
  maxLogs: 500,
};

function findJava() {
  const candidates = [
    process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : null,
    '/usr/bin/java',
    '/usr/local/bin/java',
    '/opt/homebrew/bin/java',
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) return candidate;
  }
  return 'java';
}

function findBackendJar() {
  const packaged = path.join(process.resourcesPath, 'backend.jar');
  if (fs.existsSync(packaged)) return packaged;

  const candidates = fs.readdirSync(path.join(__dirname, '..'), { withFileTypes: true })
    .filter(e => e.isFile() && e.name.endsWith('.jar') && e.name.includes('pasa-auto'))
    .map(e => path.join(__dirname, '..', e.name));

  if (candidates.length > 0) return candidates.sort().pop();

  const devJar = path.join(__dirname, '..', 'target', 'pasa-auto-0.0.14-runner');
  if (fs.existsSync(devJar)) return devJar;

  return null;
}

function addLog(level, message) {
  const timestamp = new Date().toISOString();
  const entry = { timestamp, level, message };
  state.logs.push(entry);
  if (state.logs.length > state.maxLogs) state.logs.shift();
  if (state.mainWindow) {
    state.mainWindow.webContents.send('log-entry', entry);
  }
}

function checkServerHealth() {
  return new Promise((resolve) => {
    const url = new URL(CONFIG.healthEndpoint, CONFIG.backendUrl);
    
    http.get(url.toString(), { timeout: 5000 }, (res) => {
      const isHealthy = res.statusCode >= 200 && res.statusCode < 300;
      addLog('info', `Health check: ${isHealthy ? 'OK' : 'FAILED'} (${res.statusCode})`);
      resolve(isHealthy);
    }).on('error', (err) => {
      addLog('warn', `Health check failed: ${err.message}`);
      resolve(false);
    });
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

  await checkServerHealth();
}

function stopHealthCheck() {
  if (state.healthCheckTimer) {
    clearInterval(state.healthCheckTimer);
    state.healthCheckTimer = null;
  }
}

async function startBackend() {
  if (state.backendProcess) {
    addLog('warn', 'Backend already running');
    return { success: false, message: 'Backend already running' };
  }

  const jarPath = findBackendJar();
  if (!jarPath) {
    const error = 'Could not find pasa-auto JAR. Please build: mvn clean package -DskipTests';
    addLog('error', error);
    dialog.showErrorBox('Backend Not Found', error);
    return { success: false, message: error };
  }

  const java = findJava();
  addLog('info', `Starting backend with: ${java}`);
  addLog('info', `JAR: ${path.basename(jarPath)}`);

  state.backendProcess = spawn(java, ['-jar', jarPath], {
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true,
  });

  state.backendProcess.stdout.on('data', (data) => {
    addLog('debug', data.toString().trim());
  });

  state.backendProcess.stderr.on('data', (data) => {
    addLog('error', data.toString().trim());
  });

  state.backendProcess.on('exit', (code) => {
    addLog('info', `Backend exited with code: ${code}`);
    state.backendProcess = null;
    state.serverStatus = 'stopped';
    state.startTime = null;
    stopHealthCheck();
    updateMenu();
    updateTray();
    notifyRenderer();
  });

  state.backendProcess.on('error', (err) => {
    addLog('error', `Failed to start backend: ${err.message}`);
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
    if (state.mainWindow) {
      state.mainWindow.reload();
    }
  }, CONFIG.startupDelay);

  return { success: true, message: 'Backend starting...' };
}

function stopBackend() {
  if (!state.backendProcess) {
    addLog('warn', 'No backend process to stop');
    return { success: false, message: 'No backend process running' };
  }

  addLog('info', 'Stopping backend...');
  stopHealthCheck();
  state.backendProcess.kill('SIGTERM');
  
  setTimeout(() => {
    if (state.backendProcess) {
      addLog('warn', 'Force killing backend...');
      state.backendProcess.kill('SIGKILL');
    }
    state.serverStatus = 'stopped';
    state.startTime = null;
    updateMenu();
    updateTray();
    notifyRenderer();
  }, 3000);

  return { success: true, message: 'Backend stopping...' };
}

function restartBackend() {
  addLog('info', 'Restarting backend...');
  stopBackend();
  setTimeout(() => {
    startBackend();
  }, 3500);
}

function notifyRenderer() {
  if (state.mainWindow) {
    state.mainWindow.webContents.send('server-status', getServerStatus());
  }
}

function getServerStatus() {
  return {
    status: state.serverStatus,
    uptime: state.startTime ? Date.now() - state.startTime : null,
    url: CONFIG.backendUrl,
    processId: state.backendProcess?.pid || null,
    logs: state.logs.slice(-50),
  };
}

function updateMenu() {
  createMenu();
}

function createMenu() {
  const template = [
    {
      label: 'Service',
      submenu: [
        {
          label: 'Start Backend',
          click: () => startBackend(),
          enabled: !state.backendProcess,
        },
        {
          label: 'Stop Backend',
          click: () => stopBackend(),
          enabled: !!state.backendProcess,
        },
        {
          label: 'Restart Backend',
          click: () => restartBackend(),
          enabled: !!state.backendProcess,
        },
        { type: 'separator' },
        {
          label: `Status: ${state.serverStatus.toUpperCase()}`,
          enabled: false,
        },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'View',
      submenu: [
        {
          label: 'Server Manager',
          accelerator: 'CmdOrCtrl+Shift+S',
          click: () => createStatusWindow(),
        },
        { type: 'separator' },
        { role: 'reload' },
        { role: 'forceReload' },
        { type: 'separator' },
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
      tooltip = `PazaAuto: Running\n${CONFIG.backendUrl}`;
      break;
    case 'starting':
      iconName = 'icon-yellow.png';
      tooltip = 'PazaAuto: Starting...';
      break;
    case 'unhealthy':
      iconName = 'icon-yellow.png';
      tooltip = 'PazaAuto: Unhealthy';
      break;
    default:
      iconName = 'icon-red.png';
      tooltip = 'PazaAuto: Stopped';
  }

  const iconPath = path.join(__dirname, 'resources', iconName);
  const icon = nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 });
  state.tray.setImage(icon);
  state.tray.setToolTip(tooltip);

  const contextMenu = Menu.buildFromTemplate([
    { label: `Status: ${state.serverStatus.toUpperCase()}`, enabled: false },
    { type: 'separator' },
    { label: 'Start', click: () => startBackend(), enabled: !state.backendProcess },
    { label: 'Stop', click: () => stopBackend(), enabled: !!state.backendProcess },
    { label: 'Restart', click: () => restartBackend(), enabled: !!state.backendProcess },
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
  let icon = nativeImage.createFromPath(iconPath);
  
  if (icon.isEmpty()) {
    icon = nativeImage.createEmpty();
  } else {
    icon = icon.resize({ width: 16, height: 16 });
  }
  
  state.tray = new Tray(icon);
  updateTray();

  state.tray.on('click', () => {
    if (state.mainWindow) {
      if (state.mainWindow.isVisible()) {
        state.mainWindow.focus();
      } else {
        state.mainWindow.show();
      }
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

  state.mainWindow.loadFile(CONFIG.getFrontendPath());

  state.mainWindow.once('ready-to-show', () => {
    state.mainWindow.show();
    addLog('info', 'Main window ready');
  });

  state.mainWindow.on('closed', () => {
    state.mainWindow = null;
  });

  state.mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
    addLog('error', `Failed to load: ${errorDescription} (${errorCode})`);
  });

  state.mainWindow.webContents.on('did-finish-load', () => {
    addLog('info', 'Application loaded successfully');
    notifyRenderer();
  });
}

function createStatusWindow() {
  if (state.statusWindow) {
    state.statusWindow.focus();
    return;
  }

  state.statusWindow = new BrowserWindow({
    width: 600,
    height: 700,
    minWidth: 400,
    minHeight: 500,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
    icon: path.join(__dirname, 'resources', 'icon-green.png'),
    title: 'PazaAuto - Server Manager',
    parent: null,
    modal: false,
  });

  state.statusWindow.loadFile(path.join(__dirname, 'status-panel.html'));

  state.statusWindow.on('closed', () => {
    state.statusWindow = null;
  });

  addLog('info', 'Status panel opened');
}

function closeStatusWindow() {
  if (state.statusWindow) {
    state.statusWindow.close();
    state.statusWindow = null;
  }
}

function setupIPC() {
  ipcMain.handle('get-server-status', () => getServerStatus());
  ipcMain.handle('start-server', () => startBackend());
  ipcMain.handle('stop-server', () => stopBackend());
  ipcMain.handle('restart-server', () => restartBackend());
  ipcMain.handle('get-logs', () => state.logs);
  ipcMain.handle('clear-logs', () => {
    state.logs = [];
    return [];
  });
  ipcMain.handle('get-config', () => CONFIG);
}

app.whenReady().then(() => {
  setupIPC();
  createMenu();
  createTray();
  createWindow();
  
  if (process.env.AUTO_START !== 'false') {
    startBackend();
  }
});

app.on('before-quit', () => {
  stopHealthCheck();
  if (state.backendProcess) {
    state.backendProcess.kill('SIGTERM');
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (state.mainWindow === null) createWindow();
});

app.on('quit', () => {
  addLog('info', 'Application closing...');
});
