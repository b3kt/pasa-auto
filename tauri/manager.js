(function () {
  'use strict';

  const api = window.__TAURI__ || {};

  async function invoke(cmd, args) {
    return api.invoke(cmd, args);
  }

  async function listen(event, cb) {
    return api.event.listen(event, cb);
  }

  async function Unlisten(event) {
    if (api.event && api.event.unlisten) {
      await api.event.unlisten(event);
    }
  }

  const DOM = {
    statusDot: document.getElementById('statusDot'),
    statusLabel: document.getElementById('statusLabel'),
    pidLabel: document.getElementById('pidLabel'),
    uptimeLabel: document.getElementById('uptimeLabel'),
    startBtn: document.getElementById('startBtn'),
    stopBtn: document.getElementById('stopBtn'),
    console: document.getElementById('console'),
    clearBtn: document.getElementById('clearBtn'),
    saveBtn: document.getElementById('saveBtn'),
    autoScrollCheck: document.getElementById('autoScrollCheck'),
    logToFileCheck: document.getElementById('logToFileCheck'),
    logFilePathInput: document.getElementById('logFilePathInput'),
    filePathRow: document.getElementById('filePathRow'),
    browseBtn: document.getElementById('browseBtn'),
  };

  let currentState = 'stopped';
  let startTime = null;
  let uptimeTimer = null;
  let autoScroll = true;

  function formatUptime(ms) {
    const s = Math.floor(ms / 1000);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return [h, m, sec].map((v) => String(v).padStart(2, '0')).join(':');
  }

  function startUptimeTick() {
    stopUptimeTick();
    if (!startTime) return;
    uptimeTimer = setInterval(() => {
      DOM.uptimeLabel.textContent = 'Uptime: ' + formatUptime(Date.now() - startTime);
    }, 1000);
    DOM.uptimeLabel.textContent = 'Uptime: ' + formatUptime(Date.now() - startTime);
  }

  function stopUptimeTick() {
    if (uptimeTimer) { clearInterval(uptimeTimer); uptimeTimer = null; }
    DOM.uptimeLabel.textContent = '';
  }

  const STATE_LABELS = {
    stopped: 'Stopped', starting: 'Starting…',
    running: 'Running', stopping: 'Stopping…',
  };

  function applyStatus({ state, pid, start_time }) {
    currentState = state;
    startTime = start_time || null;

    DOM.statusDot.className = 'status-dot ' + state;
    DOM.statusLabel.textContent = STATE_LABELS[state] || state;
    DOM.pidLabel.textContent = pid ? 'PID: ' + pid : '';

    if (state === 'running' && startTime) {
      startUptimeTick();
    } else {
      stopUptimeTick();
    }

    DOM.startBtn.disabled = state !== 'stopped';
    DOM.stopBtn.disabled = state !== 'running';
  }

  function getLogClass(text) {
    if (text.startsWith('[manager]')) return 'log-manager';
    const u = text.toUpperCase();
    if (u.includes(' ERROR') || u.includes('[ERROR]') || u.includes('ERROR:')) return 'log-error';
    if (u.includes(' WARN') || u.includes('[WARN]') || u.includes('WARN:')) return 'log-warn';
    if (u.includes(' DEBUG') || u.includes('[DEBUG]') || u.includes('DEBUG:')) return 'log-debug';
    if (u.includes(' INFO') || u.includes('[INFO]') || u.includes('INFO:')) return 'log-info';
    return 'log-default';
  }

  function appendLogEntry({ time, text }) {
    const line = document.createElement('div');
    line.className = 'log-line ' + getLogClass(text);
    const ts = time ? new Date(time).toLocaleTimeString() : '';
    line.textContent = ts ? ts + '  ' + text : text;
    DOM.console.appendChild(line);

    while (DOM.console.childElementCount > 2000) {
      DOM.console.removeChild(DOM.console.firstChild);
    }

    if (autoScroll) {
      DOM.console.scrollTop = DOM.console.scrollHeight;
    }
  }

  DOM.console.addEventListener('scroll', () => {
    const atBottom = DOM.console.scrollHeight - DOM.console.scrollTop - DOM.console.clientHeight < 30;
    if (autoScroll !== atBottom) {
      autoScroll = atBottom;
      DOM.autoScrollCheck.checked = atBottom;
    }
  });

  DOM.autoScrollCheck.addEventListener('change', () => {
    autoScroll = DOM.autoScrollCheck.checked;
    if (autoScroll) DOM.console.scrollTop = DOM.console.scrollHeight;
  });

  DOM.startBtn.addEventListener('click', async () => {
    DOM.startBtn.disabled = true;
    try { await invoke('start_backend_cmd'); } catch (e) { console.error(e); }
  });

  DOM.stopBtn.addEventListener('click', async () => {
    DOM.stopBtn.disabled = true;
    try { await invoke('stop_backend_cmd'); } catch (e) { console.error(e); }
  });

  DOM.clearBtn.addEventListener('click', () => { DOM.console.innerHTML = ''; });

  DOM.saveBtn.addEventListener('click', async () => {
    const { save } = await import('@tauri-apps/api/dialog');
    const { writeTextFile } = await import('@tauri-apps/api/fs');
    const lines = Array.from(DOM.console.querySelectorAll('.log-line')).map(el => el.textContent).join('\n');
    const filePath = await save({
      defaultPath: 'pasa-auto-' + Date.now() + '.log',
      filters: [{ name: 'Log', extensions: ['log', 'txt'] }]
    });
    if (filePath) await writeTextFile(filePath, lines);
  });

  async function applyLogToFileToggle(checked) {
    DOM.filePathRow.style.display = checked ? 'flex' : 'none';
    const logFilePath = DOM.logFilePathInput.value || (await invoke('get_default_log_path'));
    await invoke('set_config', { config: { log_to_file: checked, log_file_path: logFilePath } });
  }

  DOM.logToFileCheck.addEventListener('change', () => applyLogToFileToggle(DOM.logToFileCheck.checked));

  DOM.browseBtn.addEventListener('click', async () => {
    const { open } = await import('@tauri-apps/api/dialog');
    const filePath = await open({
      title: 'Choose Log File',
      defaultPath: await invoke('get_default_log_path'),
      filters: [{ name: 'Log', extensions: ['log', 'txt'] }]
    });
    if (filePath) {
      DOM.logFilePathInput.value = filePath;
      await invoke('set_config', { config: { log_to_file: DOM.logToFileCheck.checked, log_file_path: filePath } });
    }
  });

  async function init() {
    const unlistenLog = await listen('backend:log-line', e => appendLogEntry(e.payload));
    const unlistenStatus = await listen('backend:status-changed', e => applyStatus(e.payload));

    const [status, logs, cfg] = await Promise.all([
      invoke('get_status'),
      invoke('get_logs'),
      invoke('get_config'),
    ]);

    logs.forEach(appendLogEntry);
    applyStatus(status);

    DOM.logToFileCheck.checked = cfg.log_to_file;
    DOM.logFilePathInput.value = cfg.log_file_path || (await invoke('get_default_log_path'));
    DOM.filePathRow.style.display = cfg.log_to_file ? 'flex' : 'none';

    window.addEventListener('beforeunload', () => {
      unlistenLog();
      unlistenStatus();
    });
  }

  init().catch(console.error);
})();