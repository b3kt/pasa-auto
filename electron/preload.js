const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  // ── Status & logs ──
  getStatus:    () => ipcRenderer.invoke('backend:status'),
  getLogs:      () => ipcRenderer.invoke('backend:get-logs'),

  // ── Controls ──
  startBackend: () => ipcRenderer.invoke('backend:start'),
  stopBackend:  () => ipcRenderer.invoke('backend:stop'),

  // ── Push events (main → renderer) ──
  onLogLine:       (cb) => ipcRenderer.on('backend:log-line',      (_, d) => cb(d)),
  onStatusChanged: (cb) => ipcRenderer.on('backend:status-changed', (_, d) => cb(d)),
  onConfigChanged: (cb) => ipcRenderer.on('config:changed',       (_, d) => cb(d)),
  removeAllListeners: () => {
    ipcRenderer.removeAllListeners('backend:log-line');
    ipcRenderer.removeAllListeners('backend:status-changed');
    ipcRenderer.removeAllListeners('config:changed');
  },

  // ── Config ──
  getConfig:         ()    => ipcRenderer.invoke('config:get'),
  setConfig:         (cfg) => ipcRenderer.invoke('config:set', cfg),
  getDefaultLogPath: ()    => ipcRenderer.invoke('config:get-default-log-path'),
  getDefaultEnvVars: ()    => ipcRenderer.invoke('config:get-default-env-vars'),

  // ── Dialogs ──
  pickLogFile:   ()        => ipcRenderer.invoke('dialog:pick-log-file'),
  pickExecutable: ()       => ipcRenderer.invoke('dialog:pick-executable'),
  saveLogToFile: (content) => ipcRenderer.invoke('log:save-to-file', content),
});
