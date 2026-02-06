const { app, BrowserWindow, Menu, Tray, nativeImage } = require('electron');
const { spawn } = require('child_process');
const path = require('path');

let backendProcess;
let mainWindow;
let tray;

function startBackend() {
  if (backendProcess) {
    console.log("Backend already running");
    return;
  }

  const backendPath = path.join(process.resourcesPath, 'myapp');
  console.log("Starting backend from:", backendPath);

  backendProcess = spawn(backendPath, [], {
    stdio: 'ignore',
    windowsHide: true,
  });

  updateTray();

  backendProcess.on('exit', (code) => {
    console.log("Native App exited with code:", code);
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

  // Reload page after a delay to ensure backend is up
  if (mainWindow) {
    setTimeout(() => mainWindow.reload(), 1000);
  }
}

function stopBackend() {
  if (backendProcess) {
    backendProcess.kill();
    backendProcess = null;
    updateMenu();
    updateTray();
    console.log("Backend stopped");
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
          enabled: !backendProcess
        },
        {
          label: 'Stop Backend',
          click: stopBackend,
          enabled: !!backendProcess
        },
        { type: 'separator' },
        { role: 'quit' }
      ]
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { role: 'toggleDevTools' }
      ]
    }
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
  const icon = nativeImage.createFromPath(iconPath);

  // Resize to 16x16 for tray
  const trayIcon = icon.resize({ width: 16, height: 16 });

  tray.setImage(trayIcon);
  tray.setToolTip(backendProcess ? 'Backend: Running' : 'Backend: Stopped');

  const contextMenu = Menu.buildFromTemplate([
    {
      label: 'Start Backend',
      click: startBackend,
      enabled: !backendProcess
    },
    {
      label: 'Stop Backend',
      click: stopBackend,
      enabled: !!backendProcess
    },
    { type: 'separator' },
    { label: 'Quit', role: 'quit' }
  ]);

  tray.setContextMenu(contextMenu);
}

function createTray() {
  const iconPath = path.join(__dirname, 'resources', 'icon-red.png'); // Default to stopped/red
  const icon = nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 });

  tray = new Tray(icon);
  updateTray();
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1000,
    height: 800,
  });

  // Load the SPA served by the native image
  mainWindow.loadURL("http://localhost:8080/");

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createMenu();
  createTray();
  createWindow();
});

app.on('before-quit', () => {
  if (backendProcess) backendProcess.kill();
});


