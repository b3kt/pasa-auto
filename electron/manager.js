(function () {
  'use strict';

  // Wait for electronAPI to be available
  let api;
  function waitForAPI() {
    if (window.electronAPI) {
      api = window.electronAPI;
      init();
    } else {
      setTimeout(waitForAPI, 50);
    }
  }

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

  // Backend configuration refs
  const executablePathInput = document.getElementById('executablePathInput');
  const browseExecutableBtn = document.getElementById('browseExecutableBtn');
  const propertiesList = document.getElementById('propertiesList');
  const newPropertyKey = document.getElementById('newPropertyKey');
  const newPropertyValue = document.getElementById('newPropertyValue');
  const addPropertyBtn = document.getElementById('addPropertyBtn');
  const envvarsList = document.getElementById('envvarsList');
  const newEnvVarKey = document.getElementById('newEnvVarKey');
  const newEnvVarValue = document.getElementById('newEnvVarValue');
  const addEnvVarBtn = document.getElementById('addEnvVarBtn');

  // Tab refs
  const tabBtns = document.querySelectorAll('.tab-btn');
  const tabPanes = document.querySelectorAll('.tab-pane');

  // ── Local state ───────────────────────────────────────────────────────────
  let currentState  = 'stopped';
  let startTime     = null;
  let uptimeTimer   = null;
  let autoScroll    = true;
  let defaultEnvVars = {};
  let activeTab = 'console';

  // ── Tab Management ──────────────────────────────────────────────────────────────
  function switchTab(tabName) {
    // Update button states
    tabBtns.forEach(btn => {
      if (btn.dataset.tab === tabName) {
        btn.classList.add('active');
      } else {
        btn.classList.remove('active');
      }
    });

    // Update pane visibility
    tabPanes.forEach(pane => {
      if (pane.id === `${tabName}-tab`) {
        pane.classList.add('active');
      } else {
        pane.classList.remove('active');
      }
    });

    activeTab = tabName;
  }

  function initializeTabs() {
    tabBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        switchTab(btn.dataset.tab);
      });
    });
  }

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

  // ── Initialize Event Listeners ──────────────────────────────────────────────────────────────
  function initializeEventListeners() {
    // Initialize tabs first
    initializeTabs();

    // Controls
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

    // Settings
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

    // Backend configuration event listeners
    browseExecutableBtn.addEventListener('click', async () => {
      const executablePath = await api.pickExecutable();
      if (executablePath) {
        executablePathInput.value = executablePath;
        await api.setConfig({ executablePath });
      }
    });

    executablePathInput.addEventListener('change', async () => {
      await api.setConfig({ executablePath: executablePathInput.value });
    });

    addPropertyBtn.addEventListener('click', async () => {
      const key = newPropertyKey.value.trim();
      const value = newPropertyValue.value.trim();
      if (key && value) {
        const currentConfig = await api.getConfig();
        const properties = { ...currentConfig.additionalProperties };
        properties[key] = value;
        await api.setConfig({ additionalProperties: properties });
        newPropertyKey.value = '';
        newPropertyValue.value = '';
      }
    });

    propertiesList.addEventListener('click', async (e) => {
      if (e.target.classList.contains('property-remove')) {
        const key = e.target.dataset.key;
        const currentConfig = await api.getConfig();
        const properties = { ...currentConfig.additionalProperties };
        delete properties[key];
        await api.setConfig({ additionalProperties: properties });
      }
    });

    addEnvVarBtn.addEventListener('click', async () => {
      const key = newEnvVarKey.value.trim();
      const value = newEnvVarValue.value.trim();
      if (key && value) {
        const currentConfig = await api.getConfig();
        const envvars = { ...currentConfig.environmentVariables };
        envvars[key] = value;
        await api.setConfig({ environmentVariables: envvars });
        newEnvVarKey.value = '';
        newEnvVarValue.value = '';
      }
    });

    envvarsList.addEventListener('click', async (e) => {
      if (e.target.classList.contains('envvar-remove')) {
        const key = e.target.dataset.key;
        const currentConfig = await api.getConfig();
        const envvars = { ...currentConfig.environmentVariables };
        
        if (defaultEnvVars.hasOwnProperty(key)) {
          // For default variables, remove the custom value to revert to default
          delete envvars[key];
        } else {
          // For custom variables, remove completely
          delete envvars[key];
        }
        
        await api.setConfig({ environmentVariables: envvars });
      }
    });
  }

  // ── Backend Configuration Management ──────────────────────────────────────────────────────────────
  function renderProperties(properties) {
    propertiesList.innerHTML = '';
    for (const [key, value] of Object.entries(properties)) {
      const item = document.createElement('div');
      item.className = 'property-item';
      item.innerHTML = `
        <span class="property-key">${key}</span>
        <span class="property-value">${value}</span>
        <button class="property-remove" data-key="${key}">×</button>
      `;
      propertiesList.appendChild(item);
    }
  }

  function renderEnvVars(envvars, defaultEnvVars = {}) {
    envvarsList.innerHTML = '';
    for (const [key, value] of Object.entries(envvars)) {
      const item = document.createElement('div');
      const isDefault = defaultEnvVars.hasOwnProperty(key);
      item.className = `envvar-item ${isDefault ? 'envvar-default' : 'envvar-custom'}`;
      item.innerHTML = `
        <span class="envvar-key">${key}${isDefault ? ' *' : ''}</span>
        <span class="envvar-value">${value}</span>
        <button class="envvar-remove" data-key="${key}" title="${isDefault ? 'Remove custom value (will revert to default)' : 'Remove environment variable'}">×</button>
      `;
      envvarsList.appendChild(item);
    }
  }

  async function updateBackendConfig(config) {
    await api.setConfig(config);
  }

  // ── Settings ──────────────────────────────────────────────────────────────
  async function applyLogToFileToggle(checked) {
    filePathRow.style.display = checked ? 'flex' : 'none';
    const logFilePath = logFilePathInput.value || (await api.getDefaultLogPath());
    await api.setConfig({ logToFile: checked, logFilePath });
  }

  // ── Initialise ────────────────────────────────────────────────────────────
  async function init() {
    // Initialize event listeners first
    initializeEventListeners();

    const [status, logs, cfg, defaultEnv] = await Promise.all([
      api.getStatus(),
      api.getLogs(),
      api.getConfig(),
      api.getDefaultEnvVars(),
    ]);

    // Store default environment variables for reference
    defaultEnvVars = defaultEnv || {};

    // Replay buffered logs first so the console is populated before status fires
    logs.forEach(appendLogEntry);

    applyStatus(status);

    // Settings
    logToFileCheck.checked = cfg.logToFile;
    logFilePathInput.value = cfg.logFilePath || (await api.getDefaultLogPath());
    filePathRow.style.display = cfg.logToFile ? 'flex' : 'none';

    // Backend Configuration
    executablePathInput.value = cfg.executablePath || '';
    renderProperties(cfg.additionalProperties || {});
    renderEnvVars(cfg.environmentVariables || {}, defaultEnvVars);

    // Subscribe to live events from main process
    api.onLogLine(appendLogEntry);
    api.onStatusChanged(applyStatus);
    
    // Subscribe to configuration changes
    api.onConfigChanged((newConfig) => {
      renderProperties(newConfig.additionalProperties || {});
      renderEnvVars(newConfig.environmentVariables || {}, defaultEnvVars);
      executablePathInput.value = newConfig.executablePath || '';
    });
  }

  window.addEventListener('beforeunload', () => api && api.removeAllListeners());
  
  // Start the initialization process
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', waitForAPI);
  } else {
    waitForAPI();
  }
})();
