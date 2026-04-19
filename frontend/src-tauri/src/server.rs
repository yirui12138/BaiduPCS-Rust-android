use axum::{
    middleware,
    routing::{delete, get, post, put},
    Json, Router,
};
use baidu_netdisk_rust::{
    config::LogConfig, logging, server::handlers, server::websocket,
    web_auth::{self, WebAuthState},
    common::proxy_fallback::ProxyHotUpdater,
    AppState,
};
use serde::Serialize;
use std::path::PathBuf;
use std::sync::Arc;
use tokio::sync::oneshot;
use tower::ServiceBuilder;
use tower_http::{
    cors::{Any, CorsLayer},
    services::{ServeDir, ServeFile},
    trace::TraceLayer,
};
use tracing::info;

fn detect_frontend_dir() -> PathBuf {
    let mut candidates: Vec<PathBuf> = vec![];
    
    if let Ok(exe_path) = std::env::current_exe() {
        if let Some(exe_dir) = exe_path.parent() {
            candidates.push(exe_dir.join("dist"));
            candidates.push(exe_dir.join("frontend").join("dist"));
            candidates.push(exe_dir.join("web"));
            if let Some(parent) = exe_dir.parent() {
                candidates.push(parent.join("dist"));
                candidates.push(parent.join("frontend").join("dist"));
                candidates.push(parent.join("web"));
                if let Some(grandparent) = parent.parent() {
                    candidates.push(grandparent.join("frontend").join("dist"));
                    candidates.push(grandparent.join("dist"));
                }
            }
        }
    }
    
    candidates.push(PathBuf::from("./dist"));
    candidates.push(PathBuf::from("../dist"));
    candidates.push(PathBuf::from("./frontend/dist"));
    candidates.push(PathBuf::from("../frontend/dist"));
    candidates.push(PathBuf::from("../../frontend/dist"));
    candidates.push(PathBuf::from("/app/frontend/dist"));
    candidates.push(PathBuf::from("/app/dist"));

    for path in &candidates {
        if path.exists() && path.is_dir() {
            if path.join("index.html").exists() {
                match path.canonicalize() {
                    Ok(canonical) => {
                        info!("Found frontend directory: {:?}", canonical);
                        return canonical;
                    }
                    Err(_) => {
                        info!("Found frontend directory: {:?}", path);
                        return path.clone();
                    }
                }
            }
        }
    }

    let default = PathBuf::from("./dist");
    tracing::error!(
        "Frontend directory not found! Searched: {:?}",
        candidates
    );
    default
}

async fn load_log_config() -> LogConfig {
    let config_path = "config/app.toml";
    if let Ok(content) = tokio::fs::read_to_string(config_path).await {
        if let Ok(config) = toml::from_str::<toml::Value>(&content) {
            if let Some(log_table) = config.get("log") {
                if let Ok(log_config) = log_table.clone().try_into::<LogConfig>() {
                    return log_config;
                }
            }
        }
    }
    LogConfig::default()
}

async fn ensure_download_dir_accessible(download_dir: &std::path::Path) {
    if !download_dir.exists() {
        match std::fs::create_dir_all(download_dir) {
            Ok(_) => {
                info!("Download directory created: {:?}", download_dir);
                return;
            }
            Err(e) => {
                info!(
                    "Failed to create download directory {:?}: {}",
                    download_dir, e
                );
                return;
            }
        }
    }

    let test_file = download_dir.join(".baidu_write_test");
    match std::fs::write(&test_file, b"permission_test") {
        Ok(_) => {
            let _ = std::fs::remove_file(&test_file);
            info!("Download directory permission verified: {:?}", download_dir);
        }
        Err(e) => {
            info!(
                "Download directory permission denied {:?}: {}",
                download_dir, e
            );
        }
    }
}

async fn init_web_auth_state(config: &baidu_netdisk_rust::config::WebAuthConfig) -> Arc<WebAuthState> {
    use baidu_netdisk_rust::web_auth::create_auth_store;

    let auth_store = Arc::new(create_auth_store());
    if let Err(e) = auth_store.load().await {
        tracing::warn!("Failed to load auth credentials: {}", e);
    }

    let credentials = auth_store.get_credentials().await;

    let state = WebAuthState::new(
        config.clone(),
        credentials,
        None,
        auth_store,
    );

    let state = Arc::new(state);
    state.start_cleanup_tasks().await;
    state
}

#[derive(Serialize)]
struct HealthResponse {
    status: String,
    service: String,
}

async fn health_check() -> Json<HealthResponse> {
    Json(HealthResponse {
        status: "ok".to_string(),
        service: "baidu-netdisk-rust".to_string(),
    })
}

pub async fn start_embedded_server(port_tx: oneshot::Sender<u16>) -> anyhow::Result<()> {
    let log_config = load_log_config().await;
    let _log_guard = logging::init_logging(&log_config);

    info!("Baidu Netdisk Desktop v1.12.1 starting...");

    let app_state = AppState::new().await?;

    {
        let config = app_state.config.read().await;
        let download_dir = config.download.download_dir.clone();
        drop(config);
        ensure_download_dir_accessible(&download_dir).await;
    }

    app_state.fallback_mgr.set_updater(
        Arc::new(app_state.clone()) as Arc<dyn ProxyHotUpdater>
    ).await;

    {
        let cfg = app_state.config.read().await;
        let proxy = &cfg.network.proxy;
        if proxy.proxy_type != baidu_netdisk_rust::common::ProxyType::None {
            let proxy_clone = proxy.clone();
            let allow_fallback = proxy.allow_fallback;
            drop(cfg);

            app_state.fallback_mgr
                .set_user_proxy_config(Some(proxy_clone.clone()))
                .await;

            match baidu_netdisk_rust::common::probe_proxy(&proxy_clone).await {
                Ok(()) => {
                    info!("Proxy probe successful");
                    app_state.fallback_mgr
                        .set_runtime_status(baidu_netdisk_rust::common::ProxyRuntimeStatus::Normal)
                        .await;
                }
                Err(e) => {
                    tracing::warn!("Proxy probe failed: {}", e);
                    if allow_fallback {
                        app_state.fallback_mgr.execute_fallback().await;
                    } else {
                        app_state.fallback_mgr
                            .set_runtime_status(baidu_netdisk_rust::common::ProxyRuntimeStatus::FallenBackToDirect)
                            .await;
                    }
                }
            }
        }
    }

    let config = app_state.config.read().await.clone();
    let web_auth_state = init_web_auth_state(&config.web_auth).await;
    info!("Web auth initialized (mode: {:?})", config.web_auth.mode);

    let middleware = ServiceBuilder::new()
        .layer(TraceLayer::new_for_http())
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        );

    let api_routes = Router::new()
        .route("/auth/qrcode/generate", post(handlers::generate_qrcode))
        .route("/auth/qrcode/status", get(handlers::qrcode_status))
        .route("/auth/cookie/login", post(handlers::cookie_login))
        .route("/auth/user", get(handlers::get_current_user))
        .route("/auth/logout", post(handlers::logout))
        .route("/files", get(handlers::get_file_list))
        .route("/files/download", get(handlers::get_download_url))
        .route("/files/folder", post(handlers::create_folder))
        .route("/downloads", post(handlers::create_download))
        .route("/downloads", get(handlers::get_all_downloads))
        .route("/downloads/all", get(handlers::get_all_downloads_mixed))
        .route("/downloads/active", get(handlers::get_active_downloads))
        .route("/downloads/batch", post(handlers::create_batch_download))
        .route("/downloads/:id", get(handlers::get_download))
        .route("/downloads/:id/pause", post(handlers::pause_download))
        .route("/downloads/:id/resume", post(handlers::resume_download))
        .route("/downloads/:id", delete(handlers::delete_download))
        .route("/downloads/clear/completed", delete(handlers::clear_completed))
        .route("/downloads/clear/failed", delete(handlers::clear_failed))
        .route("/downloads/batch/pause", post(handlers::batch_pause_downloads))
        .route("/downloads/batch/resume", post(handlers::batch_resume_downloads))
        .route("/downloads/batch/delete", post(handlers::batch_delete_downloads))
        .route("/downloads/folder", post(handlers::create_folder_download))
        .route("/downloads/folders", get(handlers::get_all_folder_downloads))
        .route("/downloads/folder/:id", get(handlers::get_folder_download))
        .route("/downloads/folder/:id/pause", post(handlers::pause_folder_download))
        .route("/downloads/folder/:id/resume", post(handlers::resume_folder_download))
        .route("/downloads/folder/:id", delete(handlers::cancel_folder_download))
        .route("/uploads", post(handlers::create_upload))
        .route("/uploads", get(handlers::get_all_uploads))
        .route("/uploads/:id", get(handlers::get_upload))
        .route("/uploads/:id/pause", post(handlers::pause_upload))
        .route("/uploads/:id/resume", post(handlers::resume_upload))
        .route("/uploads/:id", delete(handlers::delete_upload))
        .route("/uploads/folder", post(handlers::create_folder_upload))
        .route("/uploads/batch", post(handlers::create_batch_upload))
        .route("/uploads/scan/:id", get(handlers::get_scan_status))
        .route("/uploads/scan/:id/cancel", post(handlers::cancel_scan))
        .route("/uploads/clear/completed", post(handlers::clear_completed_uploads))
        .route("/uploads/clear/failed", post(handlers::clear_failed_uploads))
        .route("/uploads/batch/pause", post(handlers::batch_pause_uploads))
        .route("/uploads/batch/resume", post(handlers::batch_resume_uploads))
        .route("/uploads/batch/delete", post(handlers::batch_delete_uploads))
        .route("/transfers", post(handlers::create_transfer))
        .route("/transfers", get(handlers::get_all_transfers))
        .route("/transfers/preview", post(handlers::preview_share_files))
        .route("/transfers/preview/dir", post(handlers::preview_share_dir))
        .route("/transfers/cleanup", post(handlers::cleanup_orphaned_temp_dirs))
        .route("/transfers/:id", get(handlers::get_transfer))
        .route("/transfers/:id", delete(handlers::delete_transfer))
        .route("/transfers/:id/cancel", post(handlers::cancel_transfer))
        .route("/fs/list", get(handlers::list_directory))
        .route("/fs/goto", get(handlers::goto_path))
        .route("/fs/validate", get(handlers::validate_path))
        .route("/fs/roots", get(handlers::get_roots))
        .route("/config", get(handlers::get_config))
        .route("/config", put(handlers::update_config))
        .route("/config/recommended", get(handlers::get_recommended_config))
        .route("/config/reset", post(handlers::reset_to_recommended))
        .route("/config/recent-dir", post(handlers::update_recent_dir))
        .route("/config/default-download-dir", post(handlers::set_default_download_dir))
        .route("/config/transfer", get(handlers::get_transfer_config))
        .route("/config/transfer", put(handlers::update_transfer_config))
        .route("/proxy/status", get(handlers::get_proxy_status))
        .route("/proxy/test", post(handlers::test_proxy_connection))
        .route("/autobackup/configs", get(handlers::autobackup::list_backup_configs))
        .route("/autobackup/configs", post(handlers::autobackup::create_backup_config))
        .route("/autobackup/configs/:id", get(handlers::autobackup::get_backup_config))
        .route("/autobackup/configs/:id", put(handlers::autobackup::update_backup_config))
        .route("/autobackup/configs/:id", delete(handlers::autobackup::delete_backup_config))
        .route("/autobackup/configs/:id/enable", post(handlers::autobackup::enable_backup_config))
        .route("/autobackup/configs/:id/disable", post(handlers::autobackup::disable_backup_config))
        .route("/autobackup/configs/:id/trigger", post(handlers::autobackup::trigger_backup))
        .route("/autobackup/configs/:id/tasks", get(handlers::autobackup::list_backup_tasks))
        .route("/autobackup/tasks/:id", get(handlers::autobackup::get_backup_task))
        .route("/autobackup/tasks/:id/cancel", post(handlers::autobackup::cancel_backup_task))
        .route("/autobackup/tasks/:id/pause", post(handlers::autobackup::pause_backup_task))
        .route("/autobackup/tasks/:id/resume", post(handlers::autobackup::resume_backup_task))
        .route("/autobackup/tasks/:id", delete(handlers::autobackup::delete_backup_task))
        .route("/autobackup/tasks/:id/files", get(handlers::autobackup::list_file_tasks))
        .route("/autobackup/tasks/:task_id/files/:file_task_id/retry", post(handlers::autobackup::retry_file_task))
        .route("/autobackup/status", get(handlers::autobackup::get_manager_status))
        .route("/autobackup/stats", get(handlers::autobackup::get_record_stats))
        .route("/autobackup/cleanup", post(handlers::autobackup::cleanup_records))
        .route("/encryption/status", get(handlers::autobackup::get_encryption_status))
        .route("/encryption/key/generate", post(handlers::autobackup::generate_encryption_key))
        .route("/encryption/key/import", post(handlers::autobackup::import_encryption_key))
        .route("/encryption/key/export", get(handlers::autobackup::export_encryption_key))
        .route("/encryption/key", delete(handlers::autobackup::delete_encryption_key))
        .route("/encryption/key/force", delete(handlers::autobackup::force_delete_encryption_key))
        .route("/encryption/export-bundle", post(handlers::export_bundle))
        .route("/encryption/export-mapping", get(handlers::export_mapping))
        .route("/encryption/export-keys", get(handlers::export_keys))
        .route("/cloud-dl/tasks", post(handlers::cloud_dl::add_task))
        .route("/cloud-dl/tasks", get(handlers::cloud_dl::list_tasks))
        .route("/cloud-dl/tasks/clear", delete(handlers::cloud_dl::clear_tasks))
        .route("/cloud-dl/tasks/refresh", post(handlers::cloud_dl::refresh_tasks))
        .route("/cloud-dl/tasks/:task_id", get(handlers::cloud_dl::query_task))
        .route("/cloud-dl/tasks/:task_id", delete(handlers::cloud_dl::delete_task))
        .route("/cloud-dl/tasks/:task_id/cancel", post(handlers::cloud_dl::cancel_task))
        .route("/shares", post(handlers::create_share))
        .route("/shares", get(handlers::get_share_list))
        .route("/shares/cancel", post(handlers::cancel_share))
        .route("/shares/:id", get(handlers::get_share_detail))
        .route("/system/watch-capability", get(handlers::autobackup::get_watch_capability))
        .route("/config/autobackup/trigger", get(handlers::autobackup::get_trigger_config))
        .route("/config/autobackup/trigger", put(handlers::autobackup::update_trigger_config))
        .route("/ws", get(websocket::handle_websocket))
        .with_state(app_state.clone())
        .layer(middleware::from_fn_with_state(
            web_auth_state.clone(),
            web_auth::web_auth_middleware,
        ));

    let web_auth_routes = Router::new()
        .route("/login", post(web_auth::login))
        .route("/refresh", post(web_auth::refresh))
        .route("/logout", post(web_auth::logout))
        .route("/status", get(web_auth::status))
        .route("/config", get(web_auth::get_config))
        .route("/config", put(web_auth::update_config))
        .route("/password/set", post(web_auth::set_password))
        .route("/totp/setup", post(web_auth::totp_setup))
        .route("/totp/verify", post(web_auth::totp_verify))
        .route("/totp/disable", post(web_auth::totp_disable))
        .route("/recovery-codes/regenerate", post(web_auth::regenerate_recovery_codes))
        .with_state(web_auth_state.clone());

    let frontend_dir = detect_frontend_dir();
    let index_html_path = frontend_dir.join("index.html");
    let static_service =
        ServeDir::new(&frontend_dir).not_found_service(ServeFile::new(&index_html_path));

    app_state.load_initial_session().await?;
    info!("Application state initialized");

    let app = Router::new()
        .nest("/api/v1", api_routes)
        .nest("/api/v1/web-auth", web_auth_routes)
        .route("/health", get(health_check))
        .fallback_service(static_service)
        .layer(middleware);

    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await?;
    let actual_port = listener.local_addr()?.port();
    
    let _ = port_tx.send(actual_port);
    
    info!("Embedded server started on port {}", actual_port);
    info!("API base: http://127.0.0.1:{}/api/v1", actual_port);
    info!("WebSocket: ws://127.0.0.1:{}/api/v1/ws", actual_port);

    let server = axum::serve(listener, app);

    tokio::select! {
        result = server => {
            if let Err(e) = result {
                tracing::error!("Server error: {}", e);
            }
        }
        _ = tokio::signal::ctrl_c() => {
            info!("Received shutdown signal...");
        }
    }

    info!("Shutting down web auth cleanup tasks...");
    web_auth_state.stop_cleanup_tasks().await;
    info!("Shutting down persistence manager...");
    app_state.shutdown().await;
    info!("Application exited safely");

    Ok(())
}
