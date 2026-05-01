#!/usr/bin/env node

const { spawn } = require('child_process');
const path = require('path');

console.log('🚀 Starting PazaAuto in development mode...');

// Start Electron in dev mode
const electron = spawn('electron', ['.'], {
  cwd: process.cwd(),
  env: { ...process.env, NODE_ENV: 'development' },
  stdio: 'inherit'
});

electron.on('close', (code) => {
  process.exit(code);
});

electron.on('error', (err) => {
  console.error('Failed to start Electron:', err);
  process.exit(1);
});
