// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appBootsIntoReadyShell() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithTag("boot_stage").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("main_webview").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 45_000) {
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty(),
        )

        composeRule.waitUntil(timeoutMillis = 45_000) {
            readJson("http://127.0.0.1:18888/health")?.optString("status") == "ok"
        }

        val config = waitForConfig()
        val downloadDir = config
            .getJSONObject("data")
            .getJSONObject("download")
            .getString("download_dir")
        assertTrue(downloadDir.endsWith("/Download/BaiduPCS"))

        assertTrue(
            composeRule.onAllNodesWithTag("quick_import_panel")
                .fetchSemanticsNodes().isEmpty(),
        )

        val webView = waitForWebView()

        composeRule.waitUntil(timeoutMillis = 45_000) {
            readLoginMetrics(webView)?.let { metrics ->
                metrics.optString("path") == "/login" && metrics.optBoolean("qrPresent")
            } == true
        }

        val metrics = readLoginMetrics(webView)
            ?: throw AssertionError("failed to read login metrics from webview")

        assertEquals("/login", metrics.getString("path"))
        assertNotEquals(buildDebugMessage(metrics), "hidden", metrics.getString("appOverflowY"))
        assertTrue(buildDebugMessage(metrics), metrics.getDouble("qrTop") >= 0.0)
        assertTrue(buildDebugMessage(metrics), metrics.getDouble("qrBottom") <= metrics.getDouble("viewportHeight"))
    }

    @Test
    fun invalidFolderOpenRequestReportsFailureWithoutCrashing() {
        composeRule.waitUntil(timeoutMillis = 45_000) {
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val webView = waitForWebView()
        val requestId = "invalid-folder-open-test"
        val missingPath = "/storage/emulated/0/Download/BaiduPCS/__missing_folder_for_open_test__"

        val acceptedRaw = evaluateJavascript(
            webView,
            """
                (() => {
                  window.__codexOpenFolderResult = null;
                  const requestId = '$requestId';
                  const handler = (event) => {
                    const detail = event.detail || {};
                    if (detail.requestId === requestId) {
                      window.__codexOpenFolderResult = JSON.stringify(detail);
                      window.removeEventListener('android-open-folder-result', handler);
                    }
                  };
                  window.addEventListener('android-open-folder-result', handler);
                  if (!window.BaiduPCSAndroid || typeof window.BaiduPCSAndroid.openFolder !== 'function') {
                    return false;
                  }
                  return Boolean(window.BaiduPCSAndroid.openFolder('$missingPath', requestId));
                })();
            """.trimIndent(),
        )

        assertEquals(true, decodeJsValue(acceptedRaw ?: "false"))

        composeRule.waitUntil(timeoutMillis = 10_000) {
            readOpenFolderResult(webView)?.optString("status") == "failed"
        }

        val result = readOpenFolderResult(webView)
            ?: throw AssertionError("failed to receive folder open result")

        assertEquals(requestId, result.getString("requestId"))
        assertEquals("failed", result.getString("status"))
        assertTrue(
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun vpnStateBridgeReturnsBooleanWithoutCrashing() {
        composeRule.waitUntil(timeoutMillis = 45_000) {
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val webView = waitForWebView()
        val resultRaw = evaluateJavascript(
            webView,
            """
                (() => {
                  if (!window.BaiduPCSAndroid || typeof window.BaiduPCSAndroid.isVpnActive !== 'function') {
                    return false;
                  }
                  const active = window.BaiduPCSAndroid.isVpnActive();
                  window.dispatchEvent(new CustomEvent('android-vpn-status', { detail: { active } }));
                  return typeof active === 'boolean';
                })();
            """.trimIndent(),
        )

        assertEquals(true, decodeJsValue(resultRaw ?: "false"))
        assertTrue(
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun importCleanupBridgeAcceptsSafeRequestsWithoutCrashing() {
        composeRule.waitUntil(timeoutMillis = 45_000) {
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val webView = waitForWebView()
        val resultRaw = evaluateJavascript(
            webView,
            """
                (() => {
                  if (!window.BaiduPCSAndroid) return false;
                  const hasCleanup = typeof window.BaiduPCSAndroid.cleanupImportedPaths === 'function';
                  const hasStaleCleanup = typeof window.BaiduPCSAndroid.cleanupStaleImports === 'function';
                  return hasCleanup &&
                    hasStaleCleanup &&
                    window.BaiduPCSAndroid.cleanupImportedPaths('[]') === true &&
                    window.BaiduPCSAndroid.cleanupStaleImports() === true;
                })();
            """.trimIndent(),
        )

        assertEquals(true, decodeJsValue(resultRaw ?: "false"))
        assertTrue(
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun waitForWebView(): WebView {
        var found: WebView? = null
        composeRule.waitUntil(timeoutMillis = 45_000) {
            found = findWebView(composeRule.activity.window.decorView.rootView)
            found != null
        }
        return found ?: throw AssertionError("webview was not attached")
    }

    private fun readLoginMetrics(webView: WebView): JSONObject? {
        val script = """
            (() => {
              const qr = document.querySelector('.qrcode-image');
              const tips = document.querySelector('.tips-card');
              const app = document.getElementById('app');
              const container = document.querySelector('.login-container');
              return JSON.stringify({
                path: location.pathname,
                qrPresent: Boolean(qr),
                qrTop: qr ? qr.getBoundingClientRect().top : -1,
                qrBottom: qr ? qr.getBoundingClientRect().bottom : -1,
                viewportHeight: window.innerHeight,
                viewportWidth: window.innerWidth,
                screenWidth: window.screen.width,
                tipsPosition: tips ? getComputedStyle(tips).position : '',
                appOverflowY: app ? getComputedStyle(app).overflowY : '',
                containerClass: container ? container.className : '',
                userAgent: navigator.userAgent,
              });
            })();
        """.trimIndent()

        val rawResult = evaluateJavascript(webView, script) ?: return null
        val jsonString = decodeJsValue(rawResult) as? String ?: return null
        return JSONObject(jsonString)
    }

    private fun readOpenFolderResult(webView: WebView): JSONObject? {
        val rawResult = evaluateJavascript(
            webView,
            "(() => window.__codexOpenFolderResult || null)();",
        ) ?: return null
        val jsonString = decodeJsValue(rawResult) as? String ?: return null
        return JSONObject(jsonString)
    }

    private fun evaluateJavascript(webView: WebView, script: String): String? {
        val latch = CountDownLatch(1)
        var result: String? = null
        composeRule.activity.runOnUiThread {
            webView.evaluateJavascript(script) {
                result = it
                latch.countDown()
            }
        }
        assertTrue("timed out waiting for javascript result", latch.await(10, TimeUnit.SECONDS))
        return result
    }

    private fun decodeJsValue(rawResult: String): Any? =
        JSONTokener(rawResult).nextValue()

    private fun waitForConfig(): JSONObject {
        var config: JSONObject? = null
        composeRule.waitUntil(timeoutMillis = 45_000) {
            config = readJson("http://127.0.0.1:18888/api/v1/config")
            config != null
        }
        return config ?: throw AssertionError("failed to read runtime config")
    }

    private fun readJson(url: String): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2_000
            readTimeout = 2_000
            requestMethod = "GET"
        }

        return runCatching {
            connection.inputStream.bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
        }.getOrNull().also {
            connection.disconnect()
        }
    }

    private fun buildDebugMessage(metrics: JSONObject): String =
        "path=${metrics.optString("path")} containerClass=${metrics.optString("containerClass")} " +
            "tipsPosition=${metrics.optString("tipsPosition")} viewport=${metrics.optDouble("viewportWidth")}x" +
            "${metrics.optDouble("viewportHeight")} screenWidth=${metrics.optDouble("screenWidth")} " +
            "qrTop=${metrics.optDouble("qrTop")} qrBottom=${metrics.optDouble("qrBottom")} " +
            "appOverflowY=${metrics.optString("appOverflowY")} ua=${metrics.optString("userAgent")}"

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findWebView(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
