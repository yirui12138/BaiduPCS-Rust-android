// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use uuid::Uuid;

/// 下载任务状态
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum TaskStatus {
    /// 等待中
    Pending,
    /// 下载中
    Downloading,
    /// 解密中（新增）
    Decrypting,
    /// 已暂停
    Paused,
    /// 已完成
    Completed,
    /// 失败
    Failed,
}

/// 下载任务
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DownloadTask {
    /// 任务ID
    pub id: String,
    /// 文件服务器ID
    pub fs_id: u64,
    /// 网盘路径
    pub remote_path: String,
    /// 本地保存路径
    pub local_path: PathBuf,
    /// 文件大小
    pub total_size: u64,
    /// 已下载大小
    pub downloaded_size: u64,
    /// 任务状态
    pub status: TaskStatus,
    /// 下载速度 (bytes/s)
    pub speed: u64,
    /// 创建时间 (Unix timestamp)
    pub created_at: i64,
    /// 开始时间 (Unix timestamp)
    pub started_at: Option<i64>,
    /// 完成时间 (Unix timestamp)
    pub completed_at: Option<i64>,
    /// 错误信息
    pub error: Option<String>,

    // === 文件夹下载相关字段 ===
    /// 文件夹下载组ID，单文件下载时为 None
    #[serde(skip_serializing_if = "Option::is_none")]
    pub group_id: Option<String>,
    /// 文件夹根路径，如 "/电影"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub group_root: Option<String>,
    /// 相对于根文件夹的路径，如 "科幻片/星际穿越.mp4"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub relative_path: Option<String>,

    // === 🔥 新增：跨任务跳转相关字段 ===
    /// 关联的转存任务 ID（如果此下载任务由转存任务自动创建）
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub transfer_task_id: Option<String>,

    // === 🔥 新增：任务位借调机制相关字段 ===
    /// 占用的槽位ID
    #[serde(skip)]
    pub slot_id: Option<usize>,

    /// 是否使用借调位（而非固定位）
    #[serde(skip)]
    pub is_borrowed_slot: bool,

    // === 🔥 新增：自动备份相关字段 ===
    /// 是否为自动备份任务
    #[serde(default)]
    pub is_backup: bool,

    /// 关联的备份配置ID（is_backup=true 时使用）
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub backup_config_id: Option<String>,

    /// 🔥 启动重试次数（用于限制准备/注册失败的重试）
    #[serde(skip)]
    pub start_retry_count: u32,

    // === 🔥 解密相关字段 ===
    /// 是否为加密文件（通过文件名或内容检测）
    #[serde(default)]
    pub is_encrypted: bool,

    /// 解密进度 (0.0 - 100.0)
    #[serde(default)]
    pub decrypt_progress: f64,

    /// 解密后的最终文件路径
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub decrypted_path: Option<PathBuf>,

    /// 原始文件名（解密后恢复的文件名）
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub original_filename: Option<String>,

    // === 🔥 分享直下相关字段 ===
    /// 是否为分享直下任务（完成后不自动清除，由转存管理器清理）
    #[serde(default)]
    pub is_share_direct_download: bool,
}

impl DownloadTask {
    pub fn new(fs_id: u64, remote_path: String, local_path: PathBuf, total_size: u64) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            fs_id,
            remote_path,
            local_path,
            total_size,
            downloaded_size: 0,
            status: TaskStatus::Pending,
            speed: 0,
            created_at: chrono::Utc::now().timestamp(),
            started_at: None,
            completed_at: None,
            error: None,
            // 文件夹下载字段默认为 None
            group_id: None,
            group_root: None,
            relative_path: None,
            // 转存任务关联字段默认为 None
            transfer_task_id: None,
            // 任务位借调机制字段初始化
            slot_id: None,
            is_borrowed_slot: false,
            // 自动备份字段初始化
            is_backup: false,
            backup_config_id: None,
            start_retry_count: 0,
            // 解密字段初始化
            is_encrypted: false,
            decrypt_progress: 0.0,
            decrypted_path: None,
            original_filename: None,
            // 分享直下字段初始化
            is_share_direct_download: false,
        }
    }

    /// 设置关联的转存任务 ID
    pub fn set_transfer_task_id(&mut self, transfer_task_id: String) {
        self.transfer_task_id = Some(transfer_task_id);
    }

    /// 创建带文件夹组信息的任务
    pub fn new_with_group(
        fs_id: u64,
        remote_path: String,
        local_path: PathBuf,
        total_size: u64,
        group_id: String,
        group_root: String,
        relative_path: String,
    ) -> Self {
        let mut task = Self::new(fs_id, remote_path, local_path, total_size);
        task.group_id = Some(group_id);
        task.group_root = Some(group_root);
        task.relative_path = Some(relative_path);
        task
    }

    /// 创建自动备份下载任务
    pub fn new_backup(
        fs_id: u64,
        remote_path: String,
        local_path: PathBuf,
        total_size: u64,
        backup_config_id: String,
    ) -> Self {
        let mut task = Self::new(fs_id, remote_path, local_path, total_size);
        task.is_backup = true;
        task.backup_config_id = Some(backup_config_id);
        task
    }

    /// 计算进度百分比
    pub fn progress(&self) -> f64 {
        if self.total_size == 0 {
            return 0.0;
        }
        (self.downloaded_size as f64 / self.total_size as f64) * 100.0
    }

    /// 估算剩余时间 (秒)
    pub fn eta(&self) -> Option<u64> {
        if self.speed == 0 || self.downloaded_size >= self.total_size {
            return None;
        }
        let remaining = self.total_size - self.downloaded_size;
        Some(remaining / self.speed)
    }

    /// 标记为下载中
    pub fn mark_downloading(&mut self) {
        self.status = TaskStatus::Downloading;
        if self.started_at.is_none() {
            self.started_at = Some(chrono::Utc::now().timestamp());
        }
    }

    /// 标记为解密中
    pub fn mark_decrypting(&mut self) {
        self.status = TaskStatus::Decrypting;
    }

    /// 更新解密进度
    pub fn update_decrypt_progress(&mut self, progress: f64) {
        self.decrypt_progress = progress.clamp(0.0, 100.0);
    }

    /// 标记解密完成
    pub fn mark_decrypt_completed(&mut self, decrypted_path: PathBuf, original_size: u64) {
        self.decrypted_path = Some(decrypted_path);
        self.total_size = original_size; // 恢复为原始大小
        self.downloaded_size = original_size;
        self.decrypt_progress = 100.0;
    }

    /// 检测文件名是否为加密文件
    pub fn detect_encrypted_filename(filename: &str) -> bool {
        crate::encryption::EncryptionService::is_encrypted_filename(filename)
    }

    /// 标记为已完成
    pub fn mark_completed(&mut self) {
        self.status = TaskStatus::Completed;
        self.completed_at = Some(chrono::Utc::now().timestamp());
        self.downloaded_size = self.total_size;
    }

    /// 标记为失败
    pub fn mark_failed(&mut self, error: String) {
        self.status = TaskStatus::Failed;
        self.error = Some(error);
    }

    /// 标记为暂停
    pub fn mark_paused(&mut self) {
        self.status = TaskStatus::Paused;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_task_creation() {
        let task = DownloadTask::new(
            12345,
            "/test/file.txt".to_string(),
            PathBuf::from("./downloads/file.txt"),
            1024 * 1024, // 1MB
        );

        assert_eq!(task.fs_id, 12345);
        assert_eq!(task.status, TaskStatus::Pending);
        assert_eq!(task.downloaded_size, 0);
        assert_eq!(task.progress(), 0.0);
    }

    #[test]
    fn test_progress_calculation() {
        let mut task = DownloadTask::new(1, "/test".to_string(), PathBuf::from("./test"), 1000);

        task.downloaded_size = 250;
        assert_eq!(task.progress(), 25.0);

        task.downloaded_size = 500;
        assert_eq!(task.progress(), 50.0);

        task.downloaded_size = 1000;
        assert_eq!(task.progress(), 100.0);
    }

    #[test]
    fn test_eta_calculation() {
        let mut task = DownloadTask::new(1, "/test".to_string(), PathBuf::from("./test"), 1000);

        task.downloaded_size = 200;
        task.speed = 100; // 100 bytes/s
        assert_eq!(task.eta(), Some(8)); // (1000 - 200) / 100 = 8s

        task.speed = 0;
        assert_eq!(task.eta(), None); // 速度为0，无法估算
    }

    #[test]
    fn test_status_transitions() {
        let mut task = DownloadTask::new(1, "/test".to_string(), PathBuf::from("./test"), 1000);

        task.mark_downloading();
        assert_eq!(task.status, TaskStatus::Downloading);
        assert!(task.started_at.is_some());

        task.mark_paused();
        assert_eq!(task.status, TaskStatus::Paused);

        task.mark_failed("Network error".to_string());
        assert_eq!(task.status, TaskStatus::Failed);
        assert_eq!(task.error, Some("Network error".to_string()));

        task.mark_completed();
        assert_eq!(task.status, TaskStatus::Completed);
        assert_eq!(task.downloaded_size, task.total_size);
        assert!(task.completed_at.is_some());
    }

    #[test]
    fn test_decrypting_status() {
        let mut task = DownloadTask::new(
            12345,
            "/test/BPR_BKUP_uuid.bkup".to_string(),
            PathBuf::from("./downloads/BPR_BKUP_uuid.bkup"),
            1100,
        );

        // 测试解密状态转换
        task.is_encrypted = true;
        task.mark_downloading();
        assert_eq!(task.status, TaskStatus::Downloading);

        task.mark_decrypting();
        assert_eq!(task.status, TaskStatus::Decrypting);

        // 测试解密进度更新
        task.update_decrypt_progress(75.0);
        assert_eq!(task.decrypt_progress, 75.0);

        // 测试进度边界
        task.update_decrypt_progress(150.0);
        assert_eq!(task.decrypt_progress, 100.0);

        task.update_decrypt_progress(-10.0);
        assert_eq!(task.decrypt_progress, 0.0);
    }

    #[test]
    fn test_decrypt_completed() {
        let mut task = DownloadTask::new(
            12345,
            "/test/BPR_BKUP_uuid.bkup".to_string(),
            PathBuf::from("./downloads/BPR_BKUP_uuid.bkup"),
            1100,
        );

        task.is_encrypted = true;
        task.mark_decrypting();
        task.mark_decrypt_completed(PathBuf::from("./downloads/original.txt"), 1024);

        assert_eq!(task.decrypt_progress, 100.0);
        assert_eq!(task.total_size, 1024);
        assert!(task.decrypted_path.is_some());
    }

    #[test]
    fn test_detect_encrypted_filename() {
        // 有效的加密文件名：UUID.dat
        assert!(DownloadTask::detect_encrypted_filename("a1b2c3d4-e5f6-7890-abcd-ef1234567890.dat"));
        // 无效的文件名
        assert!(!DownloadTask::detect_encrypted_filename("normal_file.txt"));
        assert!(!DownloadTask::detect_encrypted_filename("not-a-uuid.dat"));
    }

    /// 测试旧版本 JSON 数据反序列化兼容性
    /// 确保缺少新增解密字段的旧数据能正确反序列化
    #[test]
    fn test_backward_compatibility_deserialization() {
        // 模拟旧版本的 JSON 数据（不包含解密相关字段）
        let old_json = r#"{
            "id": "old-task-456",
            "fs_id": 12345,
            "remote_path": "/test/file.txt",
            "local_path": "./downloads/file.txt",
            "total_size": 1024,
            "downloaded_size": 512,
            "status": "downloading",
            "speed": 100,
            "created_at": 1703203200,
            "started_at": 1703203201,
            "completed_at": null,
            "error": null,
            "group_id": null,
            "group_root": null,
            "relative_path": null,
            "transfer_task_id": null,
            "is_backup": false,
            "backup_config_id": null
        }"#;

        // 反序列化应该成功，新字段使用默认值
        let task: DownloadTask = serde_json::from_str(old_json).expect("反序列化旧版本数据失败");

        // 验证基本字段
        assert_eq!(task.id, "old-task-456");
        assert_eq!(task.fs_id, 12345);
        assert_eq!(task.total_size, 1024);
        assert_eq!(task.status, TaskStatus::Downloading);

        // 验证新增解密字段使用默认值
        assert!(!task.is_encrypted); // 默认 false
        assert_eq!(task.decrypt_progress, 0.0); // 默认 0.0
        assert!(task.decrypted_path.is_none()); // 默认 None
        assert!(task.original_filename.is_none()); // 默认 None
    }

    /// 测试新版本 JSON 数据序列化/反序列化
    #[test]
    fn test_new_version_serialization() {
        let mut task = DownloadTask::new(
            12345,
            "/test/BPR_BKUP_uuid.bkup".to_string(),
            PathBuf::from("./downloads/BPR_BKUP_uuid.bkup"),
            1100,
        );
        task.is_encrypted = true;
        task.decrypt_progress = 75.0;
        task.decrypted_path = Some(PathBuf::from("./downloads/original.txt"));
        task.original_filename = Some("original.txt".to_string());

        // 序列化
        let json = serde_json::to_string(&task).expect("序列化失败");

        // 反序列化
        let restored: DownloadTask = serde_json::from_str(&json).expect("反序列化失败");

        // 验证解密字段正确恢复
        assert!(restored.is_encrypted);
        assert_eq!(restored.decrypt_progress, 75.0);
        assert_eq!(
            restored.decrypted_path,
            Some(PathBuf::from("./downloads/original.txt"))
        );
        assert_eq!(restored.original_filename, Some("original.txt".to_string()));
    }
}
