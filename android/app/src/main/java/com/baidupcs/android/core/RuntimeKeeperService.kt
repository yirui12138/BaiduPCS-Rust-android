// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.baidupcs.android.MainActivity
import com.baidupcs.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RuntimeKeeperService : Service() {
    companion object {
        private const val ACTION_START = "com.baidupcs.android.action.START_RUNTIME_KEEPER"
        private const val ACTION_STOP = "com.baidupcs.android.action.STOP_RUNTIME_KEEPER"
        private const val CHANNEL_ID = "runtime_keeper"
        private const val NOTIFICATION_ID = 18_888
        private const val POLL_INTERVAL_MS = 20_000L
        private const val RETRY_INTERVAL_MS = 10_000L

        fun start(context: Context) {
            val intent = Intent(context, RuntimeKeeperService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RuntimeKeeperService::class.java).setAction(ACTION_STOP))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bootstrapper by lazy { ServerBootstrapper(applicationContext) }
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        if (monitorJob?.isActive == true) {
            return START_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(summary = null, preparing = true))
        monitorJob = serviceScope.launch {
            monitorLoop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun monitorLoop() {
        val environment = bootstrapper.ensureStarted(forceRestart = false) { }.getOrElse {
            stopMonitoring()
            return
        }

        while (serviceScope.isActive) {
            val summary = RuntimeSummaryClient.fetch(environment.baseUrl)
            if (summary == null) {
                updateNotification(summary = null, preparing = true)
                delay(RETRY_INTERVAL_MS)
                continue
            }

            if (!summary.hasActiveWork) {
                stopMonitoring()
                return
            }

            updateNotification(summary = summary, preparing = false)
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(summary: RuntimeSummary?, preparing: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(summary, preparing))
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "柏渡云盘后台任务",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "仅在后台仍有下载、上传、转存或备份任务时保持应用存活。"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        summary: RuntimeSummary?,
        preparing: Boolean,
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_startup_mark)
        .setContentTitle(
            if (preparing) {
                "柏渡云盘正在保持任务存活"
            } else {
                "柏渡云盘后台任务进行中"
            },
        )
        .setContentText(
            when {
                summary == null -> "正在检查后台任务状态"
                else -> "下载 ${summary.activeDownloads} · 上传 ${summary.activeUploads} · 转存 ${summary.activeTransfers} · 备份 ${summary.activeBackups}"
            },
        )
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                when {
                    summary == null -> "正在检查后台任务状态，确认仍需后台保活后会继续维持本地服务。"
                    else -> "下载 ${summary.activeDownloads} · 上传 ${summary.activeUploads} · 转存 ${summary.activeTransfers} · 备份 ${summary.activeBackups}。轻触可返回应用查看详情。"
                },
            ),
        )
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .build()
}
