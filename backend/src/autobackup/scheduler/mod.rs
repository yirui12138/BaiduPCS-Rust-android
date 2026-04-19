// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

//! 调度器模块

pub mod backup_scheduler;
pub mod change_aggregator;
pub mod poll_scheduler;
pub mod task_controller;

pub use backup_scheduler::{BackupScheduler, FileTaskContext, SchedulerEvent, SchedulerStatus};
pub use change_aggregator::{
    ChangeAggregator, ChangeEvent, EventSender, BackpressureStrategy,
    bounded_event_channel, bounded_event_channel_with_strategy,
    DEFAULT_EVENT_CHANNEL_CAPACITY,
    GlobalPollType,
};
pub use poll_scheduler::{
    PollScheduler, PollScheduleConfig, ScheduledTime,
    GLOBAL_POLL_UPLOAD_INTERVAL,
    GLOBAL_POLL_UPLOAD_SCHEDULED,
    GLOBAL_POLL_DOWNLOAD_INTERVAL,
    GLOBAL_POLL_DOWNLOAD_SCHEDULED,
    is_global_poll_id,
};
pub use task_controller::{TaskController, TriggerSource, ControllerStatus, task_loop};
