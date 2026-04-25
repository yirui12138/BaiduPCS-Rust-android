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
import org.json.JSONObject
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appBootsIntoNativeShell() {
        waitForNativeShell()

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
            composeRule.onAllNodesWithTag("main_webview")
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun businessShellDoesNotAttachWebView() {
        waitForNativeShell()

        assertNull(findWebView(composeRule.activity.window.decorView.rootView))
        assertTrue(
            composeRule.onAllNodesWithTag("native_app")
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun waitForNativeShell() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithTag("boot_stage").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("native_app").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 45_000) {
            composeRule.onAllNodesWithTag("native_app")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

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
