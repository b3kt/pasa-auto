# PazaAuto Electron

Desktop application wrapper for Pasa Auto workshop management system.

## Features

- **Server Management**: Start, stop, restart the Quarkus backend
- **Health Monitoring**: Automatic health checks via `/q/health` endpoint
- **Status Panel**: Real-time server status, uptime, and logs
- **System Tray**: Background operation with status indicators
- **Native Integration**: System tray, menu bar, keyboard shortcuts

## Quick Start

```bash
cd electron
npm install
npm start
```

## Server Status

| Status | Icon | Description |
|--------|------|-------------|
| Running | Green | Backend is healthy and responding |
| Starting | Yellow | Backend process started, waiting for health check |
| Unhealthy | Yellow | Backend process running but health check failing |
| Stopped | Red | Backend not running |
| Error | Red | Failed to start backend |

## Keyboard Shortcuts

- `Cmd/Ctrl+Shift+S` - Open Server Manager panel

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `BACKEND_URL` | `http://localhost:8080` | Backend server URL |
| `AUTO_START` | `true` | Auto-start backend on launch |

## Build

```bash
# macOS
npm run dist:mac

# Windows
npm run dist:win

# Linux
npm run dist:linux
```

## Project Structure

```
electron/
├── main.js           # Main process
├── preload.js        # Context bridge for IPC
├── status-panel.html # Server manager UI
└── resources/
    ├── icon-green.png  # Running status
    ├── icon-red.png    # Stopped status
    └── icon-yellow.png # Starting/unhealthy status
```

## IPC API

The preload script exposes `window.electronAPI`:

```javascript
// Get current server status
const status = await window.electronAPI.getServerStatus();
// { status, uptime, url, processId, logs }

// Server controls
await window.electronAPI.startServer();
await window.electronAPI.stopServer();
await window.electronAPI.restartServer();

// Logs
const logs = await window.electronAPI.getLogs();
await window.electronAPI.clearLogs();

// Event listeners
window.electronAPI.onServerStatus((status) => { /* ... */ });
window.electronAPI.onLogEntry((entry) => { /* ... */ });
```

## Backend JAR

The electron app looks for the backend JAR in this order:

1. `resources/backend.jar` (packaged)
2. `target/pasa-auto-*-runner` (native image)
3. Any `pasa-auto*.jar` in parent directory

Build with:
```bash
# JVM JAR
mvn clean package -DskipTests

# Native executable
mvn clean package -Pnative -DskipTests
```
