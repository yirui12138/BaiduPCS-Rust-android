// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.baidupcs.android.core.DocumentImporter
import com.baidupcs.android.core.FolderOpenCoordinator
import com.baidupcs.android.core.ImportCleanupManager
import com.baidupcs.android.core.ImportedEntry
import com.baidupcs.android.core.RuntimeKeeperService
import com.baidupcs.android.core.RuntimeSummaryClient
import com.baidupcs.android.core.ServerEnvironment
import com.baidupcs.android.core.VpnStateMonitor
import com.baidupcs.android.nativeui.NativeApp
import com.baidupcs.android.nativeui.NativeCookieLoginResult
import com.baidupcs.android.nativeui.NativePlatformActions
import com.baidupcs.android.ui.BaiduPcsAndroidTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private var currentEnvironment: ServerEnvironment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            BaiduPcsAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainRoute(
                        viewModel = viewModel,
                        onEnvironmentChanged = { currentEnvironment = it },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        RuntimeKeeperService.stop(this)
    }

    override fun onStop() {
        val environment = currentEnvironment
        if (!isChangingConfigurations && environment != null) {
            lifecycleScope.launch {
                val summary = RuntimeSummaryClient.fetch(environment.baseUrl)
                if (summary?.hasActiveWork != false) {
                    RuntimeKeeperService.start(this@MainActivity)
                }
            }
        }

        super.onStop()
    }
}

@Composable
private fun MainRoute(
    viewModel: MainViewModel,
    onEnvironmentChanged: (ServerEnvironment?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is BootUiState.Starting -> {
            SideEffect { onEnvironmentChanged(null) }
            BootScreen(stage = state.stage)
        }
        is BootUiState.Error -> {
            SideEffect { onEnvironmentChanged(null) }
            ErrorScreen(
                message = state.message,
                onRetry = { viewModel.boot(forceRestart = true) },
            )
        }
        is BootUiState.Ready -> {
            SideEffect { onEnvironmentChanged(state.environment) }
            ReadyScreen(environment = state.environment)
        }
    }
}

@Composable
private fun BootScreen(stage: String) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val brush = remember(colorScheme, isDark) {
        Brush.linearGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.primary.copy(alpha = if (isDark) 0.72f else 0.78f),
                colorScheme.secondary.copy(alpha = if (isDark) 0.58f else 0.66f),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(24.dp)
            .testTag("boot_screen"),
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface.copy(alpha = if (isDark) 0.9f else 0.94f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "百度云盘",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = "Rust 本地服务正在启动，原生界面准备中。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.primary,
                    trackColor = colorScheme.primary.copy(alpha = 0.16f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = colorScheme.secondary,
                    )
                    Text(
                        text = stage,
                        modifier = Modifier.testTag("boot_stage"),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp)
            .testTag("boot_error"),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "启动失败",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = null,
                    )
                    Text(
                        text = "重试",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

private data class PendingImportCallbacks(
    val onSuccess: (List<ImportedEntry>) -> Unit,
    val onError: (String) -> Unit,
)

@Composable
private fun ReadyScreen(environment: ServerEnvironment) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folderOpenCoordinator = remember(context) {
        FolderOpenCoordinator(context.applicationContext)
    }
    val vpnStateMonitor = remember(context) {
        VpnStateMonitor(context.applicationContext)
    }

    var pendingFileImport by remember { mutableStateOf<PendingImportCallbacks?>(null) }
    var pendingFolderImport by remember { mutableStateOf<PendingImportCallbacks?>(null) }
    var pendingCookieLogin by remember { mutableStateOf<((NativeCookieLoginResult) -> Unit)?>(null) }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        val callbacks = pendingFileImport ?: return@rememberLauncherForActivityResult
        pendingFileImport = null

        if (uris.isEmpty()) {
            callbacks.onError("未选择文件")
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            runCatching {
                DocumentImporter.importDocuments(context, uris, environment.uploadDir)
            }.onSuccess { entries ->
                if (entries.isEmpty()) {
                    callbacks.onError("没有可导入的文件")
                } else {
                    callbacks.onSuccess(entries)
                }
            }.onFailure { error ->
                callbacks.onError(error.message ?: "导入文件失败")
            }
        }
    }

    val folderImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        val callbacks = pendingFolderImport ?: return@rememberLauncherForActivityResult
        pendingFolderImport = null

        if (uri == null) {
            callbacks.onError("未选择文件夹")
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            runCatching {
                DocumentImporter.importTree(context, uri, environment.uploadDir)
            }.onSuccess { entries ->
                if (entries.isEmpty()) {
                    callbacks.onError("没有可导入的文件夹")
                } else {
                    callbacks.onSuccess(entries)
                }
            }.onFailure { error ->
                callbacks.onError(error.message ?: "导入文件夹失败")
            }
        }
    }

    val cookieLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = pendingCookieLogin ?: return@rememberLauncherForActivityResult
        pendingCookieLogin = null

        val data = result.data
        val status = data?.getStringExtra(BaiduCookieLoginActivity.EXTRA_STATUS)
            ?: if (result.resultCode == Activity.RESULT_OK) {
                BaiduCookieLoginActivity.STATUS_SUCCESS
            } else {
                BaiduCookieLoginActivity.STATUS_CANCELLED
            }

        callback(
            NativeCookieLoginResult(
                status = status,
                cookies = data?.getStringExtra(BaiduCookieLoginActivity.EXTRA_COOKIES),
                reason = data?.getStringExtra(BaiduCookieLoginActivity.EXTRA_REASON),
            ),
        )
    }

    val platform = NativePlatformActions(
        importFiles = { onSuccess, onError ->
            pendingFileImport = PendingImportCallbacks(onSuccess, onError)
            runCatching {
                fileImportLauncher.launch(arrayOf("*/*"))
            }.onFailure { error ->
                pendingFileImport = null
                onError(error.message ?: "无法打开文件选择器")
            }
        },
        importFolder = { onSuccess, onError ->
            pendingFolderImport = PendingImportCallbacks(onSuccess, onError)
            runCatching {
                folderImportLauncher.launch(null)
            }.onFailure { error ->
                pendingFolderImport = null
                onError(error.message ?: "无法打开文件夹选择器")
            }
        },
        openFolder = { path ->
            folderOpenCoordinator.openFolder(path, "native-open-folder")
        },
        cleanupImportedPaths = { paths ->
            scope.launch {
                ImportCleanupManager.cleanupImportedPaths(paths, environment.uploadDir)
            }
        },
        cleanupStaleImports = {
            scope.launch {
                ImportCleanupManager.cleanupStaleImports(environment.uploadDir)
            }
        },
        readClipboardText = {
            readClipboardText(context)
        },
        isVpnActive = {
            vpnStateMonitor.isVpnActive()
        },
        startCookieLogin = { onResult ->
            pendingCookieLogin = onResult
            runCatching {
                cookieLoginLauncher.launch(Intent(context, BaiduCookieLoginActivity::class.java))
            }.onFailure { error ->
                pendingCookieLogin = null
                onResult(
                    NativeCookieLoginResult(
                        status = BaiduCookieLoginActivity.STATUS_FAILED,
                        cookies = null,
                        reason = error.message ?: "无法打开网页登录",
                    ),
                )
            }
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("native_app"),
    ) {
        NativeApp(
            environment = environment,
            platform = platform,
        )
    }
}

private fun readClipboardText(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clipData = clipboard?.primaryClip ?: return ""
    if (clipData.itemCount <= 0) return ""
    return clipData.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
}
