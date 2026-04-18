(function () {
  'use strict';

  const api = window.electronAPI;

  // ── DOM refs ──────────────────────────────────────────────────────────────
  const statusDot      = document.getElementById('statusDot');
  const statusLabel    = document.getElementById('statusLabel');
  const pidLabel       = document.getElementById('pidLabel');
  const uptimeLabel    = document.getElementById('uptimeLabel');
  const startBtn       = document.getElementById('startBtn');
  const stopBtn        = document.getElementById('stopBtn');
  const consoleEl      = document.getElementById('console');
  const clearBtn       = document.getElementById('clearBtn');
  const saveBtn        = document.getElementById('saveBtn');
  const autoScrollCheck   = document.getElementById('autoScrollCheck');
  const logToFileCheck    = document.getElementById('logToFileCheck');
  const logFilePathInput  = document.getElementById('logFilePathInput');
  const filePathRow    = document.getElementById('filePathRow');
  const browseBtn      = document.getElementById('browseBtn');

  // ── Local state ───────────────────────────────────────────────────────────
  let currentState  = 'stopped';
  let startTime     = null;
  let uptimeTimer   = null;
  let autoScroll    = true;

  // ── Uptime ────────────────────────────────────────────────────────────────
  function formatUptime(ms) {
    const s   = Math.floor(ms / 1000);
    const h   = Math.floor(s / 3600);
    const m   = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return [h, m, sec].map((v) => String(v).padStart(2, '0')).join(':');
  }

  function startUptimeTick() {
    stopUptimeTick();
    if (!startTime) return;
    uptimeTimer = setInterval(() => {
      uptimeLabel.textContent = 'Uptime: ' + formatUptime(Date.now() - startTime);
    }, 1000);
    // update immediately
    uptimeLabel.textContent = 'Uptime: ' + formatUptime(Date.now() - startTime);
  }

  function stopUptimeTick() {
    if (uptimeTimer) { clearInterval(uptimeTimer); uptimeTimer = null; }
    uptimeLabel.textContent = '';
  }

  // ── Status display ────────────────────────────────────────────────────────
  const STATE_LABELS = {
    stopped: 'Stopped', starting: 'Starting\u2026',
    running: 'Running', stopping: 'Stopping\u2026',
  };

  function applyStatus({ state, pid, startTime: st }) {
    currentState = state;
    startTime    = st || null;

    statusDot.className    = `status-dot ${state}`;
    statusLabel.textContent = STATE_LABELS[state] || state;
    pidLabel.textContent    = pid ? `PID: ${pid}` : '';

    if (state === 'running' && startTime) {
      startUptimeTick();
    } else {
      stopUptimeTick();
    }

    startBtn.disabled = state !== 'stopped';
    stopBtn.disabled  = state !== 'running';
  }

  // ── Log rendering ─────────────────────────────────────────────────────────
  function getLogClass(text) {
    if (text.startsWith('[manager]')) return 'log-manager';
    const u = text.toUpperCase();
    if (u.includes(' ERROR') || u.includes('[ERROR]') || u.includes('ERROR:')) return 'log-error';
    if (u.includes(' WARN')  || u.includes('[WARN]')  || u.includes('WARN:'))  return 'log-warn';
    if (u.includes(' DEBUG') || u.includes('[DEBUG]') || u.includes('DEBUG:')) return 'log-debug';
    if (u.includes(' INFO')  || u.includes('[INFO]')  || u.includes('INFO:'))  return 'log-info';
    return 'log-default';
  }

  function appendLogEntry({ time, text }) {
    const line = document.createElement('div');
    line.className = `log-line ${getLogClass(text)}`;
    const ts = time ? new Date(time).toLocaleTimeString() : '';
    line.textContent = ts ? `${ts}  ${text}` : text;
    consoleEl.appendChild(line);

    // Prune DOM to stay within 2000 visible lines
    while (consoleEl.childElementCount > 2000) {
      consoleEl.removeChild(consoleEl.firstChild);
    }

    if (autoScroll) {
      consoleEl.scrollTop = consoleEl.scrollHeight;
    }
  }

  // ── Auto-scroll sync ──────────────────────────────────────────────────────
  consoleEl.addEventListener('scroll', () => {
    const atBottom =
      consoleEl.scrollHeight - consoleEl.scrollTop - consoleEl.clientHeight < 30;
    if (autoScroll !== atBottom) {
      autoScroll = atBottom;
      autoScrollCheck.checked = atBottom;
    }
  });

  autoScrollCheck.addEventListener('change', () => {
    autoScroll = autoScrollCheck.checked;
    if (autoScroll) consoleEl.scrollTop = consoleEl.scrollHeight;
  });

  // ── Controls ──────────────────────────────────────────────────────────────
  startBtn.addEventListener('click', async () => {
    startBtn.disabled = true;
    await api.startBackend();
  });

  stopBtn.addEventListener('click', async () => {
    stopBtn.disabled = true;
    await api.stopBackend();
  });

  clearBtn.addEventListener('click', () => {
    consoleEl.innerHTML = '';
  });

  saveBtn.addEventListener('click', async () => {
    const content = Array.from(consoleEl.querySelectorAll('.log-line'))
      .map((el) => el.textContent)
      .join('\n');
    await api.saveLogToFile(content);
  });

  // ── Settings ──────────────────────────────────────────────────────────────
  async function applyLogToFileToggle(checked) {
    filePathRow.style.display = checked ? 'flex' : 'none';
    const logFilePath = logFilePathInput.value || (await api.getDefaultLogPath());
    await api.setConfig({ logToFile: checked, logFilePath });
  }

  logToFileCheck.addEventListener('change', () =>
    applyLogToFileToggle(logToFileCheck.checked),
  );

  browseBtn.addEventListener('click', async () => {
    const filePath = await api.pickLogFile();
    if (filePath) {
      logFilePathInput.value = filePath;
      await api.setConfig({ logToFile: logToFileCheck.checked, logFilePath: filePath });
    }
  });

  // ── Initialise ────────────────────────────────────────────────────────────
  async function init() {
    const [status, logs, cfg] = await Promise.all([
      api.getStatus(),
      api.getLogs(),
      api.getConfig(),
    ]);

    // Replay buffered logs first so the console is populated before status fires
    logs.forEach(appendLogEntry);

    applyStatus(status);

    // Settings
    logToFileCheck.checked = cfg.logToFile;
    logFilePathInput.value = cfg.logFilePath || (await api.getDefaultLogPath());
    filePathRow.style.display = cfg.logToFile ? 'flex' : 'none';

    // Subscribe to live events from main process
    api.onLogLine(appendLogEntry);
    api.onStatusChanged(applyStatus);
  }

  window.addEventListener('beforeunload', () => api.removeAllListeners());
  init().catch(console.error);
})();
