// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.nativeui

import android.content.Context
import android.graphics.BitmapFactory
import android.content.ClipData
import android.content.ClipboardManager
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.baidupcs.android.core.ImportedEntry
import com.baidupcs.android.core.ServerEnvironment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private enum class NativeScreen(val label: String, val title: String) {
    Files("文件", "文件管理"),
    Downloads("下载", "下载管理"),
    Uploads("上传", "上传管理"),
    ShareTransfer("分享", "分享与转存"),
    Settings("设置", "设置"),
    CloudDl("离线", "离线下载"),
    Credits("许可", "开源许可与鸣谢"),
}

private val nativeJson = Json { ignoreUnknownKeys = true }

@Composable
fun NativeApp(
    environment: ServerEnvironment,
    platform: NativePlatformActions,
) {
    val api = remember(environment.baseUrl) { NativeApiClient(environment.baseUrl) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var user by remember { mutableStateOf<UserAuth?>(null) }
    var checkingSession by remember { mutableStateOf(true) }
    var clipboardDetectionEnabled by rememberSaveable { mutableStateOf(true) }
    var vpnWarningEnabled by rememberSaveable { mutableStateOf(true) }
    var vpnDialogVisible by rememberSaveable { mutableStateOf(false) }
    var vpnDoNotShowAgain by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(api) {
        platform.cleanupStaleImports()
        runCatching {
            val mobile = api.config()["mobile"]?.jsonObject
            clipboardDetectionEnabled = mobile
                ?.get("clipboard_share_detection_enabled")
                ?.jsonPrimitive
                ?.booleanOrNull
                ?: true
            vpnWarningEnabled = mobile
                ?.get("vpn_warning_enabled")
                ?.jsonPrimitive
                ?.booleanOrNull
                ?: true
        }
        vpnDialogVisible = vpnWarningEnabled && platform.isVpnActive()
        user = runCatching { api.currentUser() }.getOrNull()
        checkingSession = false
    }

    if (vpnDialogVisible) {
        AlertDialog(
            onDismissRequest = { vpnDialogVisible = false },
            title = { Text("VPN 环境提示") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("我们无意冒犯您的互联网自由，但本软件在vpn环境下尚不稳定，您依然可以使用本软件，但关闭vpn可以提升稳定性")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = vpnDoNotShowAgain,
                            onCheckedChange = { vpnDoNotShowAgain = it },
                        )
                        Text("不再显示")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vpnDialogVisible = false
                    if (vpnDoNotShowAgain) {
                        vpnWarningEnabled = false
                        scope.launch {
                            runCatching { api.updateVpnWarning(false) }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "保存 VPN 提示设置失败") }
                        }
                    }
                }) {
                    Text("我知道了")
                }
            },
        )
    }

    when {
        checkingSession -> CenterStatus("正在同步本地会话")
        user == null -> NativeLoginScreen(
            api = api,
            platform = platform,
            snackbarHostState = snackbarHostState,
            onLoggedIn = { user = it },
        )
        else -> NativeShellWithDrawer(
            environment = environment,
            api = api,
            platform = platform,
            user = user!!,
            snackbarHostState = snackbarHostState,
            clipboardDetectionEnabled = clipboardDetectionEnabled,
            vpnWarningEnabled = vpnWarningEnabled,
            onClipboardDetectionChanged = { enabled ->
                clipboardDetectionEnabled = enabled
                scope.launch {
                    runCatching { api.updateClipboardDetection(enabled) }
                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "保存设置失败") }
                }
            },
            onVpnWarningChanged = { enabled ->
                vpnWarningEnabled = enabled
                scope.launch {
                    runCatching { api.updateVpnWarning(enabled) }
                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "保存 VPN 提示设置失败") }
                }
            },
            onLogout = {
                scope.launch {
                    runCatching { api.logout() }
                    user = null
                }
            },
            onUserRefreshed = { user = it },
        )
    }
}

@Composable
private fun CenterStatus(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(text)
        }
    }
}

private fun copyPlainText(context: Context, label: String, text: String): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    return true
}

@Composable
private fun NativeLoginScreen(
    api: NativeApiClient,
    platform: NativePlatformActions,
    snackbarHostState: SnackbarHostState,
    onLoggedIn: (UserAuth) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var qr by remember { mutableStateOf<QrCode?>(null) }
    var status by remember { mutableStateOf("生成二维码中") }
    var loading by remember { mutableStateOf(false) }
    var showManualCookie by rememberSaveable { mutableStateOf(false) }
    var manualCookie by rememberSaveable { mutableStateOf("") }

    fun generateQr() {
        scope.launch {
            loading = true
            status = "生成二维码中"
            runCatching { api.generateQrCode() }
                .onSuccess {
                    qr = it
                    status = "等待扫码。扫码后请等待片刻，授权成功会自动进入。"
                }
                .onFailure { status = localizeQrStatusMessage(it.message ?: "二维码生成失败") }
            loading = false
        }
    }

    LaunchedEffect(Unit) { generateQr() }
    LaunchedEffect(qr?.sign) {
        val sign = qr?.sign ?: return@LaunchedEffect
        while (true) {
            delay(1_500)
            when (val result = runCatching { api.qrStatus(sign) }.getOrElse { QrStatus.Failed(it.message ?: "登录失败") }) {
                QrStatus.Waiting -> status = "等待扫码。扫码后请等待片刻。"
                QrStatus.Scanned -> status = "扫描成功，请在手机百度网盘 App 中确认登录。"
                is QrStatus.Success -> {
                    status = "授权成功，正在同步会话。"
                    val synced = runCatching { api.currentUser() }.getOrDefault(result.user)
                    status = "登录完成，正在进入文件页。"
                    delay(350)
                    onLoggedIn(synced)
                    break
                }
                QrStatus.Expired -> {
                    status = "二维码已过期，请重新生成。"
                    break
                }
                is QrStatus.Failed -> {
                    status = localizeQrStatusMessage(result.reason)
                    break
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("柏渡云盘", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("原生 Android 前端 · 本地 Rust 核心", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(shape = RoundedCornerShape(28.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("扫码登录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        QrImage(qr?.imageBase64)
                        Text(status, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { generateQr() }, enabled = !loading) {
                                Text("重新生成")
                            }
                            OutlinedButton(
                                onClick = {
                                    platform.startCookieLogin { result ->
                                        scope.launch {
                                            if (result.status == "success" && !result.cookies.isNullOrBlank()) {
                                                status = "正在导入网页登录 Cookie。"
                                                runCatching { api.loginWithCookies(result.cookies) }
                                                    .onSuccess { loggedIn ->
                                                        val synced = runCatching { api.currentUser() }.getOrDefault(loggedIn)
                                                        onLoggedIn(synced)
                                                    }
                                                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Cookie 登录失败") }
                                            } else {
                                                snackbarHostState.showSnackbar(result.reason ?: "网页登录已取消")
                                            }
                                        }
                                    }
                                },
                            ) {
                                Text("网页登录")
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualCookie = !showManualCookie },
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("高级备用方式：手动 Cookie 登录", fontWeight = FontWeight.SemiBold)
                            Text(if (showManualCookie) "收起" else "展开")
                        }
                        AnimatedVisibility(showManualCookie) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = manualCookie,
                                    onValueChange = { manualCookie = it },
                                    label = { Text("Cookie") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = {
                                        scope.launch {
                                            runCatching { api.loginWithCookies(manualCookie) }
                                                .onSuccess { loggedIn ->
                                                    val synced = runCatching { api.currentUser() }.getOrDefault(loggedIn)
                                                    onLoggedIn(synced)
                                                }
                                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "Cookie 登录失败") }
                                        }
                                    },
                                    enabled = manualCookie.isNotBlank(),
                                ) {
                                    Text("导入 Cookie")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrImage(imageBase64: String?) {
    val bitmap = remember(imageBase64) {
        runCatching {
            val raw = imageBase64.orEmpty().substringAfter("base64,", imageBase64.orEmpty())
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    Surface(
        modifier = Modifier.size(252.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    return
    Box(
        modifier = Modifier
            .size(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "登录二维码", modifier = Modifier.size(200.dp))
        } else {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeShell(
    environment: ServerEnvironment,
    api: NativeApiClient,
    platform: NativePlatformActions,
    user: UserAuth,
    snackbarHostState: SnackbarHostState,
    clipboardDetectionEnabled: Boolean,
    onClipboardDetectionChanged: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(NativeScreen.Files) }
    val bottomScreens = listOf(
        NativeScreen.Files,
        NativeScreen.Downloads,
        NativeScreen.Uploads,
        NativeScreen.ShareTransfer,
        NativeScreen.Settings,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screen.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    TextButton(onClick = { screen = NativeScreen.CloudDl }) { Text("离线") }
                    IconButton(onClick = { screen = NativeScreen.Credits }) {
                        Icon(Icons.Rounded.Info, contentDescription = "开源许可")
                    }
                },
            )
        },
        bottomBar = {
            if (screen in bottomScreens) {
                NavigationBar {
                    bottomScreens.forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            icon = { BottomIcon(item) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (screen) {
                NativeScreen.Files -> FilesScreen(environment, api, platform, snackbarHostState, clipboardDetectionEnabled)
                NativeScreen.Downloads -> DownloadsScreen(api, platform, snackbarHostState)
                NativeScreen.Uploads -> UploadsScreen(api, platform, snackbarHostState)
                NativeScreen.ShareTransfer -> ShareTransferScreen(api, platform, snackbarHostState, clipboardDetectionEnabled)
                NativeScreen.Settings -> SettingsScreen(
                    user = user,
                    clipboardDetectionEnabled = clipboardDetectionEnabled,
                    onClipboardDetectionChanged = onClipboardDetectionChanged,
                    onOpenCredits = { screen = NativeScreen.Credits },
                    onLogout = onLogout,
                )
                NativeScreen.CloudDl -> CloudDlScreen(api, snackbarHostState)
                NativeScreen.Credits -> CreditsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeShellWithDrawer(
    environment: ServerEnvironment,
    api: NativeApiClient,
    platform: NativePlatformActions,
    user: UserAuth,
    snackbarHostState: SnackbarHostState,
    clipboardDetectionEnabled: Boolean,
    vpnWarningEnabled: Boolean,
    onClipboardDetectionChanged: (Boolean) -> Unit,
    onVpnWarningChanged: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onUserRefreshed: (UserAuth) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(NativeScreen.Files) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val colors = webReplicaColors()
    var userMenuExpanded by remember { mutableStateOf(false) }
    val bottomScreens = listOf(
        NativeScreen.Files,
        NativeScreen.Downloads,
        NativeScreen.Uploads,
        NativeScreen.ShareTransfer,
        NativeScreen.Settings,
    )

    fun openScreen(target: NativeScreen) {
        screen = target
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color(0x7502080E),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 280.dp),
                drawerContainerColor = Color.Transparent,
                drawerContentColor = Color.White,
            ) {
                WebDrawerBackground(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color(0xFF409EFF), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("柏渡云盘", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    listOf(
                        NativeScreen.Files,
                        NativeScreen.Downloads,
                        NativeScreen.Uploads,
                        NativeScreen.ShareTransfer,
                        NativeScreen.CloudDl,
                        NativeScreen.Settings,
                        NativeScreen.Credits,
                    ).forEach { item ->
                        WebDrawerMenuItem(
                            selected = screen == item,
                            title = item.title,
                            icon = { BottomIcon(item, tint = if (screen == item) Color.White else Color.White.copy(alpha = 0.72f)) },
                            onClick = { openScreen(item) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = colors.accentSoft,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(user.username.take(1).ifBlank { "用" }, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                user.username.ifBlank { "UID ${user.uid}" },
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        OutlinedButton(onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("退出百度")
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            containerColor = colors.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceOverlay),
                ) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WebReplicaDimens.HeaderHeight)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WebRoundIconButton(
                            icon = Icons.Rounded.Menu,
                            contentDescription = "打开侧边菜单",
                            size = 38.dp,
                            onClick = { scope.launch { drawerState.open() } },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            screen.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.text,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .clickable { userMenuExpanded = true },
                                shape = RoundedCornerShape(19.dp),
                                color = colors.accentSoft,
                                border = BorderStroke(1.dp, colors.border),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(user.username.take(1).ifBlank { "用" }, color = colors.accent, fontWeight = FontWeight.Bold)
                                }
                            }
                            DropdownMenu(
                                expanded = userMenuExpanded,
                                onDismissRequest = { userMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(user.username.ifBlank { "个人信息" }) },
                                    onClick = { userMenuExpanded = false },
                                )
                                DropdownMenuItem(
                                    text = { Text("开源许可与鸣谢") },
                                    onClick = {
                                        userMenuExpanded = false
                                        screen = NativeScreen.Credits
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("退出百度账号") },
                                    onClick = {
                                        userMenuExpanded = false
                                        onLogout()
                                    },
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = colors.border)
                }
            },
            bottomBar = {
                if (screen in bottomScreens) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceOverlay),
                    ) {
                        HorizontalDivider(color = colors.border)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(WebReplicaDimens.TabBarHeight),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            bottomScreens.forEach { item ->
                                WebBottomNavItem(
                                    selected = screen == item,
                                    label = item.label,
                                    icon = { BottomIcon(item, tint = if (screen == item) colors.accent else colors.textSecondary) },
                                    onClick = { screen = item },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when (screen) {
                    NativeScreen.Files -> FilesScreen(environment, api, platform, snackbarHostState, clipboardDetectionEnabled)
                    NativeScreen.Downloads -> DownloadsScreen(api, platform, snackbarHostState)
                    NativeScreen.Uploads -> UploadsScreen(api, platform, snackbarHostState)
                    NativeScreen.ShareTransfer -> ShareTransferScreen(api, platform, snackbarHostState, clipboardDetectionEnabled)
                    NativeScreen.Settings -> NativeSettingsScreen(
                        api = api,
                        user = user,
                        environment = environment,
                        clipboardDetectionEnabled = clipboardDetectionEnabled,
                        vpnWarningEnabled = vpnWarningEnabled,
                        onClipboardDetectionChanged = onClipboardDetectionChanged,
                        onVpnWarningChanged = onVpnWarningChanged,
                        onOpenCredits = { screen = NativeScreen.Credits },
                        onLogout = onLogout,
                        onUserRefreshed = onUserRefreshed,
                        snackbarHostState = snackbarHostState,
                    )
                    NativeScreen.CloudDl -> CloudDlScreen(api, snackbarHostState)
                    NativeScreen.Credits -> CreditsScreen()
                }
            }
        }
    }
}

@Composable
private fun BottomIcon(
    screen: NativeScreen,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val icon = when (screen) {
        NativeScreen.Files -> Icons.Rounded.Folder
        NativeScreen.Downloads -> Icons.Rounded.Download
        NativeScreen.Uploads -> Icons.Rounded.Upload
        NativeScreen.ShareTransfer -> Icons.Rounded.Share
        NativeScreen.Settings -> Icons.Rounded.Settings
        NativeScreen.CloudDl -> Icons.Rounded.CloudDownload
        NativeScreen.Credits -> Icons.Rounded.Info
    }
    Icon(icon, contentDescription = screen.label, tint = tint)
}

@Composable
private fun WebDrawerMenuItem(
    selected: Boolean,
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (selected) Color(0xFF409EFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(
            title,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun WebBottomNavItem(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = webReplicaColors()
    Column(
        modifier = modifier
            .height(WebReplicaDimens.TabBarHeight)
            .clickable(onClick = onClick)
            .padding(top = 7.dp, bottom = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Text(
            label,
            color = if (selected) colors.accent else colors.textSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = webReplicaColors()
    WebSurfaceCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.text)
        content()
    }
}

@Composable
private fun DeleteConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("请输入：删除") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = input == "删除") {
                Text("确认删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FilesScreen(
    environment: ServerEnvironment,
    api: NativeApiClient,
    platform: NativePlatformActions,
    snackbarHostState: SnackbarHostState,
    clipboardDetectionEnabled: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dir by rememberSaveable { mutableStateOf("/") }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var shareExpanded by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FileItem?>(null) }
    var newFolderName by rememberSaveable { mutableStateOf("") }
    var createFolderExpanded by rememberSaveable { mutableStateOf(false) }
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    var detectedShare by remember { mutableStateOf<DetectedShareLink?>(null) }
    var pendingDownload by remember { mutableStateOf<FileItem?>(null) }
    var pendingDownloadInitialDir by rememberSaveable { mutableStateOf(environment.downloadDir.absolutePath) }
    var pendingDownloadConflict by rememberSaveable { mutableStateOf("overwrite") }

    fun load() {
        scope.launch {
            loading = true
            runCatching { api.files(dir) }
                .onSuccess { files = it.list }
                .onFailure { snackbarHostState.showSnackbar(it.message ?: "加载文件失败") }
            loading = false
        }
    }

    fun createDownloadTask(file: FileItem, targetDir: String, conflictStrategy: String) {
        scope.launch {
            runCatching {
                api.createBatchDownload(
                    BatchDownloadRequest(
                        items = listOf(
                            BatchDownloadItem(
                                fsId = file.fsId,
                                path = file.path,
                                name = file.displayName,
                                isDir = file.isDirectory,
                                size = file.size.takeIf { !file.isDirectory },
                                originalName = file.originalName,
                            ),
                        ),
                        targetDir = targetDir,
                        conflictStrategy = conflictStrategy,
                    ),
                )
            }.onSuccess { result ->
                val successCount = result.taskIds.size + result.folderTaskIds.size
                val failText = result.failed.firstOrNull()?.let { "，失败：${it.reason}" }.orEmpty()
                snackbarHostState.showSnackbar(
                    if (successCount > 0) "下载任务已创建$failText" else "下载未创建$failText",
                )
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "创建下载失败")
            }
        }
    }

    fun requestDownload(file: FileItem) {
        scope.launch {
            runCatching {
                val config = api.config()
                val download = config.objectValue("download")
                val conflict = config.objectValue("conflict_strategy")
                    .stringValue("default_download_strategy", "overwrite")
                val configuredDir = download.stringValue("default_directory").ifBlank {
                    download.stringValue("recent_directory").ifBlank {
                        download.stringValue("download_dir", environment.downloadDir.absolutePath)
                    }
                }
                val targetDir = configuredDir.ifBlank { environment.downloadDir.absolutePath }
                if (download.boolValue("ask_each_time", true)) {
                    pendingDownload = file
                    pendingDownloadInitialDir = targetDir
                    pendingDownloadConflict = conflict
                } else {
                    api.updateRecentDir("download", targetDir)
                    createDownloadTask(file, targetDir, conflict)
                }
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "读取下载配置失败")
            }
        }
    }

    LaunchedEffect(dir) { load() }
    LaunchedEffect(clipboardDetectionEnabled) {
        detectedShare = if (clipboardDetectionEnabled) parseBaiduShareLink(platform.readClipboardText()) else null
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            title = "删除 ${target.displayName}",
            message = if (target.isDirectory) {
                "这是一个文件夹。删除后文件夹内内容也会被删除或移入网盘回收站，具体以百度网盘机制为准。"
            } else {
                "删除后文件可能会移入网盘回收站，具体以百度网盘机制为准。"
            },
            onDismiss = { deleteTarget = null },
            onConfirm = {
                scope.launch {
                    runCatching { api.deleteFiles(listOf(target.path)) }
                        .onSuccess {
                            snackbarHostState.showSnackbar("已提交删除：${it.deletedCount} 项")
                            deleteTarget = null
                            load()
                        }
                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "删除失败") }
                }
            },
        )
    }

    pendingDownload?.let { file ->
        AndroidPublicDownloadDirPickerDialog(
            rootDirPath = environment.downloadDir.absolutePath,
            initialPath = pendingDownloadInitialDir,
            onDismiss = { pendingDownload = null },
            onConfirm = { selection ->
                pendingDownload = null
                scope.launch {
                    runCatching {
                        if (selection.setAsDefault) {
                            api.setDefaultDownloadDir(selection.path)
                        } else {
                            api.updateRecentDir("download", selection.path)
                        }
                    }.onFailure {
                        snackbarHostState.showSnackbar(it.message ?: "保存下载目录失败")
                    }
                }
                createDownloadTask(file, selection.path, pendingDownloadConflict)
            },
        )
    }

    WebReplicaBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WebToolbarRow {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = webReplicaColors().textSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        dir,
                        color = webReplicaColors().text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                ShareTransferCapsule(
                    detectedShare = detectedShare,
                    expanded = shareExpanded,
                    modifier = Modifier.widthIn(min = 128.dp, max = 216.dp),
                    onClick = { shareExpanded = !shareExpanded },
                )
                WebRoundIconButton(Icons.Rounded.Refresh, "刷新", primary = true, onClick = { load() })
                Box {
                    WebRoundIconButton(Icons.Rounded.MoreHoriz, "更多", onClick = { moreExpanded = true })
                    DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("新建文件夹") },
                            onClick = {
                                moreExpanded = false
                                createFolderExpanded = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("分享直下") },
                            onClick = {
                                moreExpanded = false
                                scope.launch { snackbarHostState.showSnackbar("请在分享与转存页使用转存后自动下载") }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("上一级") },
                            enabled = dir != "/",
                            onClick = {
                                moreExpanded = false
                                dir = dir.trimEnd('/').substringBeforeLast('/', "").ifBlank { "/" }
                            },
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = shareExpanded,
                enter = fadeIn(tween(160)) + expandVertically(animationSpec = tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(animationSpec = tween(180)),
            ) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    ShareTransferMiniCard(
                        api = api,
                        snackbarHostState = snackbarHostState,
                        detectedShare = detectedShare,
                        defaultSavePath = dir,
                    )
                }
            }
            AnimatedVisibility(
                visible = createFolderExpanded,
                enter = fadeIn(tween(160)) + expandVertically(animationSpec = tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(animationSpec = tween(180)),
            ) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    WebSurfaceCard {
                        Text("新建文件夹", color = webReplicaColors().text, fontWeight = FontWeight.Bold)
                        WebTextInput(newFolderName, { newFolderName = it }, "文件夹名称")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { createFolderExpanded = false },
                                modifier = Modifier.weight(1f),
                            ) { Text("取消") }
                            Button(
                                onClick = {
                                    val name = newFolderName.trim()
                                    val path = (dir.trimEnd('/') + "/" + name).replace("//", "/")
                                    scope.launch {
                                        runCatching { api.createFolder(path) }
                                            .onSuccess {
                                                newFolderName = ""
                                                createFolderExpanded = false
                                                load()
                                            }
                                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "新建失败") }
                                    }
                                },
                                enabled = newFolderName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) { Text("创建") }
                        }
                    }
                }
            }
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(files, key = { "${it.fsId}:${it.path}" }) { file ->
                    FileRow(
                        file = file,
                        onOpen = { if (file.isDirectory) dir = file.path },
                        onDownload = { requestDownload(file) },
                        onShare = {
                            scope.launch {
                                runCatching { api.createShare(listOf(file.path), period = 7) }
                                    .onSuccess {
                                        copyPlainText(context, if (it.pwd.isBlank()) "分享链接" else "分享链接和提取码", if (it.pwd.isBlank()) it.link else "${it.link} 提取码: ${it.pwd}")
                                        snackbarHostState.showSnackbar(
                                            if (it.pwd.isBlank()) "分享已创建：${it.link}" else "分享已创建：${it.link} 提取码 ${it.pwd}",
                                        )
                                        snackbarHostState.showSnackbar("分享链接已复制，可在分享页的“我的分享”重新查看分享信息")
                                    }
                                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "创建分享失败") }
                            }
                        },
                        onDelete = { deleteTarget = file },
                    )
                }
                if (!loading && files.isEmpty()) {
                    item {
                        WebSurfaceCard {
                            Text("当前目录为空", color = webReplicaColors().textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: FileItem,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = webReplicaColors()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = file.isDirectory) { onOpen() },
        shape = RoundedCornerShape(12.dp),
        color = if (file.isDirectory && !androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFFFBF0) else colors.surfaceStrong,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.Folder,
                contentDescription = null,
                tint = if (file.isDirectory) colors.folder else Color(0xFF409EFF),
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = colors.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${if (file.isDirectory) "文件夹" else formatBytes(file.size)} · ${if (file.serverMtime > 0) file.serverMtime else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            WebRoundIconButton(Icons.Rounded.Share, "分享", size = 32.dp, onClick = onShare)
            WebRoundIconButton(Icons.Rounded.Download, "下载", primary = true, size = 32.dp, onClick = onDownload)
            WebRoundIconButton(Icons.Rounded.Delete, "删除", danger = true, size = 32.dp, onClick = onDelete)
        }
    }
}

@Composable
private fun ShareTransferMiniCard(
    api: NativeApiClient,
    snackbarHostState: SnackbarHostState,
    detectedShare: DetectedShareLink?,
    defaultSavePath: String,
) {
    val scope = rememberCoroutineScope()
    var shareUrl by rememberSaveable(detectedShare?.shareUrl) { mutableStateOf(detectedShare?.shareUrl.orEmpty()) }
    var password by rememberSaveable(detectedShare?.password) { mutableStateOf(detectedShare?.password.orEmpty()) }
    var savePath by rememberSaveable(defaultSavePath) { mutableStateOf(defaultSavePath.ifBlank { "/" }) }
    var autoDownload by rememberSaveable { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var selectingFiles by remember { mutableStateOf(false) }
    var previewFiles by remember { mutableStateOf<List<SharedFileInfo>>(emptyList()) }
    var selectedFsIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showSavePathPicker by rememberSaveable { mutableStateOf(false) }

    if (showSavePathPicker) {
        NetdiskDirectoryPickerDialog(
            api = api,
            initialPath = savePath,
            title = "选择转存目录",
            onDismiss = { showSavePathPicker = false },
            onConfirm = { selection ->
                savePath = selection.path
                showSavePathPicker = false
            },
        )
    }

    WebSurfaceCard {
        val colors = webReplicaColors()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("分享转存", color = colors.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("粘贴分享链接，转存到自己的网盘", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (detectedShare != null) {
            Surface(
                color = colors.accentSoft,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f)),
            ) {
                Text(
                    "已从剪贴板识别到分享链接",
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        WebTextInput(shareUrl, { shareUrl = it }, "分享链接", placeholder = "粘贴 pan.baidu.com 分享链接")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WebTextInput(password, { password = it }, "提取码", modifier = Modifier.weight(1f), placeholder = "可选")
            Box(modifier = Modifier.weight(1f)) {
                SettingPickerField(
                    label = "保存到网盘",
                    value = savePath,
                    onClick = { showSavePathPicker = true },
                )
            }
        }
        WebSwitchRow("转存后下载", autoDownload, { autoDownload = it })
        AnimatedVisibility(
            visible = selectingFiles,
            enter = fadeIn(tween(160)) + expandVertically(animationSpec = tween(220)),
            exit = fadeOut(tween(120)) + shrinkVertically(animationSpec = tween(180)),
        ) {
            WebSurfaceCard(color = colors.surfaceMuted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${selectedFsIds.size} 项已选", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = {
                        selectingFiles = false
                        selectedFsIds = emptySet()
                    }) {
                        Text("返回全部转存")
                    }
                }
                previewFiles.take(8).forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFsIds = if (file.fsId in selectedFsIds) {
                                    selectedFsIds - file.fsId
                                } else {
                                    selectedFsIds + file.fsId
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = file.fsId in selectedFsIds,
                            onCheckedChange = { checked ->
                                selectedFsIds = if (checked) selectedFsIds + file.fsId else selectedFsIds - file.fsId
                            },
                        )
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = if (file.isDir) colors.folder else Color(0xFF409EFF))
                        Column(Modifier.weight(1f)) {
                            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.text)
                            Text(if (file.isDir) "文件夹" else formatBytes(file.size), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (previewFiles.size > 8) {
                    Text("已显示前 8 项；如需更细选择，可后续进入完整选择器。", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        previewing = true
                        runCatching { api.previewShareFiles(shareUrl, password) }
                            .onSuccess {
                                previewFiles = it.files
                                selectedFsIds = it.files.map { file -> file.fsId }.toSet()
                                selectingFiles = true
                            }
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "预览分享文件失败") }
                        previewing = false
                    }
                },
                enabled = shareUrl.isNotBlank() && !previewing,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (previewing) "预览中..." else "选择文件")
            }
            Button(
                onClick = {
                    scope.launch {
                        runCatching {
                            api.createTransfer(
                                shareUrl = shareUrl,
                                password = password,
                                savePath = savePath,
                                autoDownload = autoDownload,
                                selectedFsIds = selectedFsIds.takeIf { selectingFiles }?.toList(),
                            )
                        }
                            .onSuccess { snackbarHostState.showSnackbar("转存任务已创建") }
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "转存失败") }
                    }
                },
                enabled = shareUrl.isNotBlank() && (!selectingFiles || selectedFsIds.isNotEmpty()),
                modifier = Modifier.weight(1.2f),
            ) {
                Text("开始转存")
            }
        }
    }
}

@Composable
private fun ShareTransferCapsule(
    detectedShare: DetectedShareLink?,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    WebPillButton(
        text = when {
            detectedShare != null -> "识别到分享，请点击转存"
            expanded -> "正在转存分享"
            else -> "分享链接转存"
        },
        icon = Icons.Rounded.Share,
        modifier = modifier,
        detected = detectedShare != null,
        expanded = expanded,
        onClick = onClick,
    )
}

@Composable
private fun DownloadsScreen(
    api: NativeApiClient,
    platform: NativePlatformActions,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<DownloadTask>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DownloadTask?>(null) }

    suspend fun loadOnce(): List<DownloadTask> {
        loading = true
        val loaded = runCatching { api.downloads() }
            .onSuccess { tasks = it }
            .onFailure { snackbarHostState.showSnackbar(it.message ?: "加载下载任务失败") }
            .getOrDefault(tasks)
        loading = false
        return loaded
    }

    fun load() {
        scope.launch { loadOnce() }
    }

    fun hasLocalFile(task: DownloadTask): Boolean =
        task.status in listOf("completed", "paused", "downloading", "running")

    fun deleteTask(task: DownloadTask, deleteFile: Boolean) {
        scope.launch {
            runCatching { api.deleteDownload(task.id, deleteFile) }
                .onSuccess {
                    snackbarHostState.showSnackbar(if (deleteFile) "任务和文件已删除" else "任务已删除")
                    load()
                }
                .onFailure { snackbarHostState.showSnackbar(it.message ?: "删除任务失败") }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val latest = loadOnce()
            val hasActive = latest.any { it.status in listOf("pending", "downloading", "running") }
            delay(if (hasActive) 800 else 4_000)
        }
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除确认") },
            text = { Text("请选择删除方式：仅删除任务，或同时删除本地文件和任务。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    deleteTask(task, deleteFile = true)
                }) {
                    Text("删除文件和任务")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("取消")
                    }
                    TextButton(onClick = {
                        pendingDelete = null
                        deleteTask(task, deleteFile = false)
                    }) {
                        Text("仅删除任务")
                    }
                }
            },
        )
        return@let
        if (hasLocalFile(task)) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除确认") },
                text = { Text("是否同时删除本地已下载的文件？") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDelete = null
                        deleteTask(task, deleteFile = true)
                    }) {
                        Text("删除文件")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pendingDelete = null }) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            pendingDelete = null
                            deleteTask(task, deleteFile = false)
                        }) {
                            Text("仅删除任务")
                        }
                    }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除确认") },
                text = { Text("确定要删除此任务吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDelete = null
                        deleteTask(task, deleteFile = false)
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("取消")
                    }
                },
            )
        }
    }

    TaskList(
        title = "下载任务",
        loading = loading,
        emptyText = "暂无下载任务",
        items = tasks,
        name = { it.remotePath.substringAfterLast('/').ifBlank { it.id } },
        status = { localizeTaskStatus(it.status) },
        progress = { progress(it.downloadedSize, it.totalSize) },
        detail = {
            "${formatBytes(it.downloadedSize)} / ${formatBytes(it.totalSize)} · ${formatBytes(it.speed)}/s" +
                (it.error?.takeIf { error -> error.isNotBlank() }?.let { error -> " · 错误: $error" } ?: "")
        },
        actions = { task ->
            if (canToggleDownloadTask(task.status)) {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            if (task.status == "paused") api.resumeDownload(task.id) else api.pauseDownload(task.id)
                        }
                        load()
                    }
                }) {
                    Text(if (task.status == "paused") "继续" else "暂停")
                }
            }
            TextButton(onClick = {
                val result = platform.openFolder(task.localPath)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (result.status == "opened") "已尝试打开文件夹" else "系统文件管理器不可用，请到下载目录查看",
                    )
                }
            }) {
                Text("打开文件夹")
            }
            TextButton(onClick = { pendingDelete = task }) {
                Text("删除")
            }
        },
    )
}

@Composable
private fun UploadsScreen(
    api: NativeApiClient,
    platform: NativePlatformActions,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<UploadTask>>(emptyList()) }
    var entries by remember { mutableStateOf<List<ImportedEntry>>(emptyList()) }
    var targetDir by rememberSaveable { mutableStateOf("/") }
    var encrypt by rememberSaveable { mutableStateOf(false) }
    var showUploadTargetPicker by rememberSaveable { mutableStateOf(false) }

    fun load() {
        scope.launch {
            runCatching { api.uploads() }
                .onSuccess { tasks = it }
                .onFailure { snackbarHostState.showSnackbar(it.message ?: "加载上传任务失败") }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            load()
            delay(3_000)
        }
    }

    if (showUploadTargetPicker) {
        NetdiskDirectoryPickerDialog(
            api = api,
            initialPath = targetDir,
            title = "选择上传目标目录",
            onDismiss = { showUploadTargetPicker = false },
            onConfirm = {
                targetDir = it.path
                showUploadTargetPicker = false
            },
        )
    }

    if (entries.isNotEmpty() && !showUploadTargetPicker) {
        AlertDialog(
            onDismissRequest = { entries = emptyList() },
            title = { Text("确认上传") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("已导入 ${entries.size} 项到 App 专属目录。")
                    SettingPickerField("网盘目标目录", targetDir, { showUploadTargetPicker = true })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("上传时加密", modifier = Modifier.weight(1f))
                        Switch(checked = encrypt, onCheckedChange = { encrypt = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val imported = entries
                    entries = emptyList()
                    scope.launch {
                        runCatching {
                            imported.forEach { entry ->
                                val remote = (targetDir.trimEnd('/') + "/" + entry.name).replace("//", "/")
                                if (entry.entryType == "directory") {
                                    api.createFolderUpload(entry.path, remote, encrypt)
                                } else {
                                    api.createUpload(entry.path, remote, encrypt)
                                }
                            }
                        }.onSuccess {
                            snackbarHostState.showSnackbar("上传任务已入队")
                            platform.cleanupImportedPaths(imported.map { it.path })
                            load()
                        }.onFailure {
                            snackbarHostState.showSnackbar(it.message ?: "创建上传失败")
                        }
                    }
                }) {
                    Text("确认上传")
                }
            },
            dismissButton = { TextButton(onClick = { entries = emptyList() }) { Text("取消") } },
        )
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("上传") {
                Text("点击上传后先导入到 App 专属目录，再确认目标网盘目录，之后自动入队。")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        platform.importFiles({ entries = it }, { scope.launch { snackbarHostState.showSnackbar(it) } })
                    }) {
                        Text("选择文件")
                    }
                    OutlinedButton(onClick = {
                        platform.importFolder({ entries = it }, { scope.launch { snackbarHostState.showSnackbar(it) } })
                    }) {
                        Text("选择文件夹")
                    }
                }
            }
        }
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                title = task.remotePath.substringAfterLast('/').ifBlank { task.id },
                status = localizeTaskStatus(task.status),
                progress = progress(task.uploadedSize, task.totalSize),
                detail = "${formatBytes(task.uploadedSize)} / ${formatBytes(task.totalSize)} · ${formatBytes(task.speed)}/s",
            ) {
                if (canToggleUploadTask(task.status)) {
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { if (task.status == "paused") api.resumeUpload(task.id) else api.pauseUpload(task.id) }
                            load()
                        }
                    }) {
                        Text(if (task.status == "paused") "继续" else "暂停")
                    }
                }
                TextButton(onClick = { scope.launch { runCatching { api.deleteUpload(task.id) }; load() } }) {
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun ShareTransferScreen(
    api: NativeApiClient,
    platform: NativePlatformActions,
    snackbarHostState: SnackbarHostState,
    clipboardDetectionEnabled: Boolean,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf("transfers") }
    var transfers by remember { mutableStateOf<List<TransferTask>>(emptyList()) }
    var shares by remember { mutableStateOf<List<ShareRecord>>(emptyList()) }
    var selectedShareIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var detailShare by remember { mutableStateOf<Pair<ShareRecord, ShareDetailData>?>(null) }
    val detected = remember(clipboardDetectionEnabled) {
        if (clipboardDetectionEnabled) parseBaiduShareLink(platform.readClipboardText()) else null
    }

    fun copyText(label: String, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        scope.launch { snackbarHostState.showSnackbar("已复制") }
    }

    fun load() {
        scope.launch {
            runCatching { api.transfers().tasks }.onSuccess { transfers = it }
            runCatching { api.shares().list }.onSuccess { shares = it }
        }
    }

    LaunchedEffect(Unit) {
        load()
        while (true) {
            delay(4_000)
            load()
        }
    }

    detailShare?.let { (share, detail) ->
        val link = detail.shorturl.ifBlank { share.shortlink }
        val textWithPwd = if (detail.pwd.isBlank()) link else "$link 提取码: ${detail.pwd}"
        AlertDialog(
            onDismissRequest = { detailShare = null },
            title = { Text("分享提取码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(link)
                    Text("提取码：${detail.pwd.ifBlank { "无" }}")
                }
            },
            confirmButton = {
                TextButton(onClick = { copyText("分享链接和提取码", textWithPwd) }) {
                    Text("复制全部")
                }
            },
            dismissButton = {
                TextButton(onClick = { detailShare = null }) {
                    Text("关闭")
                }
            },
        )
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("新建分享转存") {
                ShareTransferMiniCard(api, snackbarHostState, detected, "/")
            }
        }
        item {
            SettingSegmentedTabs(
                selected = selectedTab,
                options = listOf("transfers" to "转存任务", "shares" to "我的分享"),
                onSelectedChange = { selectedTab = it },
            )
        }
        if (selectedTab == "transfers") {
            items(transfers, key = { it.id }) { task ->
                TaskCard(
                    title = task.fileName ?: task.shareUrl,
                    status = task.status,
                    progress = progress(task.transferredCount.toLong(), task.totalCount.toLong()),
                    detail = "${task.transferredCount}/${task.totalCount} · ${task.savePath}",
                ) {
                    TextButton(onClick = { scope.launch { runCatching { api.cancelTransfer(task.id) }; load() } }) {
                        Text("取消")
                    }
                    TextButton(onClick = { scope.launch { runCatching { api.deleteTransfer(task.id) }; load() } }) {
                        Text("删除")
                    }
                }
            }
        } else {
            item {
                WebSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("我的分享 · ${shares.size} 项", color = webReplicaColors().text, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { load() }) { Text("刷新") }
                            if (selectedShareIds.isNotEmpty()) {
                                TextButton(onClick = {
                                    val ids = selectedShareIds.toList()
                                    scope.launch {
                                        runCatching { api.cancelShares(ids) }
                                            .onSuccess {
                                                selectedShareIds = emptySet()
                                                load()
                                                snackbarHostState.showSnackbar("已取消 ${ids.size} 个分享")
                                            }
                                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "批量取消失败") }
                                    }
                                }) {
                                    Text("批量取消", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            items(shares, key = { it.shareId }) { share ->
                TaskCard(
                    title = share.typicalPath.substringAfterLast('/').ifBlank { share.shortlink },
                    status = if (share.status == 0) "正常" else "异常",
                    progress = 1f,
                    detail = "${share.shortlink} · 浏览 ${share.viewCount} · 到期 ${share.expiredTime}",
                ) {
                    Checkbox(
                        checked = share.shareId in selectedShareIds,
                        onCheckedChange = { checked ->
                            selectedShareIds = if (checked) selectedShareIds + share.shareId else selectedShareIds - share.shareId
                        },
                    )
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { api.shareDetail(share.shareId) }
                                .onSuccess { detailShare = share to it }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "获取提取码失败") }
                        }
                    }) {
                        Text("提取码")
                    }
                    TextButton(onClick = { copyText("分享链接", share.shortlink) }) {
                        Text("复制")
                    }
                    TextButton(onClick = { scope.launch { runCatching { api.cancelShares(listOf(share.shareId)) }; load() } }) {
                        Text("取消分享")
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeSettingsScreen(
    api: NativeApiClient,
    user: UserAuth,
    environment: ServerEnvironment,
    clipboardDetectionEnabled: Boolean,
    vpnWarningEnabled: Boolean,
    onClipboardDetectionChanged: (Boolean) -> Unit,
    onVpnWarningChanged: (Boolean) -> Unit,
    onOpenCredits: () -> Unit,
    onLogout: () -> Unit,
    onUserRefreshed: (UserAuth) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var configJson by remember { mutableStateOf<JsonObject?>(null) }
    var recommended by remember { mutableStateOf<RecommendedConfigResponse?>(null) }
    var refreshingVip by remember { mutableStateOf(false) }
    var showDownloadDirPicker by rememberSaveable { mutableStateOf(false) }
    var showTransferPathPicker by rememberSaveable { mutableStateOf(false) }

    var downloadDir by rememberSaveable { mutableStateOf("") }
    var askEachDownload by rememberSaveable { mutableStateOf(false) }
    var downloadThreads by rememberSaveable { mutableStateOf("4") }
    var downloadTasks by rememberSaveable { mutableStateOf("2") }
    var downloadRetries by rememberSaveable { mutableStateOf("3") }

    var uploadThreads by rememberSaveable { mutableStateOf("4") }
    var uploadTasks by rememberSaveable { mutableStateOf("2") }
    var uploadRetries by rememberSaveable { mutableStateOf("3") }
    var skipHiddenFiles by rememberSaveable { mutableStateOf(true) }

    var uploadConflict by rememberSaveable { mutableStateOf("smart_dedup") }
    var downloadConflict by rememberSaveable { mutableStateOf("auto_rename") }
    var transferBehavior by rememberSaveable { mutableStateOf("transfer_only") }
    var transferRecentPath by rememberSaveable { mutableStateOf("/") }

    var proxyType by rememberSaveable { mutableStateOf("none") }
    var proxyHost by rememberSaveable { mutableStateOf("") }
    var proxyPort by rememberSaveable { mutableStateOf("") }
    var proxyUsername by rememberSaveable { mutableStateOf("") }
    var proxyPassword by rememberSaveable { mutableStateOf("") }
    var proxyFallback by rememberSaveable { mutableStateOf(true) }

    var encryptionStatus by remember { mutableStateOf("未加载") }
    var encryptionAlgorithm by rememberSaveable { mutableStateOf("aes-256-gcm") }
    var encryptionKey by rememberSaveable { mutableStateOf("") }

    if (showDownloadDirPicker) {
        AndroidPublicDownloadDirPickerDialog(
            rootDirPath = environment.downloadDir.absolutePath,
            initialPath = downloadDir.ifBlank { environment.downloadDir.absolutePath },
            onDismiss = { showDownloadDirPicker = false },
            onConfirm = { selection ->
                downloadDir = selection.path
                showDownloadDirPicker = false
                if (selection.setAsDefault) {
                    scope.launch {
                        runCatching { api.setDefaultDownloadDir(selection.path) }
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "设置默认目录失败") }
                    }
                }
            },
        )
    }

    if (showTransferPathPicker) {
        NetdiskDirectoryPickerDialog(
            api = api,
            initialPath = transferRecentPath,
            title = "选择转存保存目录",
            onDismiss = { showTransferPathPicker = false },
            onConfirm = { selection ->
                transferRecentPath = selection.path
                showTransferPathPicker = false
            },
        )
    }

    fun fillFromConfig(config: JsonObject, transferConfig: JsonObject?) {
        val download = config.objectValue("download")
        downloadDir = download.stringValue("download_dir", downloadDir)
        askEachDownload = download.boolValue("ask_each_time", false)
        downloadThreads = download.intValue("max_global_threads", 4).toString()
        downloadTasks = download.intValue("max_concurrent_tasks", 2).toString()
        downloadRetries = download.intValue("max_retries", 3).toString()

        val upload = config.objectValue("upload")
        uploadThreads = upload.intValue("max_global_threads", 4).toString()
        uploadTasks = upload.intValue("max_concurrent_tasks", 2).toString()
        uploadRetries = upload.intValue("max_retries", 3).toString()
        skipHiddenFiles = upload.boolValue("skip_hidden_files", true)

        val conflict = config.objectValue("conflict_strategy")
        uploadConflict = conflict.stringValue("default_upload_strategy", "smart_dedup")
        downloadConflict = conflict.stringValue("default_download_strategy", "auto_rename")

        val proxy = config.objectValue("network").objectValue("proxy")
        proxyType = proxy.stringValue("proxy_type", "none")
        proxyHost = proxy.stringValue("host", "")
        proxyPort = proxy.intValue("port", 0).takeIf { it > 0 }?.toString().orEmpty()
        proxyUsername = proxy.stringValue("username", "")
        proxyPassword = proxy.stringValue("password", "")
        proxyFallback = proxy.boolValue("allow_fallback", true)

        val transfer = transferConfig ?: config.objectValue("transfer")
        transferBehavior = transfer.stringValue("default_behavior", "transfer_only")
        transferRecentPath = transfer.stringValue("recent_save_path", "/")
    }

    fun load() {
        scope.launch {
            loading = true
            runCatching {
                runCatching { api.currentUser() }.getOrNull()?.let(onUserRefreshed)
                val config = api.config()
                val transfer = runCatching { api.transferConfig() }.getOrNull()
                recommended = runCatching { api.recommendedConfig() }.getOrNull()
                configJson = config
                fillFromConfig(config, transfer)
                encryptionStatus = runCatching { api.encryptionStatus().toDisplayText() }.getOrDefault("未启用或无法读取")
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "加载设置失败")
            }
            loading = false
        }
    }

    fun refreshVipStatus() {
        scope.launch {
            refreshingVip = true
            runCatching {
                runCatching { api.currentUser() }.getOrNull()?.let(onUserRefreshed)
                recommended = api.recommendedConfig()
            }.onSuccess {
                snackbarHostState.showSnackbar("会员等级已刷新")
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "刷新会员等级失败")
            }
            refreshingVip = false
        }
    }

    fun save() {
        val current = configJson ?: return
        val next = current
            .withObject("download") {
                it.withString("download_dir", downloadDir)
                    .withString("default_directory", downloadDir)
                    .withString("recent_directory", downloadDir)
                    .withBool("ask_each_time", askEachDownload)
                    .withInt("max_global_threads", downloadThreads.toIntOrNull() ?: 4)
                    .withInt("max_concurrent_tasks", downloadTasks.toIntOrNull() ?: 2)
                    .withInt("max_retries", downloadRetries.toIntOrNull() ?: 3)
            }
            .withObject("upload") {
                it.withInt("max_global_threads", uploadThreads.toIntOrNull() ?: 4)
                    .withInt("max_concurrent_tasks", uploadTasks.toIntOrNull() ?: 2)
                    .withInt("max_retries", uploadRetries.toIntOrNull() ?: 3)
                    .withBool("skip_hidden_files", skipHiddenFiles)
            }
            .withObject("conflict_strategy") {
                it.withString("default_upload_strategy", uploadConflict)
                    .withString("default_download_strategy", downloadConflict)
            }
            .withObject("network") { network ->
                network.withObject("proxy") {
                    it.withString("proxy_type", proxyType)
                        .withString("host", proxyHost)
                        .withInt("port", proxyPort.toIntOrNull() ?: 0)
                        .withString("username", proxyUsername)
                        .withString("password", proxyPassword)
                        .withBool("allow_fallback", proxyFallback)
                }
            }
            .withObject("mobile") {
                it.withBool("clipboard_share_detection_enabled", clipboardDetectionEnabled)
                    .withBool("vpn_warning_enabled", vpnWarningEnabled)
            }

        scope.launch {
            runCatching {
                api.updateConfig(next)
                api.updateTransferConfig(
                    buildJsonObject {
                        put("default_behavior", transferBehavior)
                        put("recent_save_fs_id", 0)
                        put("recent_save_path", transferRecentPath.ifBlank { "/" })
                    },
                )
                configJson = next
            }.onSuccess {
                snackbarHostState.showSnackbar("设置已保存")
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "保存设置失败")
            }
        }
    }

    LaunchedEffect(user.uid, user.vipType) { load() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(webReplicaColors().background),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WebSurfaceCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { load() }, enabled = !loading, modifier = Modifier.weight(1f)) {
                        Text("恢复/刷新")
                    }
                    Button(onClick = { save() }, enabled = !loading, modifier = Modifier.weight(1f)) {
                        Text("保存设置")
                    }
                }
            } 
        }
        item {
            CollapsibleSettingsCard("账号", expanded == "account", { expanded = if (expanded == "account") null else "account" }) {
                Text("当前用户：${user.username.ifBlank { user.uid.toString() }}")
                Button(onClick = onLogout) { Text("退出登录") }
            }
        }
        item {
            CollapsibleSettingsCard("下载设置", expanded == "download", { expanded = if (expanded == "download") null else "download" }) {
                recommended?.let { rec ->
                    WebSurfaceCard(color = webReplicaColors().surfaceMuted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("当前等级：${rec.vipName}", color = webReplicaColors().text, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { refreshVipStatus() }, enabled = !refreshingVip) {
                                Text(if (refreshingVip) "刷新中" else "手动刷新")
                            }
                        }
                        Text(
                            "推荐线程 ${rec.recommended.threads}，推荐同时下载 ${rec.recommended.maxTasks}，分片 ${rec.recommended.chunkSize} MB",
                            color = webReplicaColors().textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        rec.warnings.forEach { warning ->
                            Text("警告：$warning", color = webReplicaColors().danger, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                SettingPickerField(
                    label = "默认下载目录",
                    value = downloadDir,
                    onClick = { showDownloadDirPicker = true },
                    helper = "仅在公共 Download/BaiduPCS 及其子目录中选择，避免路径权限问题。",
                )
                SettingSwitchRow("每次下载时询问目录", askEachDownload, { askEachDownload = it })
                SettingSliderField(
                    label = "全局最大线程数",
                    value = downloadThreads.toIntOrNull() ?: 4,
                    valueRange = 1..20,
                    recommended = recommended?.recommended?.threads,
                    warning = recommended?.warnings?.firstOrNull(),
                    showWarning = recommended?.warnings?.isNotEmpty() == true,
                    onValueChange = { downloadThreads = it.toString() },
                )
                SettingSliderField(
                    label = "最大同时下载数",
                    value = downloadTasks.toIntOrNull() ?: 2,
                    valueRange = 1..10,
                    recommended = recommended?.recommended?.maxTasks,
                    warning = recommended?.warnings?.firstOrNull(),
                    showWarning = recommended?.warnings?.isNotEmpty() == true,
                    onValueChange = { downloadTasks = it.toString() },
                )
                SettingStepperField("失败重试次数", downloadRetries.toIntOrNull() ?: 3, 0..10, { downloadRetries = it.toString() })
            }
        }
        item {
            CollapsibleSettingsCard("上传设置", expanded == "upload", { expanded = if (expanded == "upload") null else "upload" }) {
                SettingSliderField("全局最大线程数", uploadThreads.toIntOrNull() ?: 4, 1..16, { uploadThreads = it.toString() })
                SettingSliderField("最大同时上传数", uploadTasks.toIntOrNull() ?: 2, 1..8, { uploadTasks = it.toString() })
                SettingStepperField("失败重试次数", uploadRetries.toIntOrNull() ?: 3, 0..10, { uploadRetries = it.toString() })
                SettingSwitchRow("上传文件夹时跳过隐藏文件", skipHiddenFiles, { skipHiddenFiles = it })
            }
        }
        item {
            CollapsibleSettingsCard("冲突策略", expanded == "conflict", { expanded = if (expanded == "conflict") null else "conflict" }) {
                SettingOptionSelector(
                    label = "上传冲突策略",
                    value = uploadConflict,
                    options = listOf(
                        SettingOption("smart_dedup", "智能去重", "优先复用百度秒传/去重能力，适合常规上传。"),
                        SettingOption("auto_rename", "自动重命名", "遇到同名文件时自动生成新名称。"),
                        SettingOption("overwrite", "覆盖", "同名时覆盖云端已有文件，请谨慎使用。"),
                    ),
                    onValueChange = { uploadConflict = it },
                )
                SettingOptionSelector(
                    label = "下载冲突策略",
                    value = downloadConflict,
                    options = listOf(
                        SettingOption("overwrite", "覆盖", "本地已有同名文件时直接覆盖。"),
                        SettingOption("skip", "跳过", "本地已有同名文件时不再下载。"),
                        SettingOption("auto_rename", "自动重命名", "保留已有文件并给新文件改名。"),
                    ),
                    onValueChange = { downloadConflict = it },
                )
            }
        }
        item {
            CollapsibleSettingsCard("分享与转存", expanded == "transfer", { expanded = if (expanded == "transfer") null else "transfer" }) {
                SettingOptionSelector(
                    label = "默认行为",
                    value = transferBehavior,
                    options = listOf(
                        SettingOption("transfer_only", "仅转存到网盘", "转存完成后停留在网盘中。"),
                        SettingOption("transfer_and_download", "转存后自动下载", "转存完成后自动创建下载任务。"),
                    ),
                    onValueChange = { transferBehavior = it },
                )
                SettingPickerField("最近保存目录", transferRecentPath, { showTransferPathPicker = true })
                SettingSwitchRow("自动识别剪贴板分享链接", clipboardDetectionEnabled, onClipboardDetectionChanged)
            }
        }
        item {
            CollapsibleSettingsCard("移动端体验", expanded == "mobile", { expanded = if (expanded == "mobile") null else "mobile" }) {
                SettingSwitchRow("VPN 环境提示", vpnWarningEnabled, onVpnWarningChanged)
            }
        }
        item {
            CollapsibleSettingsCard("网络代理", expanded == "proxy", { expanded = if (expanded == "proxy") null else "proxy" }) {
                SettingOptionSelector(
                    label = "代理类型",
                    value = proxyType,
                    options = listOf(
                        SettingOption("none", "无代理", "直接连接百度网盘服务。"),
                        SettingOption("http", "HTTP", "使用 HTTP 代理。"),
                        SettingOption("socks5", "SOCKS5", "使用 SOCKS5 代理。"),
                    ),
                    onValueChange = { proxyType = it },
                )
                if (proxyType != "none") {
                    SettingTextField("主机", proxyHost, { proxyHost = it })
                    SettingTextField("端口", proxyPort, { proxyPort = it })
                    SettingTextField("用户名", proxyUsername, { proxyUsername = it })
                    SettingTextField("密码", proxyPassword, { proxyPassword = it })
                }
                SettingSwitchRow("代理失败时允许直连兜底", proxyFallback, { proxyFallback = it })
            }
        }
        item {
            CollapsibleSettingsCard("文件加密", expanded == "encryption", { expanded = if (expanded == "encryption") null else "encryption" }) {
                Text(encryptionStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SettingOptionSelector(
                    label = "算法",
                    value = encryptionAlgorithm,
                    options = listOf(
                        SettingOption("aes-256-gcm", "AES-256-GCM（推荐）", "兼容性好，适合大多数设备。"),
                        SettingOption("chacha20-poly1305", "ChaCha20-Poly1305", "适合部分无 AES 加速的设备。"),
                    ),
                    onValueChange = { encryptionAlgorithm = it },
                )
                OutlinedTextField(
                    value = encryptionKey,
                    onValueChange = { encryptionKey = it },
                    label = { Text("密钥") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            runCatching { encryptionKey = api.generateEncryptionKey(encryptionAlgorithm) }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "生成密钥失败") }
                        }
                    }) { Text("生成") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            runCatching { api.importEncryptionKey(encryptionKey, encryptionAlgorithm) }
                                .onSuccess { encryptionStatus = runCatching { api.encryptionStatus().toDisplayText() }.getOrDefault("已导入") }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "导入密钥失败") }
                        }
                    }, enabled = encryptionKey.isNotBlank()) { Text("导入") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            runCatching { encryptionKey = api.exportEncryptionKey() }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "导出密钥失败") }
                        }
                    }) { Text("导出") }
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { api.deleteEncryptionKey() }
                            .onSuccess { encryptionStatus = "密钥已删除" }
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "删除密钥失败") }
                    }
                }) {
                    Text("删除密钥", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            CollapsibleSettingsCard("开源与合规", expanded == "credits", { expanded = if (expanded == "credits") null else "credits" }) {
                Text("查看许可证、NOTICE、第三方依赖与上游鸣谢。")
                Button(onClick = onOpenCredits) { Text("开源许可与鸣谢") }
            }
        }
    }
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helper: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WebTextInput(value = value, onValueChange = onValueChange, label = label)
        helper?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = webReplicaColors().textSecondary) }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    WebSwitchRow(label, checked, onCheckedChange)
}

private fun JsonObject.objectValue(key: String): JsonObject =
    this[key]?.let { runCatching { it.jsonObject }.getOrNull() } ?: JsonObject(emptyMap())

private fun JsonObject.stringValue(key: String, default: String = ""): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: default

private fun JsonObject.intValue(key: String, default: Int = 0): Int =
    this[key]?.jsonPrimitive?.intOrNull ?: default

private fun JsonObject.boolValue(key: String, default: Boolean = false): Boolean =
    this[key]?.jsonPrimitive?.booleanOrNull ?: default

private fun JsonObject.withObject(
    key: String,
    transform: (JsonObject) -> JsonObject,
): JsonObject =
    JsonObject(this + (key to transform(objectValue(key))))

private fun JsonObject.withString(key: String, value: String): JsonObject =
    JsonObject(this + (key to JsonPrimitive(value)))

private fun JsonObject.withInt(key: String, value: Int): JsonObject =
    JsonObject(this + (key to JsonPrimitive(value)))

private fun JsonObject.withBool(key: String, value: Boolean): JsonObject =
    JsonObject(this + (key to JsonPrimitive(value)))

private fun JsonObject.toDisplayText(): String =
    entries.joinToString(separator = "\n") { (key, value) -> "$key: ${value.toPlainText()}" }

private fun JsonElement.toPlainText(): String =
    runCatching { jsonPrimitive.contentOrNull ?: toString() }.getOrDefault(toString())

@Composable
private fun SettingsScreen(
    user: UserAuth,
    clipboardDetectionEnabled: Boolean,
    onClipboardDetectionChanged: (Boolean) -> Unit,
    onOpenCredits: () -> Unit,
    onLogout: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CollapsibleSettingsCard("账号", expanded == "account", { expanded = if (expanded == "account") null else "account" }) {
                Text("当前用户：${user.username.ifBlank { user.uid.toString() }}")
                Button(onClick = onLogout) { Text("退出登录") }
            }
        }
        item {
            CollapsibleSettingsCard("移动端体验", expanded == "mobile", { expanded = if (expanded == "mobile") null else "mobile" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("自动识别剪贴板分享链接", modifier = Modifier.weight(1f))
                    Switch(checked = clipboardDetectionEnabled, onCheckedChange = onClipboardDetectionChanged)
                }
            }
        }
        item {
            CollapsibleSettingsCard("开源与合规", expanded == "credits", { expanded = if (expanded == "credits") null else "credits" }) {
                Text("查看许可证、NOTICE、第三方依赖与上游鸣谢。")
                Button(onClick = onOpenCredits) { Text("开源许可与鸣谢") }
            }
        }
        item {
            CollapsibleSettingsCard("说明", expanded == "about", { expanded = if (expanded == "about") null else "about" }) {
                Text("当前版本为原生 Android 前端，本地 Rust 核心继续在设备内运行。自动备份入口已隐藏。")
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sectionColor = when {
        title.contains("下载") -> Color(0xFF67C23A)
        title.contains("上传") -> Color(0xFFE6A23C)
        title.contains("代理") -> Color(0xFF9B59B6)
        title.contains("加密") -> Color(0xFFF56C6C)
        title.contains("分享") || title.contains("转存") -> Color(0xFF0F766E)
        title.contains("开源") -> Color(0xFF409EFF)
        else -> Color(0xFF409EFF)
    }
    val description = when {
        title.contains("账号") -> "当前登录状态与退出"
        title.contains("下载") -> "目录、线程与重试策略"
        title.contains("上传") -> "上传并发、重试与过滤"
        title.contains("冲突") -> "上传与下载命名策略"
        title.contains("分享") || title.contains("转存") -> "分享链接、剪贴板与转存默认行为"
        title.contains("移动") -> "VPN 提示与移动端交互"
        title.contains("代理") -> "HTTP/SOCKS5 与连接兜底"
        title.contains("加密") -> "密钥、算法与导入导出"
        title.contains("开源") -> "许可证、NOTICE 与第三方依赖"
        else -> "Android 原生端说明"
    }
    val iconVector = when {
        title.contains("下载") -> Icons.Rounded.Download
        title.contains("上传") -> Icons.Rounded.Upload
        title.contains("分享") || title.contains("转存") -> Icons.Rounded.Share
        title.contains("移动") -> Icons.Rounded.Settings
        title.contains("加密") -> Icons.Rounded.Info
        title.contains("开源") -> Icons.Rounded.Info
        title.contains("代理") -> Icons.Rounded.CloudDownload
        else -> Icons.Rounded.Settings
    }
    WebSettingSection(
        title = title,
        description = description,
        color = sectionColor,
        expanded = expanded,
        onToggle = onToggle,
        icon = { Icon(iconVector, contentDescription = null, tint = sectionColor, modifier = Modifier.size(18.dp)) },
        content = content,
    )
}

@Composable
private fun CloudDlScreen(
    api: NativeApiClient,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf("") }
    var savePath by rememberSaveable { mutableStateOf("/") }
    var autoDownload by rememberSaveable { mutableStateOf(false) }
    var tasks by remember { mutableStateOf<List<CloudDlTask>>(emptyList()) }
    var showSavePathPicker by rememberSaveable { mutableStateOf(false) }

    fun load() {
        scope.launch { runCatching { api.cloudTasks().tasks }.onSuccess { tasks = it } }
    }

    if (showSavePathPicker) {
        NetdiskDirectoryPickerDialog(
            api = api,
            initialPath = savePath,
            title = "选择离线下载保存目录",
            onDismiss = { showSavePathPicker = false },
            onConfirm = {
                savePath = it.path
                showSavePathPicker = false
            },
        )
    }

    LaunchedEffect(Unit) { load() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("新增离线下载") {
                OutlinedTextField(url, { url = it }, label = { Text("下载链接") }, modifier = Modifier.fillMaxWidth())
                SettingPickerField("保存到网盘", savePath, { showSavePathPicker = true })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("完成后自动下载到本机", modifier = Modifier.weight(1f))
                    Switch(checked = autoDownload, onCheckedChange = { autoDownload = it })
                }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching { api.addCloudTask(url, savePath, autoDownload) }
                                .onSuccess {
                                    snackbarHostState.showSnackbar("离线任务已添加")
                                    url = ""
                                    load()
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "添加失败") }
                        }
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("添加任务")
                }
            }
        }
        items(tasks, key = { it.taskId }) { task ->
            TaskCard(
                title = task.taskName.ifBlank { task.sourceUrl },
                status = task.statusText.ifBlank { task.status.toString() },
                progress = progress(task.finishedSize, task.fileSize),
                detail = "${formatBytes(task.finishedSize)} / ${formatBytes(task.fileSize)} · ${task.savePath}",
            ) {
                TextButton(onClick = { scope.launch { runCatching { api.cancelCloudTask(task.taskId) }; load() } }) {
                    Text("取消")
                }
                TextButton(onClick = { scope.launch { runCatching { api.deleteCloudTask(task.taskId) }; load() } }) {
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun CreditsScreen() {
    val context = LocalContext.current
    var sourceTab by rememberSaveable { mutableStateOf("rust") }
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogText by remember { mutableStateOf("") }
    val noticeText = remember(context) { readAssetText(context, "open-source/NOTICE.txt", "未找到 NOTICE.txt") }
    val licenseText = remember(context) { readAssetText(context, "open-source/LICENSE.txt", "未找到 LICENSE.txt") }
    val openSourceIndex = remember(context) {
        runCatching {
            nativeJson.decodeFromString<OpenSourceIndex>(
                readAssetText(context, "open-source/third-party-index.json", """{"packages":[]}"""),
            )
        }.getOrDefault(OpenSourceIndex())
    }
    val packages = remember(sourceTab, openSourceIndex) {
        openSourceIndex.packages.filter { it.source == sourceTab }
    }

    fun openLegalText(title: String, assetPath: String) {
        dialogTitle = title
        dialogText = readAssetText(context, assetPath, "未找到 $title")
    }

    dialogTitle?.let { title ->
        Dialog(
            onDismissRequest = { dialogTitle = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .padding(horizontal = 12.dp, vertical = 20.dp)
                    .heightIn(max = 760.dp),
                shape = RoundedCornerShape(24.dp),
                color = webReplicaColors().surface,
                border = BorderStroke(1.dp, webReplicaColors().border),
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, color = webReplicaColors().text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { dialogTitle = null }) {
                            Text("关闭")
                        }
                    }
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp, max = 620.dp)
                                .verticalScroll(rememberScrollState())
                                .background(webReplicaColors().surfaceStrong, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                        ) {
                            Text(dialogText, color = webReplicaColors().text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(webReplicaColors().background),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WebSurfaceCard {
                Text("开源许可与鸣谢", color = webReplicaColors().text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${openSourceIndex.appName.ifBlank { "百度网盘" }} 基于 BaiduPCS-Rust 进行 Android 本地化封装、移动端适配和运行时集成。这里集中展示版权、许可证、NOTICE 和实际进入 APK 的运行时依赖清单。",
                    color = webReplicaColors().textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SectionCard("应用信息") {
                CreditInfoRow("应用名称", openSourceIndex.appName.ifBlank { "百度网盘" })
                CreditInfoRow("当前版本", "v1.0.0")
                CreditInfoRow("移植说明", "Android 本地化封装、UI 适配与系统能力集成版")
                CreditInfoRow("非官方说明", "本应用为独立 Android 移植版，非上游官方发布，也非相关品牌官方客户端。")
            }
        }
        item {
            SectionCard("上游署名与来源") {
                val upstream = openSourceIndex.upstream
                CreditInfoRow("上游项目", upstream.name.ifBlank { "BaiduPCS-Rust" })
                CreditInfoRow("原作者", upstream.author.ifBlank { "komorebiCarry" })
                CreditInfoRow("引用版本", upstream.version.ifBlank { "v1.12.1" })
                CreditInfoRow("原始许可证", upstream.license.ifBlank { "Apache License 2.0" })
                CreditInfoRow("发布地址", upstream.releaseUrl.ifBlank { "https://github.com/komorebiCarry/BaiduPCS-Rust/releases/tag/v1.12.1" })
            }
        }
        item {
            SectionCard("移植说明与 NOTICE") {
                Text(
                    "本移植版保留上游项目的作者署名、来源链接和 Apache License 2.0 许可文本；与上游直接相关的版权、归属和许可条款，仍以上游仓库发布内容为准。",
                    color = webReplicaColors().textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "应用内提供的 NOTICE 用于说明本 Android 移植版的封装性质、修改方向和非官方发布关系，不改变上游项目原有的版权与许可归属。",
                    color = webReplicaColors().textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                SelectionContainer {
                    Text(noticeText, color = webReplicaColors().text, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { openLegalText("NOTICE", "open-source/NOTICE.txt") }) {
                        Text("查看完整 NOTICE")
                    }
                }
            }
        }
        item {
            SectionCard("第三方运行时依赖") {
                val generatedAtLabel = formatGeneratedAt(openSourceIndex.generatedAt)
                Text(
                    buildString {
                        append("共 ")
                        append(openSourceIndex.packages.size)
                        append(" 项运行时依赖")
                        if (generatedAtLabel.isNotBlank()) {
                            append("，生成时间：")
                            append(generatedAtLabel)
                        }
                    },
                    color = webReplicaColors().textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                SettingSegmentedTabs(
                    selected = sourceTab,
                    options = listOf(
                        "rust" to "Rust (${openSourceIndex.packages.count { it.source == "rust" }})",
                        "web" to "Web (${openSourceIndex.packages.count { it.source == "web" }})",
                        "android" to "Android (${openSourceIndex.packages.count { it.source == "android" }})",
                    ),
                    onSelectedChange = { sourceTab = it },
                )
            }
        }
        if (packages.isEmpty()) {
            item {
                WebSurfaceCard {
                    Text("当前分组没有可显示的运行时依赖", color = webReplicaColors().textSecondary)
                }
            }
        } else {
            items(packages, key = { "${it.source}:${it.name}:${it.version}" }) { entry ->
                OpenSourcePackageRow(
                    entry = entry,
                    onOpenLicense = { openLegalText("${entry.name} ${entry.version} License", entry.licensePath) },
                    onOpenNotice = entry.noticePath?.let { path ->
                        { openLegalText("${entry.name} ${entry.version} NOTICE", path) }
                    },
                )
            }
        }
        item {
            LegalTextCard(
                title = "Apache License 2.0 全文",
                text = licenseText,
                onOpenFull = { openLegalText("Apache License 2.0", "open-source/LICENSE.txt") },
            )
        }
    }
    return
    val license = remember { runCatching { context.assets.open("open-source/LICENSE.txt").bufferedReader().readText() }.getOrDefault("未找到 LICENSE.txt") }
    val notice = remember { runCatching { context.assets.open("open-source/NOTICE.txt").bufferedReader().readText() }.getOrDefault("未找到 NOTICE.txt") }
    val thirdParty = remember { runCatching { context.assets.open("open-source/third-party-index.json").bufferedReader().readText() }.getOrDefault("[]") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("应用信息") {
                Text("柏渡云盘 Android 原生前端")
                Text("基于 BaiduPCS-Rust v1.12.1 的非官方 Android 移植版。")
            }
        }
        item { LegalTextCard("NOTICE", notice) }
        item { LegalTextCard("Apache License 2.0", license) }
        item { LegalTextCard("第三方依赖索引", thirdParty.take(6000) + if (thirdParty.length > 6000) "\n..." else "") }
    }
}

@Composable
private fun LegalTextCard(title: String, text: String) {
    SectionCard(title) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LegalTextCard(
    title: String,
    text: String,
    onOpenFull: (() -> Unit)?,
) {
    SectionCard(title) {
        SelectionContainer {
            Text(text, color = webReplicaColors().text, style = MaterialTheme.typography.bodySmall)
        }
        onOpenFull?.let {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = it) {
                    Text("查看完整内容")
                }
            }
        }
    }
}

@Composable
private fun CreditInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            color = webReplicaColors().textSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(88.dp),
        )
        Text(
            value,
            color = webReplicaColors().text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OpenSourcePackageRow(
    entry: OpenSourcePackageEntry,
    onOpenLicense: () -> Unit,
    onOpenNotice: (() -> Unit)?,
) {
    WebSurfaceCard {
        Text(entry.name, color = webReplicaColors().text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text("版本 ${entry.version}", color = webReplicaColors().textSecondary, style = MaterialTheme.typography.bodySmall)
        Text(entry.licenseExpression, color = webReplicaColors().accent, style = MaterialTheme.typography.labelSmall)
        entry.homepage?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = webReplicaColors().textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onOpenLicense) {
                Text("查看许可证")
            }
            onOpenNotice?.let {
                TextButton(onClick = it) {
                    Text("查看 NOTICE")
                }
            }
        }
    }
}

private fun readAssetText(context: Context, assetPath: String, fallback: String): String =
    runCatching {
        context.assets.open(assetPath.trimStart('/')).bufferedReader().use { it.readText() }
    }.getOrDefault(fallback)

private fun formatGeneratedAt(value: String): String =
    value.removeSuffix("Z").replace('T', ' ').trim()

private fun localizeTaskStatus(status: String): String {
    val raw = status.trim()
    return when (raw.lowercase()) {
        "queued" -> "排队中"
        "preparing" -> "准备中"
        "pending", "waiting" -> "等待中"
        "downloading" -> "下载中"
        "uploading" -> "上传中"
        "transferring" -> "传输中"
        "paused" -> "已暂停"
        "completed", "success" -> "已完成"
        "failed", "error" -> "失败"
        "cancelled", "canceled" -> "已取消"
        else -> if (raw.isBlank()) "未知状态" else if (raw.any { it.code > 127 }) raw else "处理中"
    }
}

private fun canToggleDownloadTask(status: String): Boolean =
    when (status.trim().lowercase()) {
        "pending", "queued", "preparing", "downloading", "paused" -> true
        else -> false
    }

private fun canToggleUploadTask(status: String): Boolean =
    when (status.trim().lowercase()) {
        "pending", "queued", "preparing", "uploading", "paused" -> true
        else -> false
    }

private fun localizeQrStatusMessage(message: String): String {
    val raw = message.trim()
    val normalized = raw.lowercase()
    return when {
        raw.isBlank() -> "登录状态获取失败，请稍后重试。"
        raw.any { it.code > 127 } -> raw
        normalized.contains("expired") -> "二维码已过期，请重新生成。"
        normalized.contains("scanned") -> "扫描成功，请在手机百度网盘 App 中确认登录。"
        normalized.contains("waiting") -> "等待扫码。扫码后请等待片刻。"
        normalized.contains("network") || normalized.contains("socket") -> "网络异常，请稍后重试。"
        normalized.contains("timeout") -> "请求超时，请稍后重试。"
        normalized.contains("request") && normalized.contains("failed") -> "请求失败，请稍后重试。"
        normalized.contains("login") && normalized.contains("failed") -> "登录失败，请稍后重试。"
        normalized.contains("unauthorized") || normalized.contains("forbidden") -> "授权失败，请重新扫码。"
        else -> "登录状态获取失败，请稍后重试。"
    }
}

@Composable
private fun <T> TaskList(
    title: String,
    loading: Boolean,
    emptyText: String,
    items: List<T>,
    name: (T) -> String,
    status: (T) -> String,
    progress: (T) -> Float,
    detail: (T) -> String,
    actions: @Composable RowScope.(T) -> Unit,
) {
    val colors = webReplicaColors()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WebSurfaceCard {
                Text(title, color = colors.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if (items.isEmpty()) emptyText else "共 ${items.size} 项", color = colors.textSecondary)
            }
        }
        items(items) { item ->
            TaskCard(name(item), status(item), progress(item), detail(item)) {
                actions(item)
            }
        }
    }
}

@Composable
private fun TaskCard(
    title: String,
    status: String,
    progress: Float,
    detail: String,
    actions: @Composable RowScope.() -> Unit,
) {
    val colors = webReplicaColors()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceStrong,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, color = colors.text)
                Spacer(Modifier.width(8.dp))
                Text(status, color = colors.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), content = actions)
        }
    }
}
