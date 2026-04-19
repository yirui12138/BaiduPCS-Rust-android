// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android

import android.app.Activity
import android.content.Context
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.baidupcs.android.core.DocumentImporter
import com.baidupcs.android.core.FolderOpenCoordinator
import com.baidupcs.android.core.FolderOpenResult
import com.baidupcs.android.core.ImportCleanupManager
import com.baidupcs.android.core.ImportedEntry
import com.baidupcs.android.core.RuntimeKeeperService
import com.baidupcs.android.core.RuntimeSummaryClient
import com.baidupcs.android.core.ServerEnvironment
import com.baidupcs.android.core.VpnStateMonitor
import com.baidupcs.android.ui.BaiduPcsAndroidTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private var activeWebView: WebView? = null
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
                        onWebViewChanged = { activeWebView = it },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        RuntimeKeeperService.stop(this)
    }

    override fun onResume() {
        super.onResume()
        activeWebView?.onResume()
        activeWebView?.resumeTimers()
        dispatchAndroidAppForeground(activeWebView)
    }

    override fun onStop() {
        activeWebView?.onPause()
        activeWebView?.pauseTimers()

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
    onWebViewChanged: (WebView?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is BootUiState.Starting -> {
            SideEffect {
                onEnvironmentChanged(null)
                onWebViewChanged(null)
            }
            BootScreen(stage = state.stage)
        }
        is BootUiState.Error -> {
            SideEffect {
                onEnvironmentChanged(null)
                onWebViewChanged(null)
            }
            ErrorScreen(
                message = state.message,
                onRetry = { viewModel.boot(forceRestart = true) },
            )
        }
        is BootUiState.Ready -> {
            SideEffect {
                onEnvironmentChanged(state.environment)
            }
            ReadyScreen(
                environment = state.environment,
                onWebViewChanged = onWebViewChanged,
            )
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
                    text = "柏渡云盘",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = "Rust 本地服务 + Web 前端正在装载，整个流程完全在设备本机完成。",
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
    val isDark = isSystemInDarkTheme()
    val brush = remember(colorScheme, isDark) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.7f else 0.9f),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(24.dp)
            .testTag("error_screen"),
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "启动未完成",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRetry) {
                    Icon(Icons.Rounded.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("重新启动")
                }
            }
        }
    }
}

@Composable
private fun ReadyScreen(
    environment: ServerEnvironment,
    onWebViewChanged: (WebView?) -> Unit,
) {
    val context = LocalContext.current
    val vpnStateMonitor = remember(context) { VpnStateMonitor(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pageLoadProgress by remember { mutableIntStateOf(0) }
    val initialEntryUrl = remember(environment.baseUrl) {
        buildInitialEntryUrl(environment.baseUrl)
    }
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                DocumentImporter.importDocuments(context, uris, environment.uploadDir)
            }.onSuccess { entries ->
                dispatchAndroidImportComplete(
                    webView = webViewRef,
                    sourceType = "file",
                    entries = entries,
                )
                snackbarHostState.showSnackbar("已导入 ${entries.size} 个项目，请确认上传")
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "文件导入失败")
            }
        }
    }

    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                DocumentImporter.importTree(context, uri, environment.uploadDir)
            }.onSuccess { entries ->
                dispatchAndroidImportComplete(
                    webView = webViewRef,
                    sourceType = "directory",
                    entries = entries,
                )
                snackbarHostState.showSnackbar("已导入 ${entries.size} 个项目，请确认上传")
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "目录导入失败")
            }
        }
    }

    val cookieLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        val status = data?.getStringExtra(BaiduCookieLoginActivity.EXTRA_STATUS)
            ?: if (result.resultCode == Activity.RESULT_OK) {
                BaiduCookieLoginActivity.STATUS_FAILED
            } else {
                BaiduCookieLoginActivity.STATUS_CANCELLED
            }
        val cookies = data?.getStringExtra(BaiduCookieLoginActivity.EXTRA_COOKIES)
        val reason = data?.getStringExtra(BaiduCookieLoginActivity.EXTRA_REASON)
        dispatchAndroidCookieLoginResult(webViewRef, status, cookies, reason)
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    val shellBrush = remember(colorScheme, isDark) {
        Brush.linearGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.52f else 0.82f),
                colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.08f),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(shellBrush),
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface.copy(alpha = if (isDark) 0.96f else 0.98f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 6.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_webview"),
                    factory = { viewContext ->
                        WebView(viewContext).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.offscreenPreRaster = false
                            settings.userAgentString =
                                "${settings.userAgentString} BaiduPCSAndroid/${BuildConfig.VERSION_NAME}"

                            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                            }

                            addJavascriptInterface(
                                AndroidShellBridge(
                                    context = viewContext,
                                    onImportFilesRequest = { importFilesLauncher.launch(arrayOf("*/*")) },
                                    onImportFolderRequest = { importFolderLauncher.launch(null) },
                                    onOpenFolderResult = { result ->
                                        dispatchAndroidOpenFolderResult(webViewRef, result)
                                    },
                                    onCleanupImportedPathsRequest = { paths ->
                                        scope.launch {
                                            ImportCleanupManager.cleanupImportedPaths(paths, environment.uploadDir)
                                        }
                                    },
                                    onCleanupStaleImportsRequest = {
                                        scope.launch {
                                            ImportCleanupManager.cleanupStaleImports(environment.uploadDir)
                                        }
                                    },
                                    clipboardTextProvider = { readClipboardText(viewContext) },
                                    isVpnActiveProvider = { vpnStateMonitor.isVpnActive() },
                                    onBaiduCookieLoginRequest = {
                                        cookieLoginLauncher.launch(
                                            Intent(context, BaiduCookieLoginActivity::class.java),
                                        )
                                    },
                                ),
                                "BaiduPCSAndroid",
                            )

                            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageLoadProgress = newProgress
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val target = request?.url ?: return false
                                    val isLocal = target.host == "127.0.0.1" || target.host == "localhost"
                                    if (isLocal) return false
                                    context.startActivity(Intent(Intent.ACTION_VIEW, target))
                                    return true
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    pageLoadProgress = 0
                                    canGoBack = view?.canGoBack() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    canGoBack = view?.canGoBack() == true
                                    pageLoadProgress = 100
                                    dispatchAndroidVpnStatus(view, vpnStateMonitor.isVpnActive())
                                    dispatchAndroidAppForeground(view)
                                }

                                override fun doUpdateVisitedHistory(
                                    view: WebView?,
                                    url: String?,
                                    isReload: Boolean,
                                ) {
                                    canGoBack = view?.canGoBack() == true
                                }
                            }

                            loadUrl(initialEntryUrl)
                        }.also {
                            webViewRef = it
                            onWebViewChanged(it)
                        }
                    },
                    update = { webView ->
                        if (webView.url == null) {
                            webView.loadUrl(initialEntryUrl)
                        }
                    },
                )

                if (pageLoadProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageLoadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = colorScheme.primary,
                        trackColor = colorScheme.primary.copy(alpha = 0.16f),
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp),
        )
    }

    DisposableEffect(webViewRef, vpnStateMonitor) {
        val webView = webViewRef
        if (webView == null) {
            onDispose { }
        } else {
            val registration = vpnStateMonitor.register { active ->
                dispatchAndroidVpnStatus(webView, active)
            }
            onDispose { registration.close() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onWebViewChanged(null)
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}

private fun buildInitialEntryUrl(baseUrl: String): String =
    Uri.parse(baseUrl)
        .buildUpon()
        .appendQueryParameter("shellLaunchNonce", System.currentTimeMillis().toString())
        .appendQueryParameter("shellBuild", BuildConfig.VERSION_NAME)
        .build()
        .toString()

private fun dispatchAndroidImportComplete(
    webView: WebView?,
    sourceType: String,
    entries: List<ImportedEntry>,
) {
    if (webView == null || entries.isEmpty()) return

    val payload = JSONObject().apply {
        put("sourceType", sourceType)
        put("count", entries.size)
        put(
            "entries",
            JSONArray().apply {
                entries.forEach { entry ->
                    put(
                        JSONObject().apply {
                            put("name", entry.name)
                            put("path", entry.path)
                            put("entryType", entry.entryType)
                        },
                    )
                }
            },
        )
    }

    val script = """
        window.dispatchEvent(
          new CustomEvent('android-import-complete', { detail: ${payload} })
        );
    """.trimIndent()

    webView.post {
        webView.evaluateJavascript(script, null)
    }
}

private fun dispatchAndroidOpenFolderResult(
    webView: WebView?,
    result: FolderOpenResult,
) {
    if (webView == null || result.requestId.isBlank()) return

    val payload = JSONObject().apply {
        put("requestId", result.requestId)
        put("status", result.status)
        put("path", result.path)
        result.reason?.let { put("reason", it) }
    }

    val script = """
        window.dispatchEvent(
          new CustomEvent('android-open-folder-result', { detail: ${payload} })
        );
    """.trimIndent()

    webView.post {
        webView.evaluateJavascript(script, null)
    }
}

private fun dispatchAndroidVpnStatus(
    webView: WebView?,
    active: Boolean,
) {
    if (webView == null) return

    val payload = JSONObject().apply {
        put("active", active)
    }

    val script = """
        window.dispatchEvent(
          new CustomEvent('android-vpn-status', { detail: ${payload} })
        );
    """.trimIndent()

    webView.post {
        webView.evaluateJavascript(script, null)
    }
}

private fun dispatchAndroidAppForeground(webView: WebView?) {
    if (webView == null) return

    val payload = JSONObject().apply {
        put("timestamp", System.currentTimeMillis())
    }

    val script = """
        window.dispatchEvent(
          new CustomEvent('android-app-foreground', { detail: ${payload} })
        );
    """.trimIndent()

    webView.post {
        webView.evaluateJavascript(script, null)
    }
}

private fun dispatchAndroidCookieLoginResult(
    webView: WebView?,
    status: String,
    cookies: String?,
    reason: String?,
) {
    if (webView == null) return

    val payload = JSONObject().apply {
        put("status", status)
        cookies?.takeIf { it.isNotBlank() }?.let { put("cookies", it) }
        reason?.takeIf { it.isNotBlank() }?.let { put("reason", it) }
    }

    val script = """
        window.dispatchEvent(
          new CustomEvent('android-cookie-login-result', { detail: ${payload} })
        );
    """.trimIndent()

    webView.post {
        webView.evaluateJavascript(script, null)
    }
}

private fun readClipboardText(context: Context): String =
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return@runCatching ""
        val item = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
            ?: return@runCatching ""
        item.coerceToText(context)?.toString().orEmpty()
    }.getOrDefault("")

private class AndroidShellBridge(
    private val context: Context,
    private val onImportFilesRequest: () -> Unit,
    private val onImportFolderRequest: () -> Unit,
    private val onOpenFolderResult: (FolderOpenResult) -> Unit,
    private val onCleanupImportedPathsRequest: (List<String>) -> Unit,
    private val onCleanupStaleImportsRequest: () -> Unit,
    private val clipboardTextProvider: () -> String,
    private val isVpnActiveProvider: () -> Boolean,
    private val onBaiduCookieLoginRequest: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val folderOpenCoordinator = FolderOpenCoordinator(context)

    @JavascriptInterface
    fun openFolder(path: String?, requestId: String?): Boolean =
        requestId
            ?.takeIf { it.isNotBlank() }
            ?.let { safeRequestId ->
                runCatching {
                    mainHandler.post {
                        onOpenFolderResult(folderOpenCoordinator.openFolder(path, safeRequestId))
                    }
                    true
                }.getOrDefault(false)
            }
            ?: false

    @JavascriptInterface
    fun importFiles(): Boolean =
        runCatching {
            mainHandler.post(onImportFilesRequest)
            true
        }.getOrDefault(false)

    @JavascriptInterface
    fun importFolder(): Boolean =
        runCatching {
            mainHandler.post(onImportFolderRequest)
            true
        }.getOrDefault(false)

    @JavascriptInterface
    fun cleanupImportedPaths(pathsJson: String?): Boolean =
        runCatching {
            val paths = parsePathArray(pathsJson)
            mainHandler.post { onCleanupImportedPathsRequest(paths) }
            true
        }.getOrDefault(false)

    @JavascriptInterface
    fun cleanupStaleImports(): Boolean =
        runCatching {
            mainHandler.post(onCleanupStaleImportsRequest)
            true
        }.getOrDefault(false)

    @JavascriptInterface
    fun readClipboardText(): String =
        runCatching { clipboardTextProvider() }.getOrDefault("")

    @JavascriptInterface
    fun isVpnActive(): Boolean =
        runCatching { isVpnActiveProvider() }.getOrDefault(false)

    @JavascriptInterface
    fun startBaiduCookieLogin(): Boolean =
        runCatching {
            mainHandler.post(onBaiduCookieLoginRequest)
            true
        }.getOrDefault(false)

    private fun parsePathArray(pathsJson: String?): List<String> {
        if (pathsJson.isNullOrBlank()) return emptyList()

        val array = JSONArray(pathsJson)
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }
}
