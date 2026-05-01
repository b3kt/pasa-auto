# PazaAuto Electron App

## Development

### Prerequisites
- Node.js installed
- Backend binary built at `bin/pasa-auto-runner`

### Running in Development Mode

#### Simple Dev Mode
```bash
npm run dev:simple
```
This starts Electron with NODE_ENV=development and opens DevTools automatically.

#### Enhanced Dev Mode
```bash
npm run dev
```
Uses the enhanced dev script with better logging.

#### Debug Mode
```bash
npm run dev:debug
```
Starts with Node.js debugger enabled on port 5858.

### Production Mode
```bash
npm start
```

### Building
```bash
# Build for current platform
npm run dist

# Build for Linux
npm run dist:linux

# Build for Windows
npm run dist:windows
```

## Development Features

- **DevTools**: Automatically opened in development mode
- **Hot Reload**: Windows reload when backend becomes ready
- **Error Handling**: Better error messages and fallback UI
- **Debug Logging**: Console logs show development mode status

## Troubleshooting

### Backend Not Found
Ensure the backend binary exists at `bin/pasa-auto-runner`. If not, build the project first.

### Port Conflict
If port 8080 is already in use, stop the conflicting process before starting the app.

### Connection Refused
The error `ERR_CONNECTION_REFUSED` is normal at startup - the app will automatically reload when the backend is ready.
