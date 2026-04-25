// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WebLightBg = Color(0xFFEEF3F7)
private val WebLightSurface = Color.White
private val WebLightText = Color(0xFF13212F)
private val WebLightAccent = Color(0xFF0F766E)
private val WebLightWarning = Color(0xFFE6A23C)
private val WebDarkBg = Color(0xFF08131B)
private val WebDarkSurface = Color(0xFF0D1C26)
private val WebDarkText = Color(0xFFE7F2F7)
private val WebDarkAccent = Color(0xFF3DD3C3)
private val WebDarkOutline = Color(0xFF9EB4C0)

private val LightColors = lightColorScheme(
    primary = WebLightAccent,
    secondary = WebLightWarning,
    tertiary = Color(0xFF409EFF),
    background = WebLightBg,
    surface = WebLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = WebLightText,
    onSurface = WebLightText,
    surfaceVariant = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFF536471),
    outline = Color(0x1F13212F),
)

private val DarkColors = darkColorScheme(
    primary = WebDarkAccent,
    secondary = Color(0xFFFFB17A),
    tertiary = Color(0xFF409EFF),
    background = WebDarkBg,
    surface = WebDarkSurface,
    onPrimary = WebDarkBg,
    onSecondary = WebDarkBg,
    onBackground = WebDarkText,
    onSurface = WebDarkText,
    surfaceVariant = Color(0xFF10232F),
    onSurfaceVariant = WebDarkOutline,
    outline = Color(0x2E94A3B8),
)

@Composable
fun BaiduPcsAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
