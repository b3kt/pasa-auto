use std::sync::Mutex;
use std::process::Command;
use std::path::PathBuf;
use std::thread;
use std::time::Duration;

use tauri::{AppHandle, Manager, Emitter, WebviewWindowBuilder, WebviewUrl};
use serde::{Deserialize, Serialize};

const QUARKUS_PORT: u16 = 8080;
const HEALTH_CHECK_INTERVAL_MS: u64 = 1500;
const HEALTH_CHECK_TIMEOUT_MS: u64 = 60000;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BackendStatus {
    state: String,
    pid: Option<u32>,
    start_time: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LogEntry {
    time: String,
    text: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    log_to_file: bool,
    log_file_path: String,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            log_to_file: false,
            log_file_path: String::new(),
        }
    }
}

struct AppState {
    backend: Mutex<Option<std::process::Child>>,
    status: Mutex<BackendStatus>,
    logs: Mutex<Vec<LogEntry>>,
    config: Mutex<AppConfig>,
}

fn find_backend_binary() -> Option<String> {
    let packaged = std::env::current_exe()
        .ok()
        .and_then(|p| p.parent().map(|p| p.join("pasa-auto-runner")))
        .filter(|p| p.exists())
        .map(|p| p.to_string_lossy().to_string());
    
    if packaged.is_some() {
        return packaged;
    }

    let dev = PathBuf::from("bin/pasa-auto-runner");
    if dev.exists() {
        return Some(dev.to_string_lossy().to_string());
    }

    None
}

fn check_port_in_use(port: u16) -> bool {
    std::net::TcpListener::bind(format!("127.0.0.1:{}", port)).is_err()
}

fn check_server_ready() -> bool {
    std::net::TcpStream::connect_timeout(
        &std::net::SocketAddr::from(([127, 0, 0, 1], QUARKUS_PORT)),
        Duration::from_millis(500),
    ).is_ok()
}

fn append_log(app: &AppHandle, text: &str) {
    let entry = LogEntry {
        time: chrono::Utc::now().to_rfc3339(),
        text: text.to_string(),
    };
    
    if let Some(state) = app.try_state::<AppState>() {
        let mut logs = state.logs.lock().unwrap();
        logs.push(entry.clone());
        if logs.len() > 2000 {
            logs.remove(0);
        }
    }
    
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.emit("backend:log-line", entry);
    }
}

fn start_backend(app: &AppHandle) -> Result<BackendStatus, String> {
    let state = app.state::<AppState>();
    
    {
        let status = state.status.lock().unwrap();
        if status.state != "stopped" {
            return Err("Backend is not stopped".to_string());
        }
    }
    
    if check_port_in_use(QUARKUS_PORT) {
        return Err(format!("Port {} is already in use", QUARKUS_PORT));
    }
    
    let binary_path = find_backend_binary()
        .ok_or_else(|| "Binary not found".to_string())?;
    
    #[cfg(windows)]
    {
        let _ = Command::new("cmd")
            .args(["/C", "attrib", "-R", &binary_path])
            .output();
    }
    
    let child = Command::new(&binary_path)
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .spawn()
        .map_err(|e| format!("Failed to start: {}", e))?;
    
    let pid = child.id();
    let status = BackendStatus {
        state: "starting".to_string(),
        pid: Some(pid),
        start_time: Some(chrono::Utc::now().timestamp_millis()),
    };
    
    *state.status.lock().unwrap() = status.clone();
    *state.backend.lock().unwrap() = Some(child);
    
    append_log(app, &format!("Starting backend: {} (PID {})", binary_path, pid));
    
    start_health_check(app);
    
    Ok(status)
}

fn start_health_check(app: &AppHandle) {
    let app_clone = app.clone();
    thread::spawn(move || {
        let mut elapsed = 0u64;
        
        loop {
            thread::sleep(Duration::from_millis(HEALTH_CHECK_INTERVAL_MS));
            elapsed += HEALTH_CHECK_INTERVAL_MS;
            
            if elapsed >= HEALTH_CHECK_TIMEOUT_MS {
                append_log(&app_clone, "Health check timed out after 60s");
                stop_backend(&app_clone);
                return;
            }
            
            if check_server_ready() {
                let state = app_clone.state::<AppState>();
                let status = state.status.lock().unwrap();
                let running_status = BackendStatus {
                    state: "running".to_string(),
                    pid: status.pid,
                    start_time: status.start_time,
                };
                drop(status);
                
                if let Some(window) = app_clone.get_webview_window("main") {
                    let _ = window.emit("backend:status-changed", running_status);
                }
                
                append_log(&app_clone, &format!("Backend ready on port {}", QUARKUS_PORT));
                return;
            }
        }
    });
}

fn stop_backend(app: &AppHandle) {
    let state = app.state::<AppState>();
    let mut backend = state.backend.lock().unwrap();
    
    if let Some(ref mut child) = *backend {
        append_log(app, "Stopping backend...");
        let _ = child.kill();
    }
    *backend = None;
    
    let mut status = state.status.lock().unwrap();
    status.state = "stopped".to_string();
    status.pid = None;
    status.start_time = None;
    
    let stopped_status = status.clone();
    let _ = app.emit("backend:status-changed", stopped_status);
}

#[tauri::command]
fn get_status(state: tauri::State<AppState>) -> BackendStatus {
    state.status.lock().unwrap().clone()
}

#[tauri::command]
fn get_logs(state: tauri::State<AppState>) -> Vec<LogEntry> {
    state.logs.lock().unwrap().clone()
}

#[tauri::command]
fn start_backend_cmd(app: AppHandle) -> Result<BackendStatus, String> {
    start_backend(&app)
}

#[tauri::command]
fn stop_backend_cmd(app: AppHandle) {
    stop_backend(&app)
}

#[tauri::command]
fn get_config(state: tauri::State<AppState>) -> AppConfig {
    state.config.lock().unwrap().clone()
}

#[tauri::command]
fn set_config(state: tauri::State<AppState>, config: AppConfig) -> AppConfig {
    *state.config.lock().unwrap() = config.clone();
    config
}

#[tauri::command]
fn get_default_log_path(app: AppHandle) -> String {
    app.path()
        .app_data_dir()
        .map(|p| p.join("paza-auto-backend.log").to_string_lossy().to_string())
        .unwrap_or_default()
}

pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .manage(AppState {
            backend: Mutex::new(None),
            status: Mutex::new(BackendStatus {
                state: "stopped".to_string(),
                pid: None,
                start_time: None,
            }),
            logs: Mutex::new(Vec::new()),
            config: Mutex::new(AppConfig::default()),
        })
        .setup(|app| {
            let _window = WebviewWindowBuilder::new(
                app,
                "main",
                WebviewUrl::App("manager.html".into()),
            ).title("PazaAuto Manager")
                .inner_size(900.0, 650.0)
                .min_inner_size(700.0, 500.0)
                .center()
                .build()
                .unwrap();
            
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            get_status,
            get_logs,
            start_backend_cmd,
            stop_backend_cmd,
            get_config,
            set_config,
            get_default_log_path,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}