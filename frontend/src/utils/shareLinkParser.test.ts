// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { describe, expect, it } from 'vitest'
import { parseBaiduShareText } from './shareLinkParser'

describe('parseBaiduShareText', () => {
  it('parses a plain /s share link', () => {
    const result = parseBaiduShareText('https://pan.baidu.com/s/1abcDEF_123')
    expect(result?.shareUrl).toBe('https://pan.baidu.com/s/1abcDEF_123')
  })

  it('parses share init links', () => {
    const result = parseBaiduShareText('pan.baidu.com/share/init?surl=abcDEF')
    expect(result?.shareUrl).toBe('https://pan.baidu.com/share/init?surl=abcDEF')
  })

  it('extracts pwd query password', () => {
    const result = parseBaiduShareText('https://pan.baidu.com/s/1abc?pwd=9x8y')
    expect(result?.password).toBe('9x8y')
  })

  it('extracts Chinese extraction code', () => {
    const result = parseBaiduShareText('链接: https://pan.baidu.com/s/1abc 提取码：a1B2')
    expect(result?.password).toBe('a1B2')
  })

  it('returns null for unrelated text', () => {
    expect(parseBaiduShareText('hello clipboard')).toBeNull()
  })

  it('uses the first valid share link', () => {
    const result = parseBaiduShareText('https://pan.baidu.com/s/1first https://pan.baidu.com/s/1second')
    expect(result?.shareUrl).toBe('https://pan.baidu.com/s/1first')
  })
})
