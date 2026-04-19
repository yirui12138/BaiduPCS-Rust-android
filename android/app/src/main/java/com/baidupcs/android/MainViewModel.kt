// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baidupcs.android.core.ServerBootstrapper
import com.baidupcs.android.core.ServerEnvironment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BootUiState {
    data class Starting(val stage: String) : BootUiState
    data class Ready(val environment: ServerEnvironment) : BootUiState
    data class Error(val message: String) : BootUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val bootstrapper = ServerBootstrapper(application)
    private val _uiState = MutableStateFlow<BootUiState>(
        BootUiState.Starting("准备运行环境"),
    )
    val uiState: StateFlow<BootUiState> = _uiState.asStateFlow()

    init {
        boot()
    }

    fun boot(forceRestart: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = BootUiState.Starting("准备运行环境")
            val result = bootstrapper.ensureStarted(forceRestart) { stage ->
                _uiState.value = BootUiState.Starting(stage)
            }

            _uiState.value = result.fold(
                onSuccess = { BootUiState.Ready(it) },
                onFailure = {
                    BootUiState.Error(it.message ?: "启动失败，请稍后重试")
                },
            )
        }
    }
}
