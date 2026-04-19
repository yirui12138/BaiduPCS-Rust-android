// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import type {
  BackupEvent,
  CloudDlEvent,
  DownloadEvent,
  FolderEvent,
  TimestampedEvent,
  TransferEvent,
  UploadEvent,
  WsClientMessage,
  WsServerMessage,
} from '@/types/events'

export type ConnectionState = 'disconnected' | 'connecting' | 'connected'

type DownloadEventCallback = (event: DownloadEvent) => void
type FolderEventCallback = (event: FolderEvent) => void
type UploadEventCallback = (event: UploadEvent) => void
type TransferEventCallback = (event: TransferEvent) => void
type BackupEventCallback = (event: BackupEvent) => void
type CloudDlEventCallback = (event: CloudDlEvent) => void
type ConnectionStateCallback = (state: ConnectionState) => void

const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000]
const HEARTBEAT_INTERVAL = 30000
const HEARTBEAT_TIMEOUT = 60000

class WebSocketClient {
  private static instance: WebSocketClient | null = null

  private ws: WebSocket | null = null
  private connectionState: ConnectionState = 'disconnected'
  private reconnectAttempt = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private lastPongTime = 0
  private readonly currentSubscriptions = new Set<string>()

  private readonly downloadListeners = new Set<DownloadEventCallback>()
  private readonly folderListeners = new Set<FolderEventCallback>()
  private readonly uploadListeners = new Set<UploadEventCallback>()
  private readonly transferListeners = new Set<TransferEventCallback>()
  private readonly backupListeners = new Set<BackupEventCallback>()
  private readonly cloudDlListeners = new Set<CloudDlEventCallback>()
  private readonly connectionStateListeners = new Set<ConnectionStateCallback>()

  private constructor() {
    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', this.handleVisibilityChange)
      window.addEventListener('focus', this.handleVisibilityChange)
      window.addEventListener('blur', this.handleVisibilityChange)
      window.addEventListener('online', this.handleVisibilityChange)
    }
  }

  public static getInstance(): WebSocketClient {
    if (!WebSocketClient.instance) {
      WebSocketClient.instance = new WebSocketClient()
    }
    return WebSocketClient.instance
  }

  private readonly handleVisibilityChange = () => {
    this.syncConnectionDemand()
  }

  private getWsUrl(): string {
    const isDev = import.meta.env?.DEV ?? false
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const path = isDev ? '/ws/api/v1/ws' : '/api/v1/ws'
    return `${protocol}//${host}${path}`
  }

  private isDocumentVisible(): boolean {
    if (typeof document === 'undefined') {
      return true
    }
    return document.visibilityState !== 'hidden'
  }

  private hasDemand(): boolean {
    return (
      this.currentSubscriptions.size > 0 ||
      this.downloadListeners.size > 0 ||
      this.folderListeners.size > 0 ||
      this.uploadListeners.size > 0 ||
      this.transferListeners.size > 0 ||
      this.backupListeners.size > 0 ||
      this.cloudDlListeners.size > 0
    )
  }

  private shouldMaintainConnection(): boolean {
    return this.isDocumentVisible() && this.hasDemand()
  }

  private syncConnectionDemand(): void {
    if (this.shouldMaintainConnection()) {
      if (this.connectionState === 'disconnected') {
        this.connect()
      }
      return
    }

    this.disconnect('Connection suspended')
  }

  public connect(): void {
    if (!this.shouldMaintainConnection()) {
      return
    }

    if (this.connectionState !== 'disconnected') {
      return
    }

    this.setConnectionState('connecting')
    this.cancelReconnect()

    try {
      this.ws = new WebSocket(this.getWsUrl())
      this.setupEventHandlers()
    } catch (error) {
      console.error('[WS] 创建连接失败:', error)
      this.scheduleReconnect()
    }
  }

  public disconnect(reason: string = 'Client disconnect'): void {
    this.stopHeartbeat()
    this.cancelReconnect()

    if (this.ws) {
      this.ws.close(1000, reason)
      this.ws = null
    }

    this.setConnectionState('disconnected')
  }

  private setConnectionState(state: ConnectionState): void {
    if (this.connectionState === state) {
      return
    }

    this.connectionState = state
    this.connectionStateListeners.forEach((callback) => callback(state))
  }

  private setupEventHandlers(): void {
    if (!this.ws) {
      return
    }

    this.ws.onopen = () => {
      this.reconnectAttempt = 0
      this.setConnectionState('connected')
      this.startHeartbeat()

      if (this.currentSubscriptions.size > 0) {
        this.send({
          type: 'subscribe',
          subscriptions: Array.from(this.currentSubscriptions),
        })
      }
    }

    this.ws.onclose = (event) => {
      this.stopHeartbeat()
      this.ws = null
      this.setConnectionState('disconnected')

      if (event.code !== 1000 && this.shouldMaintainConnection()) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = (error) => {
      console.error('[WS] 连接异常:', error)
    }

    this.ws.onmessage = (event) => {
      this.handleMessage(event.data)
    }
  }

  private handleMessage(data: string): void {
    try {
      const message = JSON.parse(data) as WsServerMessage

      switch (message.type) {
        case 'connected':
          return
        case 'pong':
          this.lastPongTime = Date.now()
          return
        case 'event':
          this.dispatchEvent(message as TimestampedEvent)
          return
        case 'event_batch':
          message.events.forEach((event) => this.dispatchEvent(event))
          return
        case 'snapshot':
          return
        case 'error':
          console.error('[WS] 服务端返回错误:', message.code, message.message)
          return
        case 'subscribe_success':
        case 'unsubscribe_success':
          return
        default:
          console.warn('[WS] 未知消息类型:', message)
      }
    } catch (error) {
      console.error('[WS] 解析消息失败:', error, data)
    }
  }

  private dispatchEvent(event: TimestampedEvent): void {
    switch (event.category) {
      case 'download':
        this.downloadListeners.forEach((callback) => callback(event.event as DownloadEvent))
        return
      case 'folder':
        this.folderListeners.forEach((callback) => callback(event.event as FolderEvent))
        return
      case 'upload':
        this.uploadListeners.forEach((callback) => callback(event.event as UploadEvent))
        return
      case 'transfer':
        this.transferListeners.forEach((callback) => callback(event.event as TransferEvent))
        return
      case 'backup':
        this.backupListeners.forEach((callback) => callback(event.event as BackupEvent))
        return
      case 'cloud_dl':
        this.cloudDlListeners.forEach((callback) => callback(event.event as CloudDlEvent))
        return
      default:
        console.warn('[WS] 未知事件分类:', event.category)
    }
  }

  private send(message: WsClientMessage): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    }
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.lastPongTime = Date.now()

    this.heartbeatTimer = window.setInterval(() => {
      if (!this.shouldMaintainConnection()) {
        this.disconnect('Background suspend')
        return
      }

      if (Date.now() - this.lastPongTime > HEARTBEAT_TIMEOUT) {
        this.ws?.close(4000, 'Heartbeat timeout')
        return
      }

      this.send({
        type: 'ping',
        timestamp: Date.now(),
      })
    }, HEARTBEAT_INTERVAL)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private scheduleReconnect(): void {
    if (!this.shouldMaintainConnection()) {
      return
    }

    this.cancelReconnect()

    const delay = RECONNECT_DELAYS[Math.min(this.reconnectAttempt, RECONNECT_DELAYS.length - 1)]
    this.reconnectAttempt += 1

    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, delay)
  }

  private cancelReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  public onDownloadEvent(callback: DownloadEventCallback): () => void {
    this.downloadListeners.add(callback)
    this.syncConnectionDemand()
    return () => {
      this.downloadListeners.delete(callback)
      this.syncConnectionDemand()
    }
  }

  public onFolderEvent(callback: FolderEventCallback): () => void {
    this.folderListeners.add(callback)
    this.syncConnectionDemand()
    return () => {
      this.folderListeners.delete(callback)
      this.syncConnectionDemand()
    }
  }

  public onUploadEvent(callback: UploadEventCallback): () => void {
    this.uploadListeners.add(callback)
    this.syncConnectionDemand()
    return () => {
      this.uploadListeners.delete(callback)
      this.syncConnectionDemand()
    }
  }

  public onTransferEvent(callback: TransferEventCallback): () => void {
    this.transferListeners.add(callback)
    this.syncConnectionDemand()
    return () => {
      this.transferListeners.delete(callback)
      this.syncConnectionDemand()
    }
  }

  public onBackupEvent(callback: BackupEventCallback): () => void {
    this.backupListeners.add(callback)
    this.syncConnectionDemand()
    return () => {
      this.backupListeners.delete(callback)
      this.syncConnectionDemand()
    }
  }

  public onCloudDlEvent(callback: CloudDlEventCallback): () => void {
    this.cloudDlListeners.add(callback)
    this.syncConnectionDemand()
    return () => {
      this.cloudDlListeners.delete(callback)
      this.syncConnectionDemand()
    }
  }

  public onConnectionStateChange(callback: ConnectionStateCallback): () => void {
    this.connectionStateListeners.add(callback)
    callback(this.connectionState)
    return () => {
      this.connectionStateListeners.delete(callback)
    }
  }

  public requestSnapshot(): void {
    this.send({ type: 'request_snapshot' })
  }

  public subscribe(subscriptions: string[]): void {
    subscriptions.forEach((subscription) => this.currentSubscriptions.add(subscription))

    if (this.isConnected()) {
      this.send({
        type: 'subscribe',
        subscriptions,
      })
    }

    this.syncConnectionDemand()
  }

  public unsubscribe(subscriptions: string[]): void {
    subscriptions.forEach((subscription) => this.currentSubscriptions.delete(subscription))

    if (this.isConnected()) {
      this.send({
        type: 'unsubscribe',
        subscriptions,
      })
    }

    this.syncConnectionDemand()
  }

  public getSubscriptions(): string[] {
    return Array.from(this.currentSubscriptions)
  }

  public getConnectionState(): ConnectionState {
    return this.connectionState
  }

  public isConnected(): boolean {
    return this.connectionState === 'connected'
  }
}

export function getWebSocketClient(): WebSocketClient {
  return WebSocketClient.getInstance()
}

export function connectWebSocket(): void {
  getWebSocketClient().connect()
}

export function disconnectWebSocket(): void {
  getWebSocketClient().disconnect()
}
