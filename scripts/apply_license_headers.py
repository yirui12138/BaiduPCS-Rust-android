#!/usr/bin/env python3
# SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
# SPDX-License-Identifier: Apache-2.0
#
# This script is part of the Android port in this repository.
# Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
# for Android integration, mobile UX, and distribution compliance.
# See the repository LICENSE and NOTICE files for details.

from __future__ import annotations

from pathlib import Path
import re


ROOT = Path(__file__).resolve().parent.parent

INCLUDE_ROOTS = [
    ROOT / "backend" / "src",
    ROOT / "frontend" / "src",
    ROOT / "android",
]

INCLUDE_FILES = [
    ROOT / "android" / "app" / "build.gradle.kts",
    ROOT / "android" / "build.gradle.kts",
    ROOT / "android" / "settings.gradle.kts",
]

EXCLUDED_PARTS = {
    "node_modules",
    "build",
    "dist",
    "target",
    ".git",
}

LINE_COMMENT_EXTENSIONS = {".rs", ".kt", ".kts", ".ts"}
BLOCK_COMMENT_EXTENSIONS = {".vue"}

HEADER_BODY = [
    "This file is part of the Android port in this repository.",
    "Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified",
    "for Android integration, mobile UX, and distribution compliance.",
    "See the repository LICENSE and NOTICE files for details.",
]

OLD_HEADER_PATTERNS = [
    re.compile(r"^// Modified from upstream BaiduPCS-Rust[^\n]*\n+", re.MULTILINE),
    re.compile(r"^<!-- Modified from upstream BaiduPCS-Rust.*?-->\s*\n*", re.DOTALL),
    re.compile(
        r"^// SPDX-FileCopyrightText: Copyright \d{4} .+?\n"
        r"// SPDX-License-Identifier: Apache-2\.0\n"
        r"//\n"
        r"// This file is part of .+?\n"
        r"// Derived from BaiduPCS-Rust v1\.12\.1 by komorebiCarry and modified\n"
        r"// for Android integration, mobile UX, and distribution compliance\.\n"
        r"// See the repository LICENSE and NOTICE files for details\.\n\n?",
        re.MULTILINE,
    ),
    re.compile(
        r"^<!--\n"
        r"SPDX-FileCopyrightText: Copyright \d{4} .+?\n"
        r"SPDX-License-Identifier: Apache-2\.0\n\n"
        r"This file is part of .+?\n"
        r"Derived from BaiduPCS-Rust v1\.12\.1 by komorebiCarry and modified\n"
        r"for Android integration, mobile UX, and distribution compliance\.\n"
        r"See the repository LICENSE and NOTICE files for details\.\n"
        r"-->\s*\n*",
        re.MULTILINE,
    ),
]


def should_skip(path: Path) -> bool:
    return any(part in EXCLUDED_PARTS for part in path.parts)


def iter_target_files() -> list[Path]:
    files: set[Path] = set()
    for root in INCLUDE_ROOTS:
        for path in root.rglob("*"):
            if not path.is_file() or should_skip(path):
                continue
            if path.suffix in LINE_COMMENT_EXTENSIONS | BLOCK_COMMENT_EXTENSIONS:
                files.add(path)
    for path in INCLUDE_FILES:
        if path.is_file():
            files.add(path)
    return sorted(files)


def build_header(path: Path) -> str:
    if path.suffix in LINE_COMMENT_EXTENSIONS:
        lines = [
            "// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors",
            "// SPDX-License-Identifier: Apache-2.0",
            "//",
            *[f"// {line}" for line in HEADER_BODY],
            "",
            "",
        ]
        return "\n".join(lines)
    if path.suffix in BLOCK_COMMENT_EXTENSIONS:
        lines = [
            "<!--",
            "SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors",
            "SPDX-License-Identifier: Apache-2.0",
            "",
            *HEADER_BODY,
            "-->",
            "",
            "",
        ]
        return "\n".join(lines)
    raise ValueError(f"Unsupported file type: {path}")


def strip_known_headers(text: str) -> str:
    updated = text
    for pattern in OLD_HEADER_PATTERNS:
        updated = pattern.sub("", updated, count=1)
    return updated.lstrip("\n")


def split_shebang(text: str) -> tuple[str, str]:
    if text.startswith("#!"):
        newline = text.find("\n")
        if newline == -1:
            return text, ""
        return text[: newline + 1], text[newline + 1 :]
    return "", text


def detect_encoding(path: Path) -> str:
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        return "utf-8-sig"
    return "utf-8"


def update_file(path: Path) -> bool:
    encoding = detect_encoding(path)
    original = path.read_text(encoding=encoding)
    shebang, body = split_shebang(original)
    body = strip_known_headers(body)
    updated = f"{shebang}{build_header(path)}{body}"
    if updated == original:
        return False
    path.write_text(updated, encoding=encoding, newline="\n")
    return True


def main() -> None:
    changed = 0
    for path in iter_target_files():
        if update_file(path):
            changed += 1
            print(path.relative_to(ROOT))
    print(f"Updated {changed} files.")


if __name__ == "__main__":
    main()
