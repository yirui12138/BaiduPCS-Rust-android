# Modified From Upstream

This repository is a derivative Android port based on the upstream project `BaiduPCS-Rust` by `komorebiCarry`.

The following files were modified or added in this port and carry material changes for Android packaging, mobile UX, branding, and open-source compliance:

- `android/app/build.gradle.kts`
  - Adds Android runtime dependency reporting and open-source asset generation.
- `android/app/src/main/java/com/baidupcs/android/MainActivity.kt`
  - Adds Android shell behavior, startup UI, and compliance-safe in-app container behavior.
- `android/app/src/main/java/com/baidupcs/android/core/RuntimeKeeperService.kt`
  - Adjusts foreground-service copy and product branding for the Android port.
- `android/app/src/main/res/values/strings.xml`
  - Changes the user-visible app name for the Android port.
- `frontend/src/layouts/MainLayout.vue`
  - Adds mobile navigation, legal entry points, and Android-oriented shell behavior.
- `frontend/src/constants/appInfo.ts`
  - Centralizes Android product branding, upstream attribution constants, and displayed app version helpers.
- `frontend/src/router/index.ts`
  - Adds the in-app legal route and updates product-facing titles.
- `frontend/src/views/LoginView.vue`
  - Reworks the mobile login experience and Android product branding.
- `frontend/src/views/SettingsView.vue`
  - Adds legal entry points and corrects product/license presentation.
- `frontend/src/views/CreditsView.vue`
  - Adds the legal center page for upstream attribution, NOTICE, and third-party licenses.
- `frontend/src/components/settings/AuthSettingsSection.vue`
  - Updates exported recovery-code branding for the Android port.
- `scripts/generate_open_source_assets.py`
  - New build-time generator for LICENSE, NOTICE, and third-party license assets included in APK distributions.
- `scripts/apply_license_headers.py`
  - New repository maintenance script that standardizes Apache 2.0 license headers across first-party source files and key Android build scripts.
- `NOTICE.txt`
  - New NOTICE file for the Android derivative distribution.

In addition to the files above, the Android port includes broader UI, interaction, and packaging changes across the repository to support on-device operation and mobile distribution.
