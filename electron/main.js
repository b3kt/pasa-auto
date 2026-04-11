const { app, BrowserWindow, Menu, Tray, nativeImage, dialog } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

let backendProcess;
let mainWindow;
let tray;

function findJava() {
  const candidates = [
    process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : null,
    '/usr/bin/java',
    '/usr/local/bin/java',
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) return candidate;
  }
  return 'java'; // fallback to PATH
}

function findBackendJar() {
  // Packaged app: jar is in extraResources
  const packaged = path.join(process.resourcesPath, 'backend.jar');
  if (fs.existsSync(packaged)) return packaged;

  // Development: look for the built jar in target/
  const devJar = path.join(__dirname, '..', 'target', 'pasa-auto-0.0.14.jar');
  if (fs.existsSync(devJar)) return devJar;

  return null;
}

function startBackend() {
  if (backendProcess) {
    console.log('Backend already running');
    return;
  }

  const jarPath = findBackendJar();
  if (!jarPath) {
    dialog.showErrorBox('Backend Not Found', 'Could not find pasa-auto JAR. Please build the project first with: mvn clean package -DskipTests');
    return;
  }

  const java = findJava();
  console.log('Starting backend:', java, '-jar', jarPath);

  backendProcess = spawn(java, ['-jar', jarPath], {
    stdio: 'ignore',
    windowsHide: true,
  });

  updateTray();

  backendProcess.on('exit', (code) => {
    console.log('Backend exited with code:', code);
    backendProcess = null;
    updateMenu();
    updateTray();
  });

  backendProcess.on('error', (err) => {
    console.error('Failed to start backend:', err);
    backendProcess = null;
    updateMenu();
    updateTray();
  });

  updateMenu();

  // Reload page after backend starts
  if (mainWindow) {
    setTimeout(() => mainWindow.reload(), 3000);
  }
}

function stopBackend() {
  if (backendProcess) {
    backendProcess.kill();
    backendProcess = null;
    updateMenu();
    updateTray();
    console.log('Backend stopped');
  }
}

function createMenu() {
  const template = [
    {
      label: 'Service',
      submenu: [
        {
          label: 'Start Backend',
          click: startBackend,
          enabled: !backendProcess,
        },
        {
          label: 'Stop Backend',
          click: stopBackend,
          enabled: !!backendProcess,
        },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { role: 'toggleDevTools' },
      ],
    },
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

function updateMenu() {
  createMenu();
}

function updateTray() {
  if (!tray) return;

  const iconName = backendProcess ? 'icon-green.png' : 'icon-red.png';
  const iconPath = path.join(__dirname, 'resources', iconName);
  const icon = nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 });

  tray.setImage(icon);
  tray.setToolTip(backendProcess ? 'Backend: Running' : 'Backend: Stopped');

  const contextMenu = Menu.buildFromTemplate([
    { label: 'Start Backend', click: startBackend, enabled: !backendProcess },
    { label: 'Stop Backend', click: stopBackend, enabled: !!backendProcess },
    { type: 'separator' },
    { label: 'Quit', role: 'quit' },
  ]);

  tray.setContextMenu(contextMenu);
}

function createTray() {
  const iconPath = path.join(__dirname, 'resources', 'icon-red.png');
  const icon = nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 });
  tray = new Tray(icon);
  updateTray();
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 900,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  mainWindow.loadURL('http://localhost:8080/');

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createMenu();
  createTray();
  createWindow();
  startBackend(); // auto-start backend on launch
});

app.on('before-quit', () => {
  if (backendProcess) backendProcess.kill();
});

app.on('window-all-closed', () => {
  // Keep app alive in tray on macOS
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === null) createWindow();
});
