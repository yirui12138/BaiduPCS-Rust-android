// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class BaiduCookieLoginActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.textZoom = 92
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.userAgentString =
                "${settings.userAgentString} BaiduPCSAndroidCookieLogin/${BuildConfig.VERSION_NAME}"
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            setInitialScale(1)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar.progress = newProgress
                    progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                    if (newProgress in 1..99) {
                        updateStatus("正在打开百度网盘登录页...")
                    } else if (!completed) {
                        updateStatus("请在页面中完成百度网盘登录，完成后会自动返回应用")
                    }
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    updateStatus("正在打开百度网盘登录页...")
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val target = request?.url ?: return false
                    if (target.isBaiduHost()) return false

                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, target))
                    }
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    tryCompleteWithCookies()
                }
            }
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.displayMetrics.density.toInt().coerceAtLeast(2),
            )
            max = 100
            visibility = View.GONE
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.rgb(248, 250, 252))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val title = TextView(this).apply {
            text = "百度网盘网页登录"
            textSize = 16f
            setTextColor(Color.rgb(30, 41, 59))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val cancel = Button(this).apply {
            text = "取消"
            setOnClickListener { finishWithStatus(STATUS_CANCELLED) }
        }

        toolbar.addView(title)
        toolbar.addView(cancel)

        statusText = TextView(this).apply {
            text = "正在打开百度网盘登录页..."
            textSize = 13f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(Color.rgb(241, 245, 249))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            addView(toolbar)
            addView(progressBar)
            addView(statusText)
            addView(webView)
        }

        setContentView(root)
        webView.loadUrl(BAIDU_MOBILE_LOGIN_URL)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            finishWithStatus(STATUS_CANCELLED)
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun tryCompleteWithCookies() {
        if (completed) return

        val cookies = collectBaiduCookies()
        if (!cookies.contains("BDUSS=")) return

        CookieManager.getInstance().flush()
        completed = true
        updateStatus("登录完成，正在导入 Cookie...")
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_STATUS, STATUS_SUCCESS)
                .putExtra(EXTRA_COOKIES, cookies),
        )
        finish()
    }

    private fun collectBaiduCookies(): String {
        val merged = linkedMapOf<String, String>()
        COOKIE_URLS
            .mapNotNull { CookieManager.getInstance().getCookie(it) }
            .flatMap { it.split(';') }
            .map { it.trim() }
            .filter { it.contains('=') }
            .forEach { pair ->
                val name = pair.substringBefore('=').trim()
                if (name.isNotBlank()) {
                    merged[name] = pair
                }
            }
        return merged.values.joinToString("; ")
    }

    private fun finishWithStatus(status: String, reason: String? = null) {
        if (completed) return
        completed = true
        setResult(
            RESULT_CANCELED,
            Intent()
                .putExtra(EXTRA_STATUS, status)
                .putExtra(EXTRA_REASON, reason),
        )
        finish()
    }

    private fun updateStatus(message: String) {
        if (::statusText.isInitialized) {
            statusText.text = message
        }
    }

    private fun Uri.isBaiduHost(): Boolean {
        val host = host.orEmpty().lowercase()
        return host == "baidu.com" ||
            host.endsWith(".baidu.com") ||
            host == "baidu.cn" ||
            host.endsWith(".baidu.cn")
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_STATUS = "status"
        const val EXTRA_COOKIES = "cookies"
        const val EXTRA_REASON = "reason"

        const val STATUS_SUCCESS = "success"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_FAILED = "failed"

        private const val BAIDU_MOBILE_LOGIN_URL =
            "https://wappass.baidu.com/passport/?login&tpl=netdisk&u=https%3A%2F%2Fpan.baidu.com%2Fwap%2Fhome"

        private val COOKIE_URLS = listOf(
            "https://pan.baidu.com",
            "https://wappass.baidu.com",
            "https://www.baidu.com",
            "https://passport.baidu.com",
            "https://baidu.com",
            "https://yun.baidu.com",
        )
    }
}
