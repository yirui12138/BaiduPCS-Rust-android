// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

export interface ParsedShareLink {
  shareUrl: string
  password?: string
  source: 'manual' | 'clipboard'
}

const SHARE_URL_PATTERN = /(?:https?:\/\/)?(?:pan\.baidu\.com|yun\.baidu\.com)\/(?:s\/[A-Za-z0-9_-]+|share\/init\?surl=[A-Za-z0-9_-]+)(?:[^\s，。；;]*)?/i
const PASSWORD_PATTERNS = [
  /(?:提取码|提取碼|访问码|密[码碼]|pwd|code)[：:\s=]+([A-Za-z0-9]{4})/i,
  /[?&]pwd=([A-Za-z0-9]{4})/i,
]

export function parseBaiduShareText(
  text: string | null | undefined,
  source: ParsedShareLink['source'] = 'manual',
): ParsedShareLink | null {
  const normalized = (text || '').trim()
  if (!normalized) return null

  const urlMatch = normalized.match(SHARE_URL_PATTERN)
  if (!urlMatch) return null

  const shareUrl = normalizeShareUrl(urlMatch[0])
  const password = extractSharePassword(normalized)
  return {
    shareUrl,
    password,
    source,
  }
}

export function extractSharePassword(text: string): string | undefined {
  for (const pattern of PASSWORD_PATTERNS) {
    const match = text.match(pattern)
    if (match?.[1]) return match[1]
  }
  return undefined
}

function normalizeShareUrl(rawUrl: string): string {
  const trimmed = rawUrl
    .trim()
    .replace(/[，。；;、]+$/g, '')

  const withProtocol = /^https?:\/\//i.test(trimmed)
    ? trimmed
    : `https://${trimmed}`

  try {
    const url = new URL(withProtocol)
    const pwd = url.searchParams.get('pwd')
    url.hash = ''
    if (pwd) {
      url.searchParams.set('pwd', pwd)
    }
    return url.toString()
  } catch {
    return withProtocol
  }
}
