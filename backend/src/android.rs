// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

use crate::config::AppConfig;
use crate::runtime;
use crate::web_auth::AuthMode;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring};
use jni::JNIEnv;
use std::fs;
use std::path::{Path, PathBuf};
use std::sync::{Mutex, OnceLock};
use std::thread::JoinHandle;

struct AndroidRuntimeHandle {
    shutdown_tx: tokio::sync::oneshot::Sender<()>,
    thread: JoinHandle<()>,
}

static ANDROID_RUNTIME: OnceLock<Mutex<Option<AndroidRuntimeHandle>>> = OnceLock::new();
static LAST_ERROR: OnceLock<Mutex<String>> = OnceLock::new();

fn runtime_slot() -> &'static Mutex<Option<AndroidRuntimeHandle>> {
    ANDROID_RUNTIME.get_or_init(|| Mutex::new(None))
}

fn last_error_slot() -> &'static Mutex<String> {
    LAST_ERROR.get_or_init(|| Mutex::new(String::new()))
}

fn set_last_error(message: impl Into<String>) {
    if let Ok(mut guard) = last_error_slot().lock() {
        *guard = message.into();
    }
}

fn clear_last_error() {
    set_last_error("");
}

fn normalize_existing_runtime() {
    let Ok(mut guard) = runtime_slot().lock() else {
        return;
    };

    let should_cleanup = guard
        .as_ref()
        .map(|handle| handle.thread.is_finished())
        .unwrap_or(false);

    if should_cleanup {
        if let Some(handle) = guard.take() {
            let _ = handle.thread.join();
        }
    }
}

fn ensure_dir(path: &Path) -> anyhow::Result<()> {
    fs::create_dir_all(path)?;
    Ok(())
}

async fn prepare_android_config(
    home_dir: &Path,
    download_dir: &Path,
    upload_dir: &Path,
    port: u16,
) -> anyhow::Result<()> {
    let config_dir = home_dir.join("config");
    let temp_dir = config_dir.join("temp");
    let config_path = config_dir.join("app.toml");

    ensure_dir(&config_dir)?;
    ensure_dir(&temp_dir)?;

    let config_path_string = config_path.to_string_lossy().to_string();
    let mut config = AppConfig::load_or_default(&config_path_string).await;

    config.server.host = "127.0.0.1".to_string();
    config.server.port = port;
    config.server.cors_origins = vec!["*".to_string()];

    config.download.download_dir = download_dir.to_path_buf();
    config.download.default_directory = Some(download_dir.to_path_buf());
    config.download.recent_directory = Some(download_dir.to_path_buf());
    config.download.ask_each_time = false;

    config.filesystem.allowed_paths = vec![
        upload_dir.to_string_lossy().to_string(),
        download_dir.to_string_lossy().to_string(),
    ];
    config.filesystem.default_path = Some(upload_dir.to_string_lossy().to_string());
    config.filesystem.show_hidden = false;
    config.filesystem.follow_symlinks = false;
    config.filesystem.enforce_allowlist_on_followed_symlinks = true;

    config.log.log_dir = home_dir.join("logs");
    config.persistence.wal_dir = home_dir.join("wal").to_string_lossy().to_string();
    config.persistence.db_path = config_dir.join("baidu-pcs.db").to_string_lossy().to_string();
    config.autobackup.temp_dir = temp_dir.to_string_lossy().to_string();
    config.autobackup.config_path = config_dir
        .join("autobackup_configs.json")
        .to_string_lossy()
        .to_string();

    config.web_auth.enabled = false;
    config.web_auth.mode = AuthMode::None;

    let _ = config.save_to_file(&config_path_string).await?;
    Ok(())
}

fn start_server(
    home_dir: PathBuf,
    download_dir: PathBuf,
    upload_dir: PathBuf,
    port: u16,
) -> anyhow::Result<()> {
    normalize_existing_runtime();

    let mut guard = runtime_slot()
        .lock()
        .map_err(|_| anyhow::anyhow!("failed to acquire runtime lock"))?;

    if guard.is_some() {
        clear_last_error();
        return Ok(());
    }

    ensure_dir(&home_dir)?;
    ensure_dir(&home_dir.join("logs"))?;
    ensure_dir(&home_dir.join("wal"))?;
    ensure_dir(&download_dir)?;
    ensure_dir(&upload_dir)?;

    let (shutdown_tx, shutdown_rx) = tokio::sync::oneshot::channel::<()>();
    let thread_home = home_dir.clone();
    let thread_download = download_dir.clone();
    let thread_upload = upload_dir.clone();

    let thread = std::thread::Builder::new()
        .name("baidupcs-android".to_string())
        .spawn(move || {
            let runtime = match tokio::runtime::Builder::new_multi_thread()
                .enable_all()
                .build()
            {
                Ok(runtime) => runtime,
                Err(error) => {
                    set_last_error(format!("failed to create runtime: {error}"));
                    return;
                }
            };

            let result = runtime.block_on(async move {
                std::env::set_current_dir(&thread_home)?;
                prepare_android_config(&thread_home, &thread_download, &thread_upload, port)
                    .await?;
                runtime::run_until_shutdown(async {
                    let _ = shutdown_rx.await;
                })
                .await
            });

            if let Err(error) = result {
                set_last_error(error.to_string());
            }
        })?;

    *guard = Some(AndroidRuntimeHandle { shutdown_tx, thread });
    clear_last_error();
    Ok(())
}

fn stop_server() -> anyhow::Result<()> {
    let mut guard = runtime_slot()
        .lock()
        .map_err(|_| anyhow::anyhow!("failed to acquire runtime lock"))?;

    if let Some(handle) = guard.take() {
        let _ = handle.shutdown_tx.send(());
        let _ = handle.thread.join();
    }

    clear_last_error();
    Ok(())
}

fn jstring_to_string(env: &mut JNIEnv, value: JString) -> anyhow::Result<String> {
    Ok(env.get_string(&value)?.into())
}

fn bool_to_jboolean(value: bool) -> jboolean {
    if value { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_com_baidupcs_android_core_NativeBridge_startServer(
    mut env: JNIEnv,
    _class: JClass,
    home_dir: JString,
    download_dir: JString,
    upload_dir: JString,
    port: jint,
) -> jboolean {
    let result = (|| -> anyhow::Result<()> {
        let home_dir = PathBuf::from(jstring_to_string(&mut env, home_dir)?);
        let download_dir = PathBuf::from(jstring_to_string(&mut env, download_dir)?);
        let upload_dir = PathBuf::from(jstring_to_string(&mut env, upload_dir)?);
        let port: u16 = port.try_into().map_err(|_| anyhow::anyhow!("invalid port"))?;
        start_server(home_dir, download_dir, upload_dir, port)
    })();

    match result {
        Ok(()) => bool_to_jboolean(true),
        Err(error) => {
            set_last_error(error.to_string());
            bool_to_jboolean(false)
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_baidupcs_android_core_NativeBridge_stopServer(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    match stop_server() {
        Ok(()) => bool_to_jboolean(true),
        Err(error) => {
            set_last_error(error.to_string());
            bool_to_jboolean(false)
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_baidupcs_android_core_NativeBridge_getLastError(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let message = last_error_slot()
        .lock()
        .map(|guard| guard.clone())
        .unwrap_or_else(|_| "unknown native error".to_string());

    match env.new_string(message) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
