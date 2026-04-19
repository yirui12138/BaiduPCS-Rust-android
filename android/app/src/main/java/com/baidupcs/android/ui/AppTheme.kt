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

private val Sand = Color(0xFFF4EFE5)
private val Ink = Color(0xFF10212B)
private val Foam = Color(0xFFE6FFF8)
private val Lagoon = Color(0xFF0D8E73)
private val Ember = Color(0xFFB45A2A)
private val Night = Color(0xFF08131A)
private val Slate = Color(0xFF98AFB7)

private val LightColors = lightColorScheme(
    primary = Lagoon,
    secondary = Ember,
    tertiary = Color(0xFF187B99),
    background = Sand,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Ink,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF32C9AA),
    secondary = Color(0xFFFFA36F),
    tertiary = Color(0xFF65D7FF),
    background = Night,
    surface = Color(0xFF10202A),
    onPrimary = Night,
    onSecondary = Night,
    onBackground = Foam,
    onSurface = Foam,
    outline = Slate,
)

@Composable
fun BaiduPcsAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
