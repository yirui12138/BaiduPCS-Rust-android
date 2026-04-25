// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.nativeui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal data class SettingOption(
    val value: String,
    val label: String,
    val description: String,
)

internal data class NetdiskDirectorySelection(
    val path: String,
    val fsId: Long,
)

internal data class PublicDirectorySelection(
    val path: String,
    val setAsDefault: Boolean,
)

@Composable
internal fun SettingPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    helper: String? = null,
    placeholder: String = "点击选择",
) {
    val colors = webReplicaColors()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = colors.text)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceStrong,
            border = BorderStroke(1.dp, colors.border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.folder)
                Text(
                    text = value.ifBlank { placeholder },
                    modifier = Modifier.weight(1f),
                    color = if (value.isBlank()) colors.textSecondary else colors.text,
                )
                Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
            }
        }
        helper?.let { Text(it, color = colors.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun SettingSliderField(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    helper: String? = null,
    recommended: Int? = null,
    warning: String? = null,
    showWarning: Boolean = false,
) {
    val colors = webReplicaColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = colors.text)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(valueRange.first, valueRange.last)) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
        )
        Text(
            "当前: $value" + (recommended?.let { " (推荐: $it)" } ?: ""),
            color = colors.textSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
        helper?.let { Text(it, color = colors.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
        if (showWarning && !warning.isNullOrBlank()) {
            Surface(
                color = colors.danger.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.30f)),
            ) {
                Text(
                    text = warning,
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun SettingStepperField(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    helper: String? = null,
) {
    val colors = webReplicaColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = colors.text)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WebRoundIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "减少",
                size = 34.dp,
                onClick = { onValueChange((value - 1).coerceAtLeast(valueRange.first)) },
            )
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceStrong,
                border = BorderStroke(1.dp, colors.border),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$value", color = colors.text)
                }
            }
            WebRoundIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "增加",
                size = 34.dp,
                onClick = { onValueChange((value + 1).coerceAtMost(valueRange.last)) },
            )
        }
        helper?.let { Text(it, color = colors.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun SettingOptionSelector(
    label: String,
    value: String,
    options: List<SettingOption>,
    onValueChange: (String) -> Unit,
    helper: String? = null,
) {
    val colors = webReplicaColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = colors.text)
        options.forEach { option ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onValueChange(option.value) },
                shape = RoundedCornerShape(12.dp),
                color = if (value == option.value) colors.accentSoft else colors.surfaceStrong,
                border = BorderStroke(1.dp, if (value == option.value) colors.accent.copy(alpha = 0.45f) else colors.border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(selected = value == option.value, onClick = { onValueChange(option.value) })
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(option.label, color = colors.text)
                        Text(option.description, color = colors.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        helper?.let { Text(it, color = colors.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun SettingSegmentedTabs(
    selected: String,
    options: List<Pair<String, String>>,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = webReplicaColors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WebReplicaDimens.ButtonRadius),
        color = colors.surfaceStrong,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (key, label) ->
                val active = selected == key
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(WebReplicaDimens.ButtonRadius))
                        .clickable { onSelectedChange(key) },
                    shape = RoundedCornerShape(WebReplicaDimens.ButtonRadius),
                    color = if (active) colors.accentSoft else colors.surfaceStrong,
                    border = BorderStroke(1.dp, if (active) colors.accent.copy(alpha = 0.50f) else colors.surfaceStrong),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if (active) colors.accent else colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NetdiskDirectoryPickerDialog(
    api: NativeApiClient,
    initialPath: String,
    title: String = "选择保存位置",
    onDismiss: () -> Unit,
    onConfirm: (NetdiskDirectorySelection) -> Unit,
) {
    val colors = webReplicaColors()
    val scope = rememberCoroutineScope()
    var currentPath by rememberSaveable { mutableStateOf(normalizeNetdiskPath(initialPath)) }
    var currentFsId by rememberSaveable { mutableStateOf(0L) }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var newFolderName by rememberSaveable { mutableStateOf("") }
    var creatingFolder by remember { mutableStateOf(false) }

    fun refresh(path: String = currentPath) {
        scope.launch {
            loading = true
            errorMessage = null
            runCatching { api.files(path).list.filter { it.isDirectory }.sortedBy { it.displayName.lowercase() } }
                .onSuccess { folders = it }
                .onFailure {
                    folders = emptyList()
                    errorMessage = it.message ?: "加载目录失败"
                }
            loading = false
        }
    }

    LaunchedEffect(currentPath) {
        refresh(currentPath)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(title, color = colors.text, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    WebRoundIconButton(Icons.Rounded.Close, "关闭", size = 32.dp, onClick = onDismiss)
                }
                NetdiskBreadcrumb(
                    currentPath = currentPath,
                    onNavigateRoot = {
                        currentPath = "/"
                        currentFsId = 0L
                    },
                    onNavigateTo = {
                        currentPath = it
                        currentFsId = 0L
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .heightIn(min = 320.dp, max = 420.dp)
                        .background(colors.surfaceStrong, RoundedCornerShape(16.dp)),
                ) {
                    when {
                        loading -> Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        errorMessage != null -> Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text(errorMessage ?: "", color = colors.danger)
                        }
                        folders.isEmpty() -> Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text("当前目录为空", color = colors.textSecondary)
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                        ) {
                            items(folders, key = { it.fsId }) { folder ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            currentPath = normalizeNetdiskPath(folder.path)
                                            currentFsId = folder.fsId
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceMuted,
                                    border = BorderStroke(1.dp, colors.border),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.folder)
                                        Text(folder.displayName, modifier = Modifier.weight(1f), color = colors.text)
                                        Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WebTextInput(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = "新建文件夹",
                        placeholder = "在当前目录创建子文件夹",
                    )
                    OutlinedButton(
                        onClick = {
                            val folderName = newFolderName.trim()
                            if (folderName.isBlank()) {
                                errorMessage = "请输入文件夹名称"
                                return@OutlinedButton
                            }
                            creatingFolder = true
                            scope.launch {
                                val targetPath = if (currentPath == "/") "/$folderName" else "${currentPath.trimEnd('/')}/$folderName"
                                runCatching { api.createFolder(targetPath) }
                                    .onSuccess {
                                        newFolderName = ""
                                        currentPath = normalizeNetdiskPath(targetPath)
                                        errorMessage = null
                                    }
                                    .onFailure { errorMessage = it.message ?: "创建文件夹失败" }
                                creatingFolder = false
                            }
                        },
                        enabled = !creatingFolder,
                    ) {
                        Text(if (creatingFolder) "创建中..." else "新建文件夹")
                    }
                }
                HorizontalDivider(color = colors.border)
                Text("保存到: $currentPath", color = colors.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = { onConfirm(NetdiskDirectorySelection(currentPath, currentFsId)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
internal fun AndroidPublicDownloadDirPickerDialog(
    rootDirPath: String,
    initialPath: String,
    showSetDefaultToggle: Boolean = true,
    title: String = "选择下载目录",
    onDismiss: () -> Unit,
    onConfirm: (PublicDirectorySelection) -> Unit,
) {
    val colors = webReplicaColors()
    val rootDir = remember(rootDirPath) { File(rootDirPath).absoluteFile.apply { mkdirs() } }
    val scope = rememberCoroutineScope()
    var currentPath by rememberSaveable { mutableStateOf(coerceLocalPath(initialPath, rootDir).absolutePath) }
    var newFolderName by rememberSaveable { mutableStateOf("") }
    var creatingFolder by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var setAsDefault by rememberSaveable { mutableStateOf(false) }
    val currentDir = remember(currentPath, rootDirPath) { coerceLocalPath(currentPath, rootDir) }
    val children = remember(currentPath, rootDirPath, creatingFolder) {
        currentDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(title, color = colors.text, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    WebRoundIconButton(Icons.Rounded.Close, "关闭", size = 32.dp, onClick = onDismiss)
                }
                LocalDirectoryBreadcrumb(
                    rootDir = rootDir,
                    currentDir = currentDir,
                    onNavigateRoot = { currentPath = rootDir.absolutePath },
                    onNavigateTo = { currentPath = it.absolutePath },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 220.dp, max = 300.dp)
                        .background(colors.surfaceStrong, RoundedCornerShape(16.dp)),
                ) {
                    if (children.isEmpty()) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text("当前目录为空", color = colors.textSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                        ) {
                            items(children, key = { it.absolutePath }) { folder ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { currentPath = folder.absolutePath },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceMuted,
                                    border = BorderStroke(1.dp, colors.border),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = colors.folder)
                                        Text(folder.name, modifier = Modifier.weight(1f), color = colors.text)
                                        Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                errorMessage?.let {
                    Text(it, color = colors.danger, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                WebTextInput(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = "新建子目录",
                    placeholder = "在当前目录创建子目录",
                )
                OutlinedButton(
                    onClick = {
                        val folderName = newFolderName.trim()
                        if (folderName.isBlank()) {
                            errorMessage = "请输入目录名称"
                            return@OutlinedButton
                        }
                        val target = File(currentDir, folderName)
                        creatingFolder = true
                        scope.launch {
                            val created = runCatching {
                                if (target.exists()) target.isDirectory else target.mkdirs()
                            }.getOrDefault(false)
                            if (created) {
                                newFolderName = ""
                                currentPath = target.absolutePath
                                errorMessage = null
                            } else {
                                errorMessage = "创建目录失败"
                            }
                            creatingFolder = false
                        }
                    },
                    enabled = !creatingFolder,
                ) {
                    Text(if (creatingFolder) "创建中..." else "新建目录")
                }
                if (showSetDefaultToggle) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = setAsDefault, onCheckedChange = { setAsDefault = it })
                        Text("同时设为默认下载目录", color = colors.text)
                    }
                }
                HorizontalDivider(color = colors.border)
                Text("保存到: ${currentDir.absolutePath}", color = colors.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = { onConfirm(PublicDirectorySelection(currentDir.absolutePath, setAsDefault)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun NetdiskBreadcrumb(
    currentPath: String,
    onNavigateRoot: () -> Unit,
    onNavigateTo: (String) -> Unit,
) {
    val colors = webReplicaColors()
    val segments = currentPath.trim('/').takeIf { it.isNotBlank() }?.split('/') ?: emptyList()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceStrong,
        border = BorderStroke(1.dp, colors.border),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 88.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onNavigateRoot)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Text("/", color = colors.text)
                }
            }
            items(segments.indices.toList(), key = { it }) { index ->
                val path = "/" + segments.take(index + 1).joinToString("/")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateTo(path) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Spacer(Modifier.size(16.dp))
                    Text(segments[index], color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun LocalDirectoryBreadcrumb(
    rootDir: File,
    currentDir: File,
    onNavigateRoot: () -> Unit,
    onNavigateTo: (File) -> Unit,
) {
    val colors = webReplicaColors()
    val relativeParts = currentDir.absolutePath
        .removePrefix(rootDir.absolutePath)
        .trim(File.separatorChar)
        .takeIf { it.isNotBlank() }
        ?.split(File.separatorChar)
        ?: emptyList()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceStrong,
        border = BorderStroke(1.dp, colors.border),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 88.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onNavigateRoot)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Text(rootDir.name.ifBlank { "Download/BaiduPCS" }, color = colors.text)
                }
            }
            items(relativeParts.indices.toList(), key = { it }) { index ->
                val next = relativeParts.take(index + 1).fold(rootDir) { acc, part -> File(acc, part) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateTo(next) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Spacer(Modifier.size(16.dp))
                    Text(relativeParts[index], color = colors.textSecondary)
                }
            }
        }
    }
}

private fun normalizeNetdiskPath(path: String): String {
    if (path.isBlank()) return "/"
    val trimmed = path.trim().replace("//", "/").trim('/')
    return if (trimmed.isBlank()) "/" else "/$trimmed"
}

private fun coerceLocalPath(path: String, rootDir: File): File =
    runCatching {
        val normalizedRoot = rootDir.absoluteFile
        val candidate = File(path).absoluteFile
        if (candidate.absolutePath.startsWith(normalizedRoot.absolutePath, ignoreCase = true) &&
            candidate.exists() &&
            candidate.isDirectory
        ) {
            candidate
        } else {
            normalizedRoot
        }
    }.getOrDefault(rootDir.absoluteFile)
