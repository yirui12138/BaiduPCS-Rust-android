// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.nativeui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Web replica files and settings", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun NativeReplicaPreviewGallery() {
    WebReplicaBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                WebToolbarRow {
                    Text("/", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    WebPillButton("识别到分享，请点击转存", Icons.Rounded.Share, detected = true) {}
                    WebRoundIconButton(Icons.Rounded.Refresh, "刷新", primary = true) {}
                }
            }
            item {
                WebSurfaceCard(color = Color(0xFFFFFBF0)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = webReplicaColors().folder)
                        Column(Modifier.weight(1f)) {
                            Text("Adobe CC2022", fontWeight = FontWeight.Medium)
                            Text("文件夹 · 2022/7/11 3:44:9", color = webReplicaColors().textSecondary)
                        }
                        WebRoundIconButton(Icons.Rounded.Share, "分享", size = 32.dp) {}
                        WebRoundIconButton(Icons.Rounded.Download, "下载", primary = true, size = 32.dp) {}
                    }
                }
            }
            item {
                WebSettingSection(
                    title = "下载配置",
                    description = "目录、线程与重试策略",
                    color = Color(0xFF67C23A),
                    expanded = true,
                    onToggle = {},
                    icon = { Icon(Icons.Rounded.Download, contentDescription = null, tint = Color(0xFF67C23A)) },
                ) {
                    WebTextInput("/storage/emulated/0/Download/BaiduPCS", {}, "下载目录")
                    WebSwitchRow("下载时选择目录", false, {})
                    Button(onClick = {}) { Text("保存设置") }
                }
            }
            item {
                WebSettingSection(
                    title = "上传配置",
                    description = "上传并发、重试与过滤",
                    color = Color(0xFFE6A23C),
                    expanded = false,
                    onToggle = {},
                    icon = { Icon(Icons.Rounded.Upload, contentDescription = null, tint = Color(0xFFE6A23C)) },
                ) {}
            }
        }
    }
}
