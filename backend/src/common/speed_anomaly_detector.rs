// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

//! 速度异常检测器
//!
//! 基于基线速度检测全局速度异常下降
//!
//! 核心机制：
//! 1. 在任务启动后建立基线速度
//! 2. 持续监控当前速度与基线的对比
//! 3. 当速度下降超过阈值且持续一定时间时，触发链接刷新

use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Instant;
use tracing::{debug, info};

/// 速度异常检测配置
#[derive(Clone, Debug)]
pub struct SpeedAnomalyConfig {
    /// 基线建立时间（秒）- 任务开始后多久建立基线
    pub baseline_establish_secs: u64,
    /// 速度下降阈值（比例，如 0.5 表示下降50%）
    pub speed_drop_threshold: f64,
    /// 持续时长阈值（秒）- 速度下降持续多久触发刷新
    pub duration_threshold_secs: u64,
    /// 检查间隔（秒）
    pub check_interval_secs: u64,
    /// 最小基线速度（字节/秒）- 避免基线太低导致误判
    pub min_baseline_speed: u64,
}

impl Default for SpeedAnomalyConfig {
    fn default() -> Self {
        Self {
            baseline_establish_secs: 30,
            speed_drop_threshold: 0.5,   // 下降50%
            duration_threshold_secs: 10, // 持续10秒
            check_interval_secs: 5,
            min_baseline_speed: 100 * 1024, // 至少100KB/s
        }
    }
}

/// 速度异常检测器
///
/// 使用原子操作实现线程安全的状态管理
#[derive(Debug)]
pub struct SpeedAnomalyDetector {
    /// 基线速度（字节/秒）
    baseline_speed: AtomicU64,
    /// 基线是否已建立（0=未建立, 1=已建立）
    baseline_established: AtomicU64,
    /// 速度下降累计持续时间（秒）
    slow_duration_secs: AtomicU64,
    /// 任务开始时间
    task_start: Instant,
    /// 配置
    config: SpeedAnomalyConfig,
}

impl SpeedAnomalyDetector {
    /// 创建新的速度异常检测器
    pub fn new(config: SpeedAnomalyConfig) -> Self {
        Self {
            baseline_speed: AtomicU64::new(0),
            baseline_established: AtomicU64::new(0),
            slow_duration_secs: AtomicU64::new(0),
            task_start: Instant::now(),
            config,
        }
    }

    /// 检查速度异常
    ///
    /// # 参数
    /// * `current_speed` - 当前全局速度（字节/秒），从 ChunkScheduler.get_global_speed() 获取
    ///
    /// # 返回
    /// - `true`: 检测到异常，需要刷新链接
    /// - `false`: 速度正常
    pub fn check(&self, current_speed: u64) -> bool {
        let elapsed = self.task_start.elapsed();

        // 1. 检查是否到了建立基线的时间
        if self.baseline_established.load(Ordering::SeqCst) == 0 {
            if elapsed.as_secs() >= self.config.baseline_establish_secs {
                // 建立基线
                let baseline = current_speed.max(self.config.min_baseline_speed);
                self.baseline_speed.store(baseline, Ordering::SeqCst);
                self.baseline_established.store(1, Ordering::SeqCst);
                info!("📊 基线速度已建立: {:.2} KB/s", baseline as f64 / 1024.0);
            }
            return false; // 基线建立前不检测
        }

        let baseline = self.baseline_speed.load(Ordering::SeqCst);
        if baseline == 0 {
            return false;
        }

        // 2. 计算速度下降比例
        let drop_ratio = if current_speed < baseline {
            1.0 - (current_speed as f64 / baseline as f64)
        } else {
            0.0
        };

        // 3. 判断是否超过下降阈值
        if drop_ratio >= self.config.speed_drop_threshold {
            // 累计持续时间
            let prev_duration = self
                .slow_duration_secs
                .fetch_add(self.config.check_interval_secs, Ordering::SeqCst);
            let new_duration = prev_duration + self.config.check_interval_secs;

            debug!(
                "速度下降: 当前 {:.2} KB/s, 基线 {:.2} KB/s, 下降 {:.1}%, 持续 {}秒",
                current_speed as f64 / 1024.0,
                baseline as f64 / 1024.0,
                drop_ratio * 100.0,
                new_duration
            );

            if new_duration >= self.config.duration_threshold_secs {
                // 触发刷新
                info!(
                    "⚠️ 速度异常下降: 当前 {:.2} KB/s, 基线 {:.2} KB/s, 下降 {:.1}%, 持续 {}秒",
                    current_speed as f64 / 1024.0,
                    baseline as f64 / 1024.0,
                    drop_ratio * 100.0,
                    new_duration
                );

                // 重置持续时间
                self.slow_duration_secs.store(0, Ordering::SeqCst);

                // 更新基线（使用当前速度，但不低于最小值）
                let new_baseline = current_speed.max(self.config.min_baseline_speed);
                self.baseline_speed.store(new_baseline, Ordering::SeqCst);

                return true;
            }
        } else {
            // 速度正常，重置持续时间
            self.slow_duration_secs.store(0, Ordering::SeqCst);

            // 如果速度超过基线，更新基线
            if current_speed > baseline {
                self.baseline_speed.store(current_speed, Ordering::SeqCst);
                debug!("基线速度更新: {:.2} KB/s", current_speed as f64 / 1024.0);
            }
        }

        false
    }

    /// 重置检测器（任务重新开始时调用）
    pub fn reset(&mut self) {
        self.baseline_speed.store(0, Ordering::SeqCst);
        self.baseline_established.store(0, Ordering::SeqCst);
        self.slow_duration_secs.store(0, Ordering::SeqCst);
        self.task_start = Instant::now();
    }

    /// 获取当前基线速度（字节/秒）
    pub fn baseline_speed(&self) -> u64 {
        self.baseline_speed.load(Ordering::SeqCst)
    }

    /// 检查基线是否已建立
    pub fn is_baseline_established(&self) -> bool {
        self.baseline_established.load(Ordering::SeqCst) == 1
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_baseline_establishment() {
        let config = SpeedAnomalyConfig {
            baseline_establish_secs: 0, // 立即建立基线
            ..Default::default()
        };
        let detector = SpeedAnomalyDetector::new(config);

        // 第一次检查应建立基线
        assert!(!detector.check(1_000_000)); // 1 MB/s
        assert!(detector.is_baseline_established());
        assert_eq!(detector.baseline_speed(), 1_000_000);
    }

    #[test]
    fn test_speed_normal() {
        let config = SpeedAnomalyConfig {
            baseline_establish_secs: 0,
            speed_drop_threshold: 0.5,
            ..Default::default()
        };
        let detector = SpeedAnomalyDetector::new(config);

        // 建立基线
        detector.check(1_000_000);

        // 速度略微下降（40%），不应触发
        assert!(!detector.check(600_000));
    }

    #[test]
    fn test_speed_drop_single() {
        let config = SpeedAnomalyConfig {
            baseline_establish_secs: 0,
            speed_drop_threshold: 0.5,
            duration_threshold_secs: 10,
            check_interval_secs: 5,
            ..Default::default()
        };
        let detector = SpeedAnomalyDetector::new(config);

        // 建立基线
        detector.check(1_000_000);

        // 速度大幅下降（60%），但持续时间不够
        assert!(!detector.check(400_000));

        // 再次检查（累计 10 秒），应该触发
        assert!(detector.check(400_000));
    }

    #[test]
    fn test_baseline_update() {
        let config = SpeedAnomalyConfig {
            baseline_establish_secs: 0,
            ..Default::default()
        };
        let detector = SpeedAnomalyDetector::new(config);

        // 建立基线
        detector.check(1_000_000);

        // 速度提升，基线应更新
        detector.check(2_000_000);
        assert_eq!(detector.baseline_speed(), 2_000_000);
    }
}
