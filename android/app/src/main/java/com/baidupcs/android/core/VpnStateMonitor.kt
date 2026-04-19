// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import java.io.Closeable

class VpnStateMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager: ConnectivityManager? =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun isVpnActive(): Boolean =
        runCatching {
            val manager = connectivityManager ?: return@runCatching false
            val activeNetwork = manager.activeNetwork
            if (activeNetwork != null && manager.isVpnNetwork(activeNetwork)) {
                return@runCatching true
            }

            manager.allNetworks.any { network -> manager.isVpnNetwork(network) }
        }.getOrElse { error ->
            Log.w(TAG, "Failed to query VPN state", error)
            false
        }

    fun register(onStatusChanged: (Boolean) -> Unit): Closeable {
        val manager = connectivityManager ?: return Closeable { }
        var lastState: Boolean? = null

        fun publishIfChanged() {
            val current = isVpnActive()
            if (lastState == current) return
            lastState = current
            onStatusChanged(current)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                publishIfChanged()
            }

            override fun onLost(network: Network) {
                publishIfChanged()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                publishIfChanged()
            }
        }

        return runCatching {
            manager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
            publishIfChanged()
            Closeable {
                runCatching { manager.unregisterNetworkCallback(callback) }
                    .onFailure { Log.w(TAG, "Failed to unregister VPN state callback", it) }
            }
        }.getOrElse { error ->
            Log.w(TAG, "Failed to register VPN state callback", error)
            publishIfChanged()
            Closeable { }
        }
    }

    private fun ConnectivityManager.isVpnNetwork(network: Network): Boolean =
        getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

    companion object {
        private const val TAG = "VpnStateMonitor"
    }
}
