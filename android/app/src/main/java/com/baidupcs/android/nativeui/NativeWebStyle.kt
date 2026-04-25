// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.nativeui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class WebReplicaColors(
    val background: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceMuted: Color,
    val surfaceOverlay: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val drawerStart: Color = Color(0xFF11202D),
    val drawerEnd: Color = Color(0xFF162A39),
    val folder: Color = Color(0xFFE6A23C),
    val danger: Color = Color(0xFFF56C6C),
    val info: Color = Color(0xFF909399),
)

internal object WebReplicaDimens {
    val HeaderHeight = 48.dp
    val TabBarHeight = 60.dp
    val TouchTarget = 44.dp
    val CardRadius = 18.dp
    val ButtonRadius = 999.dp
    val PagePadding = 12.dp
}

@Composable
internal fun webReplicaColors(): WebReplicaColors =
    if (isSystemInDarkTheme()) {
        WebReplicaColors(
            background = Color(0xFF08131B),
            surface = Color(0xEB0B1822),
            surfaceStrong = Color(0xFF0D1C26),
            surfaceMuted = Color(0xFF10232F),
            surfaceOverlay = Color(0xF00D1C26),
            text = Color(0xFFE7F2F7),
            textSecondary = Color(0xFF9EB4C0),
            border = Color(0x2E94A3B8),
            accent = Color(0xFF3DD3C3),
            accentSoft = Color(0x2E3DD3C3),
        )
    } else {
        WebReplicaColors(
            background = Color(0xFFEEF3F7),
            surface = Color.White.copy(alpha = 0.88f),
            surfaceStrong = Color.White,
            surfaceMuted = Color(0xFFF5F7FA),
            surfaceOverlay = Color.White.copy(alpha = 0.92f),
            text = Color(0xFF13212F),
            textSecondary = Color(0xFF536471),
            border = Color(0x141E293B),
            accent = Color(0xFF0F766E),
            accentSoft = Color(0x1F0F766E),
        )
    }

@Composable
internal fun WebReplicaBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = webReplicaColors()
    Box(
        modifier = modifier.background(colors.background),
    ) {
        content()
    }
}

@Composable
internal fun WebSurfaceCard(
    modifier: Modifier = Modifier,
    radius: Dp = WebReplicaDimens.CardRadius,
    color: Color = webReplicaColors().surface,
    borderColor: Color = webReplicaColors().border,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), shape),
        shape = shape,
        color = color,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
internal fun WebRoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    size: Dp = 36.dp,
    onClick: () -> Unit,
) {
    val colors = webReplicaColors()
    val container = when {
        danger -> colors.danger.copy(alpha = if (isSystemInDarkTheme()) 0.22f else 0.12f)
        primary -> Color(0xFF409EFF)
        else -> colors.surfaceOverlay
    }
    val tint = when {
        danger -> colors.danger
        primary -> Color.White
        else -> colors.textSecondary
    }
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = container,
        border = if (primary) null else BorderStroke(1.dp, colors.border),
        shadowElevation = if (primary) 3.dp else 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun WebPillButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    detected: Boolean = false,
    expanded: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = webReplicaColors()
    val targetColor = when {
        detected -> colors.accentSoft
        expanded -> colors.accentSoft
        else -> colors.surfaceOverlay
    }
    val container by animateColorAsState(targetColor, tween(180), label = "web-pill-container")
    Surface(
        modifier = modifier
            .heightIn(min = 36.dp)
            .animateContentSize(tween(180))
            .clip(RoundedCornerShape(WebReplicaDimens.ButtonRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(WebReplicaDimens.ButtonRadius),
        color = container,
        border = BorderStroke(1.dp, if (detected || expanded) colors.accent.copy(alpha = 0.55f) else colors.border),
        shadowElevation = if (detected || expanded) 4.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, tint = if (detected || expanded) colors.accent else colors.text, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (detected || expanded) colors.accent else colors.text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun WebTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val colors = webReplicaColors()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedContainerColor = colors.surfaceStrong,
            unfocusedContainerColor = colors.surfaceStrong,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedLabelColor = colors.accent,
            unfocusedLabelColor = colors.textSecondary,
        ),
    )
}

@Composable
internal fun WebSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    val colors = webReplicaColors()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = colors.text, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        helper?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun WebSettingSection(
    title: String,
    description: String,
    color: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = webReplicaColors()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val triggerShape = RoundedCornerShape(18.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(triggerShape)
                .clickable(onClick = onToggle),
            color = if (expanded) color.copy(alpha = if (isSystemInDarkTheme()) 0.16f else 0.08f) else colors.surface,
            shape = triggerShape,
            border = BorderStroke(1.dp, if (expanded) color.copy(alpha = 0.48f) else colors.border),
            shadowElevation = if (expanded) 4.dp else 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = color.copy(alpha = if (isSystemInDarkTheme()) 0.20f else 0.14f),
                ) {
                    Box(contentAlignment = Alignment.Center) { icon() }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = colors.text, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(description, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (expanded) "收起" else "展开", color = if (expanded) color else colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160)) + expandVertically(animationSpec = tween(220)),
            exit = fadeOut(tween(120)) + shrinkVertically(animationSpec = tween(180)),
        ) {
            WebSurfaceCard(
                color = colors.surface,
                borderColor = colors.border,
                content = content,
            )
        }
    }
}

@Composable
internal fun WebDrawerBackground(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = webReplicaColors()
    Column(
        modifier = modifier.background(Brush.verticalGradient(listOf(colors.drawerStart, colors.drawerEnd))),
        content = content,
    )
}

@Composable
internal fun WebToolbarRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = webReplicaColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
