# Open Source Package Notes

This directory is a source-only open-source package refreshed from the local
Android port workspace.

## Package Scope

- Source code, documentation, tests, build scripts, and compliance files are included.
- Built APK/AAB files, dependency caches, build outputs, logs, runtime databases, and local account data are excluded.
- The original local working directory was not modified, moved, or cleaned.
- The existing `.git` directory in this open-source worktree was preserved.

## Upstream And License

- This Android port is derived from `BaiduPCS-Rust v1.12.1` by `komorebiCarry`.
- Upstream source reference: `https://github.com/komorebiCarry/BaiduPCS-Rust/releases/tag/v1.12.1`
- License text is in `LICENSE`.
- Android port notice text is in `NOTICE.txt`.
- Modified-from-upstream notes are in `MODIFIED_FROM_UPSTREAM.md`.

## Before Publishing

- Review `README.md` and repository description before creating or updating a public GitHub repository.
- Do not add local build outputs, signing keys, account cookies, session files, local databases, or downloaded user data.
- If publishing APK binaries later, perform a separate binary distribution compliance check.
