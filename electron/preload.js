const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  getServerStatus: () => ipcRenderer.invoke('get-server-status'),
  startServer: () => ipcRenderer.invoke('start-server'),
  stopServer: () => ipcRenderer.invoke('stop-server'),
  restartServer: () => ipcRenderer.invoke('restart-server'),
  getLogs: () => ipcRenderer.invoke('get-logs'),
  clearLogs: () => ipcRenderer.invoke('clear-logs'),
  getConfig: () => ipcRenderer.invoke('get-config'),
  
  onServerStatus: (callback) => {
    const handler = (event, data) => callback(data);
    ipcRenderer.on('server-status', handler);
    return () => ipcRenderer.removeListener('server-status', handler);
  },
  
  onLogEntry: (callback) => {
    const handler = (event, data) => callback(data);
    ipcRenderer.on('log-entry', handler);
    return () => ipcRenderer.removeListener('log-entry', handler);
  },
});
