#!/usr/bin/env python3
# Modified from upstream BaiduPCS-Rust for Android distribution compliance and third-party legal asset generation.
"""
Generate open-source legal assets for the Android APK distribution.

Outputs:
- LICENSE.txt (upstream Apache-2.0 text)
- NOTICE.txt (derivative distribution notice)
- third-party-index.json
- third-party/<id>/LICENSE.txt
- third-party/<id>/NOTICE.txt (when available)
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
import re
import shutil
import sys
import textwrap
import tomllib
import zipfile
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
import xml.etree.ElementTree as ET


COMMON_LICENSE_NAMES = (
    "LICENSE",
    "LICENCE",
    "COPYING",
    "COPYRIGHT",
)
COMMON_NOTICE_NAMES = ("NOTICE",)
ZIP_LICENSE_PATTERNS = (
    "META-INF/LICENSE",
    "META-INF/LICENCE",
    "META-INF/NOTICE",
    "META-INF/ASL2.0",
    "META-INF/LGPL2.1",
)

LICENSE_NORMALIZATION = {
    "apache-2.0": "Apache-2.0",
    "apache 2.0": "Apache-2.0",
    "apache license 2.0": "Apache-2.0",
    "the apache license, version 2.0": "Apache-2.0",
    "the apache software license, version 2.0": "Apache-2.0",
    "mit": "MIT",
    "mit license": "MIT",
    "bsd-2-clause": "BSD-2-Clause",
    "bsd 2-clause": "BSD-2-Clause",
    "bsd-3-clause": "BSD-3-Clause",
    "bsd 3-clause": "BSD-3-Clause",
    "isc": "ISC",
    "isc license": "ISC",
    "mpl-2.0": "MPL-2.0",
    "mpl 2.0": "MPL-2.0",
    "mozilla public license 2.0": "MPL-2.0",
    "zlib": "Zlib",
    "zlib license": "Zlib",
    "cc0-1.0": "CC0-1.0",
    "cc0": "CC0-1.0",
    "unlicense": "Unlicense",
    "boost software license 1.0": "BSL-1.0",
    "bsl-1.0": "BSL-1.0",
    "0bsd": "0BSD",
    "mit-0": "MIT-0",
    "unicode-3.0": "Unicode-3.0",
    "unicode dfs 2016": "Unicode-DFS-2016",
    "unicode-dfs-2016": "Unicode-DFS-2016",
}

STANDARD_LICENSE_TEXTS = {
    "MIT": """MIT License

Copyright (c) <year> <copyright holders>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.""",
    "BSD-2-Clause": """BSD 2-Clause License

Copyright (c) <year> <copyright holders>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.""",
    "BSD-3-Clause": """BSD 3-Clause License

Copyright (c) <year> <copyright holders>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.""",
    "ISC": """ISC License

Copyright (c) <year> <copyright holders>

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.""",
    "Zlib": """zlib License

Copyright (c) <year> <copyright holders>

This software is provided 'as-is', without any express or implied warranty. In
no event will the authors be held liable for any damages arising from the use
of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not claim
   that you wrote the original software. If you use this software in a
   product, an acknowledgment in the product documentation would be
   appreciated but is not required.

2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.

3. This notice may not be removed or altered from any source distribution.""",
}


@dataclass
class PackageEntry:
    source: str
    name: str
    version: str
    license_expression: str
    license_path: str
    notice_path: str | None
    homepage: str | None = None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", required=True)
    parser.add_argument("--android-report", required=True)
    parser.add_argument("--out-dir", required=True)
    parser.add_argument(
        "--include-web-runtime",
        action="store_true",
        help="Include frontend npm runtime packages when the APK still ships the web UI assets.",
    )
    return parser.parse_args()


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8").replace("\r\n", "\n").strip()


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def slugify(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]+", "_", value).strip("_")


def unique_texts(texts: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for text in texts:
        normalized = text.strip()
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        out.append(normalized)
    return out


def join_named_texts(items: list[tuple[str, str]]) -> str:
    parts = []
    for index, (label, text) in enumerate(items, start=1):
        header = f"===== {label} ====="
        parts.append(f"{header}\n{text.strip()}")
    return "\n\n".join(parts)


def normalize_license_name(value: str) -> str:
    key = re.sub(r"\s+", " ", value.strip()).lower()
    return LICENSE_NORMALIZATION.get(key, value.strip())


def extract_license_tokens(expression: str) -> list[str]:
    normalized = expression.replace("/", " OR ").replace("(", " ").replace(")", " ")
    tokens = re.findall(r"[A-Za-z0-9.+-]+", normalized)
    results: list[str] = []
    for token in tokens:
        lowered = token.lower()
        if lowered in {"or", "and", "with"}:
            continue
        results.append(normalize_license_name(token))
    return list(dict.fromkeys(results))


def split_license_alternatives(expression: str) -> list[str]:
    normalized = expression.replace("/", " OR ").replace("(", " ").replace(")", " ")
    alternatives = [
        item.strip()
        for item in re.split(r"\s+OR\s+", normalized, flags=re.IGNORECASE)
        if item.strip()
    ]
    return alternatives or [expression]


def find_named_files(root: Path, prefixes: tuple[str, ...]) -> list[Path]:
    matches: list[Path] = []
    for child in root.iterdir():
        if child.is_file() and child.name.upper().startswith(prefixes):
            matches.append(child)
    return sorted(matches, key=lambda item: item.name.lower())


def read_zip_texts(zip_path: Path, prefixes: tuple[str, ...]) -> list[tuple[str, str]]:
    if not zip_path.exists():
        return []

    results: list[tuple[str, str]] = []
    with zipfile.ZipFile(zip_path) as archive:
        for member in archive.namelist():
            upper = member.upper()
            if any(upper.startswith(prefix.upper()) for prefix in prefixes):
                try:
                    raw = archive.read(member)
                    text = raw.decode("utf-8", errors="replace").strip()
                    if text:
                        results.append((member, text))
                except KeyError:
                    continue
    return results


def license_files_from_directory(package_root: Path, license_file: str | None = None) -> list[tuple[str, str]]:
    results: list[tuple[str, str]] = []

    if license_file:
        target = package_root / license_file
        if target.exists() and target.is_file():
            results.append((license_file, read_text(target)))

    for path in find_named_files(package_root, COMMON_LICENSE_NAMES):
        results.append((path.name, read_text(path)))

    deduped = {}
    for label, text in results:
        deduped[text] = label
    return [(label, text) for text, label in deduped.items()]


def notice_files_from_directory(package_root: Path) -> list[tuple[str, str]]:
    results: list[tuple[str, str]] = []
    for path in find_named_files(package_root, COMMON_NOTICE_NAMES):
        results.append((path.name, read_text(path)))
    deduped = {}
    for label, text in results:
        deduped[text] = label
    return [(label, text) for text, label in deduped.items()]


def cache_simple_license_texts(cache: dict[str, str], expression: str, license_text: str) -> None:
    tokens = extract_license_tokens(expression)
    if len(tokens) == 1 and tokens[0] not in cache:
        cache[tokens[0]] = license_text


def build_license_text_from_cache(expression: str, license_cache: dict[str, str]) -> str | None:
    for alternative in split_license_alternatives(expression):
        tokens = extract_license_tokens(alternative)
        if not tokens:
            continue

        items = []
        for token in tokens:
            text = license_cache.get(token)
            if not text:
                items = []
                break
            items.append((token, text))

        if items:
            if alternative.strip() != expression.strip():
                header = textwrap.dedent(
                    f"""
                    Selected license alternative from original SPDX expression:
                    {alternative.strip()}

                    Original SPDX expression:
                    {expression.strip()}
                    """
                ).strip()
                return f"{header}\n\n{join_named_texts(items)}"
            return join_named_texts(items)

    tokens = extract_license_tokens(expression)
    if not tokens:
        return None

    items = []
    for token in tokens:
        text = license_cache.get(token)
        if not text:
            return None
        items.append((token, text))

    return join_named_texts(items)


def locate_gradle_cache_root(repo_root: Path) -> Path:
    home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    return home / "caches" / "modules-2" / "files-2.1"


def locate_cargo_registry_roots(repo_root: Path) -> list[Path]:
    candidates = [
        Path(os.environ.get("CARGO_HOME", "")),
        repo_root.parent / "_cargo_home",
        Path.home() / ".cargo",
    ]
    roots: list[Path] = []
    for candidate in candidates:
        if not candidate:
            continue
        registry = candidate / "registry" / "src"
        if not registry.exists():
            continue
        for child in registry.iterdir():
            if child.is_dir():
                roots.append(child)
    return roots


def locate_crate_dir(registry_roots: list[Path], name: str, version: str) -> Path | None:
    for root in registry_roots:
        candidate = root / f"{name}-{version}"
        if candidate.exists():
            return candidate
    return None


def parse_cargo_runtime_packages(repo_root: Path) -> list[tuple[str, str]]:
    backend_root = repo_root / "backend"
    lock = tomllib.loads((backend_root / "Cargo.lock").read_text(encoding="utf-8"))
    manifest = tomllib.loads((backend_root / "Cargo.toml").read_text(encoding="utf-8"))
    packages = lock["package"]
    root_package = next(pkg for pkg in packages if pkg["name"] == "baidu-netdisk-rust")
    by_key = {(pkg["name"], pkg["version"]): pkg for pkg in packages}
    versions_by_name: dict[str, list[str]] = defaultdict(list)
    for pkg in packages:
        versions_by_name[pkg["name"]].append(pkg["version"])

    dev_dependencies = set((manifest.get("dev-dependencies") or {}).keys())

    def resolve(name: str, version: str | None) -> tuple[str, str] | None:
        if version:
            return (name, version)
        versions = versions_by_name.get(name, [])
        if len(versions) == 1:
            return (name, versions[0])
        return None

    def parse_dependency(value: str) -> tuple[str, str] | None:
        match = re.match(r"^([^ ]+)(?: ([^ ]+))?(?: \(.+\))?$", value)
        if not match:
            return None
        return resolve(match.group(1), match.group(2))

    seeds: list[tuple[str, str]] = []
    for dependency in root_package.get("dependencies", []):
        parsed = parse_dependency(dependency)
        if parsed and parsed[0] not in dev_dependencies:
            seeds.append(parsed)

    visited: set[tuple[str, str]] = set()
    stack = list(seeds)
    while stack:
        current = stack.pop()
        if current in visited:
            continue
        visited.add(current)
        package = by_key.get(current)
        if not package:
            continue
        for dependency in package.get("dependencies", []):
            parsed = parse_dependency(dependency)
            if parsed:
                stack.append(parsed)

    return sorted(visited)


def frontend_runtime_packages(repo_root: Path) -> list[dict[str, object]]:
    frontend_root = repo_root / "frontend"
    package_json = json.loads((frontend_root / "package.json").read_text(encoding="utf-8"))
    seen: set[str] = set()
    queue = list((package_json.get("dependencies") or {}).keys())
    results: list[dict[str, object]] = []

    while queue:
        name = queue.pop(0)
        if name in seen:
            continue
        seen.add(name)

        if name.startswith("@"):
            scope, sub = name.split("/", 1)
            package_dir = frontend_root / "node_modules" / scope / sub
        else:
            package_dir = frontend_root / "node_modules" / name

        package_file = package_dir / "package.json"
        if not package_file.exists():
            continue

        data = json.loads(package_file.read_text(encoding="utf-8"))
        results.append(
            {
                "name": data["name"],
                "version": data["version"],
                "license_expression": str(data.get("license") or "(missing)"),
                "homepage": data.get("homepage") or data.get("repository"),
                "package_dir": package_dir,
            }
        )
        for dependency in (data.get("dependencies") or {}).keys():
            queue.append(dependency)

    return sorted(results, key=lambda item: (str(item["name"]), str(item["version"])))


def locate_gradle_module_pom(pom_path: Path, group: str, artifact: str, version: str) -> Path | None:
    if len(pom_path.parents) < 5:
        return None

    cache_root = pom_path.parents[4]
    version_dir = cache_root / group / artifact / version
    if not version_dir.exists():
        return None

    return next(
        (
            candidate
            for candidate in version_dir.rglob("*.pom")
            if candidate.is_file()
        ),
        None,
    )


def parse_pom_licenses(
    pom_path: Path,
    visited: set[Path] | None = None,
) -> tuple[list[str], str | None]:
    if not pom_path or not pom_path.exists():
        return [], None

    visited = visited or set()
    resolved_path = pom_path.resolve()
    if resolved_path in visited:
        return [], None
    visited.add(resolved_path)

    try:
        tree = ET.parse(pom_path)
    except ET.ParseError:
        return [], None

    root = tree.getroot()
    namespace = ""
    if root.tag.startswith("{"):
        namespace = root.tag.split("}", 1)[0] + "}"

    homepage = root.findtext(f"{namespace}url")
    licenses = []
    for item in root.findall(f"{namespace}licenses/{namespace}license"):
        name = item.findtext(f"{namespace}name")
        if name:
            licenses.append(normalize_license_name(name))

    if licenses:
        return licenses, homepage

    parent = root.find(f"{namespace}parent")
    if parent is None:
        return [], homepage

    parent_group = parent.findtext(f"{namespace}groupId") or ""
    parent_artifact = parent.findtext(f"{namespace}artifactId") or ""
    parent_version = parent.findtext(f"{namespace}version") or ""
    if not parent_group or not parent_artifact or not parent_version:
        return [], homepage

    parent_pom = locate_gradle_module_pom(pom_path, parent_group, parent_artifact, parent_version)
    if parent_pom is None:
        return [], homepage

    parent_licenses, parent_homepage = parse_pom_licenses(parent_pom, visited)
    return parent_licenses, homepage or parent_homepage


def load_android_runtime_packages(android_report: Path) -> list[dict[str, object]]:
    if not android_report.exists():
        raise FileNotFoundError(f"Android runtime report not found: {android_report}")
    data = json.loads(android_report.read_text(encoding="utf-8"))
    return sorted(data, key=lambda item: (item["group"], item["name"], item["version"]))


def copy_bundle(
    out_root: Path,
    source: str,
    name: str,
    version: str,
    license_expression: str,
    license_text: str,
    notice_text: str | None,
    homepage: str | None,
) -> PackageEntry:
    package_id = slugify(f"{source}__{name}__{version}")
    target_dir = out_root / "third-party" / package_id
    write_text(target_dir / "LICENSE.txt", license_text)

    notice_path = None
    if notice_text:
        write_text(target_dir / "NOTICE.txt", notice_text)
        notice_path = f"/open-source/third-party/{package_id}/NOTICE.txt"

    return PackageEntry(
        source=source,
        name=name,
        version=version,
        license_expression=license_expression,
        license_path=f"/open-source/third-party/{package_id}/LICENSE.txt",
        notice_path=notice_path,
        homepage=homepage,
    )


def generate_notice_text() -> str:
    return textwrap.dedent(
        """
        柏渡云盘 Android 移植版 NOTICE

        本应用基于开源项目 BaiduPCS-Rust v1.12.1 进行 Android 本地化移植与系统集成。

        上游项目名称: BaiduPCS-Rust
        上游项目作者: komorebiCarry
        上游项目地址: https://github.com/komorebiCarry/BaiduPCS-Rust
        上游引用版本: https://github.com/komorebiCarry/BaiduPCS-Rust/releases/tag/v1.12.1

        本移植版包含以下类型的修改:
        - Android 原生壳层与本地运行时集成
        - 移动端 UI 与交互适配
        - 上传/导入流程适配
        - 下载目录与系统能力桥接
        - 开源许可与第三方依赖清单展示

        本应用为独立 Android 移植版，非上游官方发布，也非相关品牌官方客户端。

        与上游项目直接相关的版权、归属与许可条款，仍以上游仓库中的 Apache License 2.0 许可证文件及相关声明为准。
        """
    ).strip()


def main() -> int:
    args = parse_args()
    repo_root = Path(args.repo_root).resolve()
    android_report = Path(args.android_report).resolve()
    build_root = Path(args.out_dir).resolve()
    open_source_root = build_root / "open-source"

    if open_source_root.exists():
        shutil.rmtree(open_source_root)
    open_source_root.mkdir(parents=True, exist_ok=True)

    upstream_license = read_text(repo_root / "LICENSE")
    notice_file = repo_root / "NOTICE.txt"
    notice_text = read_text(notice_file) if notice_file.exists() else generate_notice_text()
    write_text(open_source_root / "LICENSE.txt", upstream_license)
    write_text(open_source_root / "NOTICE.txt", notice_text)

    license_cache: dict[str, str] = {
        **STANDARD_LICENSE_TEXTS,
        "Apache-2.0": upstream_license,
    }
    entries: list[PackageEntry] = []
    errors: list[str] = []

    # Web packages are no longer part of the Android artifact after the native UI migration.
    if args.include_web_runtime:
        for package in frontend_runtime_packages(repo_root):
            package_dir = Path(package["package_dir"])
            license_expression = str(package["license_expression"])
            license_parts = license_files_from_directory(package_dir)
            notice_parts = notice_files_from_directory(package_dir)

            if license_parts:
                license_text = join_named_texts(license_parts)
                cache_simple_license_texts(license_cache, license_expression, license_text)
            else:
                license_text = build_license_text_from_cache(license_expression, license_cache)

            if not license_text:
                errors.append(f"web {package['name']}@{package['version']}: missing license text")
                continue

            notice_text_value = join_named_texts(notice_parts) if notice_parts else None
            entries.append(
                copy_bundle(
                    open_source_root,
                    "web",
                    str(package["name"]),
                    str(package["version"]),
                    license_expression,
                    license_text,
                    notice_text_value,
                    str(package.get("homepage") or "") or None,
                )
            )

    # Rust packages
    registry_roots = locate_cargo_registry_roots(repo_root)
    for name, version in parse_cargo_runtime_packages(repo_root):
        crate_dir = locate_crate_dir(registry_roots, name, version)
        if crate_dir is None:
            # Packages without a local source directory are not part of the built Android artifact set.
            continue

        cargo_toml = tomllib.loads((crate_dir / "Cargo.toml").read_text(encoding="utf-8"))
        package_meta = cargo_toml.get("package", {})
        license_expression = str(
            package_meta.get("license")
            or package_meta.get("license-file")
            or "(missing)"
        )
        license_parts = license_files_from_directory(crate_dir, package_meta.get("license-file"))
        notice_parts = notice_files_from_directory(crate_dir)

        if license_parts:
            license_text = join_named_texts(license_parts)
            cache_simple_license_texts(license_cache, license_expression, license_text)
        else:
            license_text = build_license_text_from_cache(license_expression, license_cache)

        if not license_text:
            errors.append(f"rust {name}@{version}: missing license text")
            continue

        notice_text_value = join_named_texts(notice_parts) if notice_parts else None
        entries.append(
            copy_bundle(
                open_source_root,
                "rust",
                name,
                version,
                license_expression,
                license_text,
                notice_text_value,
                str(package_meta.get("homepage") or package_meta.get("repository") or "") or None,
            )
        )

    # Android packages
    for package in load_android_runtime_packages(android_report):
        group = str(package["group"])
        name = str(package["name"])
        version = str(package["version"])
        artifact_path = Path(str(package["artifactPath"]))
        pom_path = Path(str(package["pomPath"])) if package.get("pomPath") else None

        pom_licenses, homepage = parse_pom_licenses(pom_path) if pom_path else ([], None)
        license_expression = " OR ".join(pom_licenses) if pom_licenses else "(missing)"

        zip_license_parts = read_zip_texts(artifact_path, tuple(pattern.upper() for pattern in ZIP_LICENSE_PATTERNS))
        zip_notice_parts = [
            (label, text)
            for label, text in zip_license_parts
            if "/NOTICE" in label.upper()
        ]
        zip_license_only_parts = [
            (label, text)
            for label, text in zip_license_parts
            if "/NOTICE" not in label.upper()
        ]

        if zip_license_only_parts:
            license_text = join_named_texts(zip_license_only_parts)
            cache_simple_license_texts(license_cache, license_expression, license_text)
        else:
            license_text = build_license_text_from_cache(license_expression, license_cache)

        if not license_text:
            errors.append(f"android {group}:{name}:{version}: missing license text")
            continue

        notice_text_value = join_named_texts(zip_notice_parts) if zip_notice_parts else None
        entries.append(
            copy_bundle(
                open_source_root,
                "android",
                f"{group}:{name}",
                version,
                license_expression,
                license_text,
                notice_text_value,
                homepage,
            )
        )

    entries.sort(key=lambda item: (item.source, item.name.lower(), item.version))

    index = {
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        "appName": "柏渡云盘",
        "upstream": {
            "name": "BaiduPCS-Rust",
            "version": "v1.12.1",
            "author": "komorebiCarry",
            "license": "Apache License 2.0",
            "releaseUrl": "https://github.com/komorebiCarry/BaiduPCS-Rust/releases/tag/v1.12.1",
        },
        "packages": [entry.__dict__ for entry in entries],
    }
    write_text(
        open_source_root / "third-party-index.json",
        json.dumps(index, ensure_ascii=False, indent=2),
    )

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(
        f"Generated {len(entries)} third-party legal entries in {open_source_root}",
        file=sys.stdout,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
