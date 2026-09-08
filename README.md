# APK Extractor 📦

[![License](https://img.shields.io/badge/License-AGPL_3.0-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/github/actions/workflow/status/psadi/apk-extractor/build-and-release.yml?branch=main&label=Build&logo=github)](https://github.com/psadi/apk-extractor/actions)
[![Release](https://img.shields.io/github/v/release/psadi/apk-extractor?label=Release&logo=android&color=success)](https://github.com/psadi/apk-extractor/releases/latest)
[![AI Model](https://img.shields.io/badge/AI_Model-Gemini_Flash-8E75B2?logo=googlegemini&logoColor=white)](USAGE_QUOTA.md)
[![Input Tokens](https://img.shields.io/badge/Input_Tokens-114.0M-informational?logo=google&logoColor=white)](USAGE_QUOTA.md)
[![Output Tokens](https://img.shields.io/badge/Output_Tokens-385K-blueviolet)](USAGE_QUOTA.md)
[![AI Spend](https://img.shields.io/badge/AI_Spend-$14.61_USD-brightgreen?logo=googlecloud&logoColor=white)](USAGE_QUOTA.md)
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/psadi)
[![PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal&logoColor=white)](https://paypal.me/psadithya)

A modern, production-grade native Android application for exploring installed apps and extracting base APKs with a smooth Material You (Material 3) user interface, live search, alphabetical fast-scrolling, and Storage Access Framework (SAF) integration.

---

## Features

- **Dynamic Material You Theming:** Native Monet dynamic color theming on Android 12+ (API 31–35) with fallback to an elegant Material 3 indigo/purple palette on earlier releases.
- **Triple Category Filtering:** Instant toggling between User Installed apps, System Apps, and Extracted Backups with real-time counters.
- **Real-Time Lifecycle & Broadcast Auto-Refresh:** Automatically refreshes the app list on foreground (`onResume`) and on system package installation/uninstallation broadcasts (`ACTION_PACKAGE_ADDED/REMOVED/REPLACED`).
- **Extracted Backups Explorer:** Dedicated tab to view, search, install, share, or delete previously extracted `.apk` and `.apks` split backups directly from device storage.
- **Split APK & Bundle Support:** Automatically detects split APKs, bundles them into `.apks` zip archives, and installs them seamlessly via Android's `PackageInstaller.Session`.
- **Real-Time Search & Fast-Scroller:**
  - Responsive search bar filtering by app label and package name across installed apps and extracted backup archives.
  - Interactive vertical A–Z sidebar with touch/drag tracking, haptic feedback, and floating letter indicator.
- **Instant APK Extraction:**
  - One-tap APK extraction directly to storage with smooth progress feedback.
  - Default extraction path: `Downloads/APK_Extractor/` via MediaStore API (no dangerous storage permissions required).
  - Custom directory picker via Storage Access Framework (`Intent.ACTION_OPEN_DOCUMENT_TREE`) with persistable URI permissions.
- **Action Bottom Sheet:**
  - Extract APK to storage.
  - Direct Share via standard Android share sheet (`content://` URI via `FileProvider`).
  - Open System App Settings for the target package.
- **Actionable Completion Feedback:**
  - Slide-in feedback banner offering direct "Share" and "Open Folder" actions.
- **Settings, Support & About:**
  - Configurable storage destination (SAF directory picker or MediaStore).
  - In-app Support / Donate button to fund ongoing development.
  - Full app version info, open source license, and GitHub repository links.

---

## Tech Stack & Architecture

- **Language:** Kotlin 2.0+ (Coroutines + Flow)
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** Single Activity, MVVM (Model-View-ViewModel) with Unidirectional Data Flow
- **Data & Storage:** Storage Access Framework (SAF) DocumentFile, MediaStore API, Jetpack DataStore Preferences
- **SDK Targets:** `minSdk = 26` (Android 8.0) • `targetSdk = 35` (Android 15) • `compileSdk = 35`

---

## 📱 Built 100% on Mobile with Gemini & Antigravity CLI (`agy`)

This entire application—architecture planning, Jetpack Compose UI, Material 3 theming, Storage Access Framework & MediaStore integration, Split APK bundling, local compilation, GitHub Actions CI/CD, and release publishing—was **autonomously developed and built entirely on an Android mobile device** running **Termux** and Google's **Antigravity CLI (`agy`)** powered by **Gemini**.

### 📊 AI Token Usage & Cost Transparency

| Metric | Highlights |
| :--- | :--- |
| **Cumulative AI Spend** | **$14.61 USD** |
| **Total Tokens Processed** | **114,405,857** (~114.4M tokens) |
| **Input / Output Breakdown** | 114.0M prompt tokens (6.17M cached) • 385K output tokens (202K thinking) |
| **Generations / Steps** | **922** model invocations across 6 production releases |
| **Environment** | Termux (`aarch64` Linux), hermetic JDK 17 & Gradle 8.7 via [mise](https://mise.jdx.dev), native `aapt2` |
| **Complete Accounting** | 👉 **[View Complete Usage Quota & Cost Breakdown (USAGE_QUOTA.md)](USAGE_QUOTA.md)** |

> [!TIP]
> For complete financial transparency and a release-by-release breakdown of tokens, thinking overhead, and tiered pricing across every release (v1.0.0 through v1.2.0), see [`USAGE_QUOTA.md`](USAGE_QUOTA.md).

## Getting Started & Build Instructions

This project is configured with [mise](https://mise.jdx.dev) via `.mise.toml` for hermetic and reproducible developer tooling.

### Prerequisites

Install `mise` on your development machine (Linux, macOS, WSL):
```bash
curl https://mise.run | sh
```

### 1. Provision Java and Gradle via Mise

From the project root:
```bash
mise install
```
This automatically sets up OpenJDK 17 and Gradle 8.7 as configured in `.mise.toml`.

### 2. Build Debug APK

Execute the build task using `mise` or direct `./gradlew`:
```bash
# Using mise:
mise exec -- ./gradlew assembleDebug

# Or directly via the Gradle wrapper:
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Install to Connected Device / Emulator

Ensure your device has USB debugging enabled:
```bash
# Verify device connection
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Continuous Integration & Automated Releases

Automated builds and releases are managed via GitHub Actions ([`.github/workflows/build-and-release.yml`](.github/workflows/build-and-release.yml)):

- **Push & Pull Requests to `main`:**
  - Automatically compiles `assembleDebug`, `assembleRelease`, and `bundleRelease` (Google Play App Bundle).
  - Publishes the compiled APKs and AAB as downloadable workflow run artifacts (`apk-extractor-builds`).
- **Tag Pushes (`v*`):**
  - Triggers automated release publishing to **GitHub Releases**.
  - Attaches `APK-Extractor-release.aab`, `APK-Extractor-release.apk`, and `APK-Extractor-debug.apk` directly to the release.
  - Automatically generates changelog notes from commit history.

To trigger a new release:
```bash
git tag -a v1.0.1 -m "Release v1.0.1"
git push origin v1.0.1
```

---

## 🚀 Google Play Store Publication

Everything required to publish APK Extractor on the Google Play Store is pre-configured and documented:

- **Publication Guide & Store Metadata:** See [`PLAY_STORE_GUIDE.md`](PLAY_STORE_GUIDE.md) for complete copy-paste descriptions, store graphics specifications, upload key generation commands, and console walkthrough.
- **Privacy Policy:** See [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) (required by Google Play).
- **Play Store App Bundle:** Generate locally via `./gradlew bundleRelease` or download `APK-Extractor-release.aab` directly from GitHub Releases / CI artifacts.

---

## Google Play Store Policy Compliance: `QUERY_ALL_PACKAGES`

This app declares the `android.permission.QUERY_ALL_PACKAGES` permission in `AndroidManifest.xml` to allow users to view, back up, and extract APKs for all installed applications on Android 11+ (API 30+).

### Google Play Submission Guidance
- **Permitted Core Use Case:** Google Play policy permits `QUERY_ALL_PACKAGES` for apps that require broad package visibility to provide core functionality, specifically categorized under **Device Search, Antivirus, File Management, or Backup & Restore tools**.
- **Declaration in Play Console:**
  1. In the Google Play Console, go to **Policy and Programs > App Content > Sensitive permissions and APIs**.
  2. Select **QUERY_ALL_PACKAGES**.
  3. Under justification, indicate that the app's primary core functionality is an APK backup, inspection, and extraction utility that requires discovering all installed packages on the device to fulfill user-requested backup tasks.

---

## 🛠️ Architecture & Troubleshooting Guide for Agents

For engineers and AI agents working on this repository, see [`AGENT_TROUBLESHOOTING.md`](AGENT_TROUBLESHOOTING.md) for in-depth documentation on:
- Sideloading compatibility and ColorOS / Android 14+ Super Guard keystore rules
- Split APK (App Bundle) structure and `isSplitRequired` resolution
- Termux mobile toolchain, FUSE permissions, and hermetic Gradle setup
- Android 14+ background activity restrictions and native `PackageInstaller` sessions

---

## Support & Donations

APK Extractor is 100% free and open source. **100% of all contributions go solely and directly toward the development of the app** — adding new features, resolving bug reports, and funding AI token usage and API spends alone. There is zero ambiguity: your support directly fuels continuous mobile-first development and open-source improvements.

- **GitHub Sponsors:** [github.com/sponsors/psadi](https://github.com/sponsors/psadi)
- **PayPal:** [paypal.me/psadithya](https://paypal.me/psadithya)

---

## License

This project is licensed under the GNU Affero General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
