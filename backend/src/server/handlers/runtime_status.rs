// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

use axum::{extract::State, http::StatusCode, Json};
use serde::Serialize;

use crate::{
    downloader::{FolderStatus, TaskStatus},
    server::handlers::auth::ApiResponse,
    AppState,
};

#[derive(Debug, Serialize)]
pub struct RuntimeSummaryResponse {
    pub active_downloads: usize,
    pub active_uploads: usize,
    pub active_transfers: usize,
    pub active_backups: usize,
    pub has_active_work: bool,
}

/// GET /api/v1/runtime/summary
/// 返回当前运行时的轻量活跃任务摘要，供移动端后台保活与省电策略使用。
pub async fn get_runtime_summary(
    State(app_state): State<AppState>,
) -> Result<Json<ApiResponse<RuntimeSummaryResponse>>, StatusCode> {
    let active_downloads = {
        let download_manager = app_state.download_manager.read().await.clone();
        let mut total = 0usize;

        if let Some(manager) = download_manager {
            total += manager
                .get_all_tasks()
                .await
                .into_iter()
                .filter(|task| {
                    matches!(
                        task.status,
                        TaskStatus::Pending | TaskStatus::Downloading | TaskStatus::Decrypting
                    )
                })
                .count();
        }

        total += app_state
            .folder_download_manager
            .get_all_folders()
            .await
            .into_iter()
            .filter(|folder| {
                matches!(folder.status, FolderStatus::Scanning | FolderStatus::Downloading)
            })
            .count();

        total
    };

    let active_uploads = {
        let upload_manager = app_state.upload_manager.read().await.clone();
        match upload_manager {
            Some(manager) => manager.runtime_active_task_count().await,
            None => 0,
        }
    };

    let active_transfers = {
        let transfer_manager = app_state.transfer_manager.read().await.clone();
        match transfer_manager {
            Some(manager) => manager.active_task_count().await,
            None => 0,
        }
    };

    let active_backups = {
        let autobackup_manager = app_state.autobackup_manager.read().await.clone();
        match autobackup_manager {
            Some(manager) => manager.get_status_nonblocking().active_task_count,
            None => 0,
        }
    };

    let response = RuntimeSummaryResponse {
        active_downloads,
        active_uploads,
        active_transfers,
        active_backups,
        has_active_work: active_downloads + active_uploads + active_transfers + active_backups > 0,
    };

    Ok(Json(ApiResponse::success(response)))
}
