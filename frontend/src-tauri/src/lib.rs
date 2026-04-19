use tauri::Manager;
use tokio::sync::oneshot;

mod server;

#[derive(Debug, serde::Serialize)]
struct ServerStatus {
    port: u16,
    running: bool,
}

#[tauri::command]
async fn get_server_status(port: tauri::State<'_, u16>) -> Result<ServerStatus, String> {
    Ok(ServerStatus {
        port: *port.inner(),
        running: true,
    })
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let (tx, rx) = oneshot::channel::<u16>();
    
    std::thread::spawn(move || {
        let rt = tokio::runtime::Runtime::new().expect("Failed to create tokio runtime");
        rt.block_on(async {
            match server::start_embedded_server(tx).await {
                Ok(_) => {
                    tracing::info!("Embedded server stopped");
                }
                Err(e) => {
                    tracing::error!("Embedded server error: {}", e);
                }
            }
        });
    });
    
    let server_port = rx.blocking_recv().expect("Failed to receive server port");
    tracing::info!("Backend server started on port {}", server_port);
    
    let server_url = format!("http://127.0.0.1:{}", server_port);
    tracing::info!("Will load frontend from {}", server_url);
    
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(move |app| {
            app.manage(server_port);
            
            let window = app.get_webview_window("main").unwrap();
            let url = server_url.clone();
            
            std::thread::spawn(move || {
                std::thread::sleep(std::time::Duration::from_millis(500));
                let _ = window.eval(&format!("window.location.replace('{}')", url));
            });
            
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![get_server_status])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
