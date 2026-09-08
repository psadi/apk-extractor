# 🤖 Agent Troubleshooting & Engineering Guide: APK Extractor

> **Purpose:** This document is written for AI agents and human engineers working on the **APK Extractor** codebase. It details every architectural challenge, OS-specific failure mode, mobile environment quirk, and exact resolution implemented in this repository.

---

## 📱 System & Runtime Architecture

- **Operating Environment:** On-device mobile development via [Termux](https://termux.dev) on Android 16 (ColorOS / OnePlus `CPH2745`).
- **CPU Architecture:** Pure 64-bit ARM (`aarch64` / `arm64-v8a`). Note: `ro.product.cpu.abilist32` is empty — 32-bit binaries (`armeabi-v7a`) cannot run natively on this CPU.
- **Java / Gradle Toolchain:** OpenJDK 17 + Gradle 8.7 managed hermetically via [mise](https://mise.jdx.dev) (`.mise.toml`).
- **Native AAPT2:** Termux native package `aapt2` (`/data/data/com.termux/files/usr/bin/aapt2`) overriding AGP maven binaries.

---

## ⚠️ Issues, Root Causes, and Proven Solutions

### 1. "App not compatible with your phone" / Sideloading Block

#### The Symptom:
When attempting to install an extracted `.apk` (or freshly compiled release APK) by tapping it in the phone's **Files** or **Downloads** app, the system PackageInstaller immediately halts with:
> *"You cannot install the app on your device."* (ColorOS / OnePlus)  
> or *"App not compatible with your phone."*

#### Root Cause 1: Debug Keystore Sideloading Block (ColorOS Super Guard)
- **Mechanism:** In `app/build.gradle.kts`, `assembleRelease` previously reused `signingConfigs.debug` (`CN=Android Debug, O=Android, C=US`).
- **Behavior:** Modern OEM Android builds (especially ColorOS 14/15/16 on OPPO/OnePlus devices) run an aggressive system security service (Super Guard / App Protection). If an APK file in user storage (`/Download/`) is signed with the well-known Android Debug certificate, the installer **silently aborts installation** and shows the generic "App not compatible" message to prevent users from sideloading debug or test builds outside ADB.
- **Resolution:**
  1. Generated a dedicated permanent project keystore: [`keystore/release.keystore`](keystore/release.keystore):
     ```bash
     keytool -genkeypair -v -keystore keystore/release.keystore -alias apkextractor \
       -keyalg RSA -keysize 2048 -validity 10000 \
       -dname "CN=APK Extractor, OU=Mobile, O=psadi, L=Bangalore, ST=KA, C=IN" \
       -storepass apkextractor123 -keypass apkextractor123
     ```
  2. Updated `app/build.gradle.kts` so both `release` and `debug` build types use the release signing config with V1, V2, and V3 signing schemes enabled.

#### Root Cause 2: Missing Splits in Modern Play Store Apps (`isSplitRequired="true"`)
- **Mechanism:** More than 90% of apps distributed on the Google Play Store (e.g., `Aadhaar`, `AAWireless`, `WhatsApp`, banking apps) are delivered as **Split APKs (Android App Bundles)**.
  A split app does not exist as a single APK; it is split into:
  - `base.apk` (Core manifest, partial resources, primary classes)
  - `split_config.arm64_v8a.apk` (Native shared libraries)
  - `split_config.xxhdpi.apk` (Density-specific drawables)
  - `split_config.en.apk` (Localized string tables)
- **The Trap:** In `base.apk`'s `AndroidManifest.xml`, Google Play sets:
  ```xml
  <manifest ... android:isSplitRequired="true">
  ```
- **What happened:** If APK Extractor only copies `appInfo.apkPath` (`base.apk`) and names it `<AppName>_v<Version>.apk`, the user gets a file that **CAN NEVER BE INSTALLED** alone! When Android PackageInstaller opens it, it detects `isSplitRequired="true"`, finds no splits, and aborts with `INSTALL_FAILED_MISSING_SPLIT` ("App not compatible with your phone").
- **Resolution:**
  1. In `AppPackageScanner.kt`, query `appInfo.splitPublicSourceDirs` / `splitSourceDirs`.
  2. In `StorageRepository.kt`, if `appInfo.isSplitApk`:
     - Package `base.apk` and all `split_config.*.apk` files into a standard `.apks` bundle (Zip archive).
  3. In `IntentUtil.kt`, provide native `PackageInstaller.Session` multi-split streaming (`session.openWrite("base.apk")`, `session.openWrite("split_$i.apk")`) to install split bundles seamlessly without signature tampering.

#### Root Cause 3: 16KB Page Alignment & 64-bit ABI Filters
- **Mechanism:** On Android 15/16 pure 64-bit devices, native libraries packaged without 16KB page alignment or missing 64-bit binaries cause package extraction failures.
- **Resolution:**
  In `app/build.gradle.kts`:
  ```kotlin
  ndk {
      abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
  }
  packaging {
      jniLibs {
          useLegacyPackaging = true
      }
  }
  ```

---

### 2. Hardcoded App Version vs Dynamic Gradle Version

#### The Symptom:
After tagging releases, the app's Settings dialog continued to show `1.0.0 (Build 1)`.

#### Root Cause:
1. `app/build.gradle.kts` had static `versionCode` and `versionName`.
2. `SettingsDialog.kt` had a hardcoded string literal `"1.0.0 (Build 1)"`.
3. `buildConfig = true` was not enabled in `buildFeatures`.

#### Resolution:**
1. Enabled `buildConfig = true` under `buildFeatures` in `app/build.gradle.kts`.
2. Reference the generated `BuildConfig` dynamically in `SettingsDialog.kt`:
   ```kotlin
   Text(text = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
   ```

---

### 3. UI Pill Buttons Text Overflowing in Post-Extraction Banner

#### The Symptom:
After an extraction finished, the bottom floating card had buttons whose text overflowed, truncated, or wrapped awkwardly.

#### Root Cause:
The banner crammed 3 separate buttons (`Install`, `Share`, `Folder`) plus an explanatory install tip into a single row on a narrow mobile portrait screen. Furthermore, an "Install" button immediately after extracting an app that is *already installed* on the device is logically redundant and confusing.

#### Resolution:
1. Removed the redundant "Install" button and the installation tip from `AppListScreen.kt`.
2. Cleaned up the bottom banner to feature only 2 primary actions:
   - **Share APK** (`Button` with `weight(1f)`)
   - **Open Folder** (`OutlinedButton` with `weight(1f)`)
3. Used `Arrangement.spacedBy(10.dp)` to guarantee 50/50 balanced layout with zero text overflow.

---

### 4. App & APK Inspection via Magnifying Glass

#### The User Need:
Users requested a quick way to inspect any installed app's underlying APK package structure, format (Split vs Standalone), package name, and filesystem path.

#### Resolution:
1. In `AppCardItem.kt`, added a dedicated magnifying glass button (`Icons.Default.Search`) to every list item.
2. In `AppDetailBottomSheet.kt`, added an **APK Inspection Details** card displaying:
   - Package Name
   - Source Path (`/data/app/.../base.apk`)
   - Architecture Format (`Universal Standalone APK` vs `Split APK (N components)`)
   - Target SDK, Min SDK, First Install Date, and Formatted File Size.

---

### 5. Termux FUSE Filesystem Permissions & `chmod`

#### The Symptom:
Files created or copied by Termux CLI into `/storage/emulated/0/Download/` show permissions `-rw-rw----` owned by `u0_a...:media_rw`. Running `chmod 777` returns exit code 0 but does not change the permission bits.

#### Root Cause:
Android emulated storage (`/storage/emulated/0`) is a virtual FUSE filesystem. A file created by Termux's UID is masked by default `007` (`rw-rw----`), meaning external system processes cannot read the raw path directly without SAF or MediaStore content URIs.

#### Resolution:
Always perform extraction inside the Android app using `MediaStore.Downloads` API so MediaProvider assigns proper system permissions and indexes the file.

---

### 6. Background Activity Start (BAL) Restrictions on Android 14+ (API 34+)

#### The Symptom:
When invoking `context.startActivity(confirmIntent)` from `InstallReceiver` upon receiving `PackageInstaller.STATUS_PENDING_USER_ACTION`, Android 14+ logs:
`Background activity start [confirmIntent] blocked!`

#### Resolution:
In `InstallReceiver.kt`, explicitly grant background activity start mode via `ActivityOptions`:
```kotlin
val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    ActivityOptions.makeBasic().apply {
        setPendingIntentBackgroundActivityStartMode(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        )
    }.toBundle()
} else {
    null
}
context.startActivity(confirmIntent, options)
```

---

### 7. Native AAPT2 in Termux on Android ARM64

#### The Symptom:
During `./gradlew assembleRelease`, Gradle fails trying to execute `aapt2` with:
`Execution failed for task ':app:processReleaseResources'. > aapt2: Exec format error`

#### Root Cause:
Android Gradle Plugin downloads `aapt2` binaries compiled for x86_64 Linux by default. On ARM64 Android inside Termux, this binary cannot execute.

#### Resolution:
In `gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

---

### 8. Live Lifecycle & Package Broadcast Auto-Refresh

#### The Symptom:
When an extracted APK is installed (or an app is installed/uninstalled from Google Play Store), returning to APK Extractor does not display the newly installed package in search or the app list.

#### Root Cause:
`AppListViewModel` only queried `AppPackageScanner` once during `init`. There was no lifecycle listener (`onResume`) or system broadcast receiver for package changes.

#### Resolution:
In `MainActivity.kt`:
1. Register a dynamic `BroadcastReceiver` for `ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, and `ACTION_PACKAGE_REPLACED` with the `package:` data scheme (using `ContextCompat.RECEIVER_EXPORTED` on API 33+).
2. Trigger `viewModel.loadApps(showLoading = false)` in `onResume()` for silent background synchronization without UI flicker.

---

### 9. Scoped Storage Deletion of Extracted APKs on Android 10+ (API 29–35)

#### The Symptom:
Deleting an extracted APK or split bundle from the Extracted Backups tab did not delete the physical file on disk.

#### Root Cause:
On Android 10+ (API 29–35), direct `java.io.File(path).delete()` fails silently in shared storage (`Downloads/APK_Extractor/`) under Scoped Storage restrictions. Files created via MediaStore must be removed via `ContentResolver.delete()`.

#### Resolution:
In `StorageRepository.deleteExtractedApk()`:
1. Query `MediaStore.Downloads.EXTERNAL_CONTENT_URI` by `DISPLAY_NAME` and delete the matching MediaStore URI.
2. Query `MediaStore.Files.getContentUri("external")` by `DATA` (file path) and delete.
3. For SAF folders, delete via `DocumentsContract.deleteDocument(context.contentResolver, uri)`.
4. Fall back to `File.delete()` / `File.deleteRecursively()`.
5. In `AppListViewModel.kt`, call `loadExtractedApps()` unconditionally so the UI always reflects storage state.

---

### 10. Brand Alias Search Discrepancies (e.g. PlayStation vs "PS App")

#### The Symptom:
Searching for "PlayStation" returned zero results even after installing the official PlayStation app from Google Play Store.

#### Root Cause:
Sony's AndroidManifest specifies `android:label="PS App"` and package name `com.scee.psxandroid`. Substring search for "playstation" or "sony" matches neither the label nor the package name.

#### Resolution:
1. Created `SearchUtil.kt` defining brand alias synonyms (`playstation`, `sony`, `psn`, `ps4`, `ps5` for `com.scee.psxandroid`; `google play store`, `market` for `com.android.vending`, etc.).
2. Integrated alias matching into `AppListViewModel.filterAndSortApps()` and `filterExtractedApps()`.
3. Displayed a prominent brand tag badge (e.g. `[PlayStation]`) beside the app label in `AppCardItem.kt` and `ExtractedApkCardItem.kt`.
4. Automated verification via unit test suite (`SearchUtilTest.kt` and `AppFilterTest.kt`).

---

### 11. Testing & Release Publishing Gating

#### The Guideline:
- Never push a git tag (`v*`) or run `gh release create` without explicit confirmation from the user.
- Commits pushed to `main` trigger GitHub Actions to build and upload testable artifacts (`apk-extractor-builds`) without publishing a GitHub Release.
- Keep local test APKs refreshed at `/storage/emulated/0/Download/APK-Extractor-release.apk`.

## 📋 Quick Command Cheat Sheet for Future Agents

```bash
# Clean release build and Google Play Bundle (AAB)
./gradlew assembleRelease bundleRelease

# Verify signature scheme and certificate of compiled APK
apksigner verify -v --print-certs app/build/outputs/apk/release/app-release.apk

# Inspect badging, permissions, and SDK levels
aapt2 dump badging app/build/outputs/apk/release/app-release.apk | grep -E "package|version|minSdk|targetSdk"

# Verify native libraries in APK
unzip -l app/build/outputs/apk/release/app-release.apk | grep -i "\.so"

# Check GitHub Actions runs
gh run list --limit 5
```
