# AGENTS.md

Native Android app (Kotlin + Jetpack Compose, Material 3) that lists installed apps and extracts base/split APKs. Single module `:app`, package `com.psadi.apkextractor`.

## Knowledge lookup: use graphify first

- Rules live in `.agents/rules/graphify.md`; workflow in `.agents/workflows/graphify.md`.
- If `graphify-out/graph.json` exists, prefer `graphify query "<question>"`, `graphify path "A" "B"`, `graphify explain "<concept>"` over grep/file reads. If `graphify-out/wiki/index.md` exists, navigate it instead of raw files.
- `graphify-out/` is gitignored, so it is absent on a fresh clone. Generate/refresh with the `/graphify` workflow, then keep current with `graphify update .` after code edits (AST-only, no API cost).
- `graphify-out/graph.json` has a custom git merge driver (`.gitattributes`); never hand-edit it.

## Commands

- Provision toolchain: `mise install` (OpenJDK 17 + Gradle 8.7 + `android-sdk` 23.0 per `.mise.toml`).
- First run only: `mise run android:sdk` accepts licenses and installs `platforms;android-35` + `build-tools;34/35`. The SDK lives under the mise install dir; mise exports `ANDROID_HOME`/`ANDROID_SDK_ROOT`, so no `local.properties` and no systemwide SDK are needed.
- Run Gradle through mise (`mise exec -- ./gradlew ...` or an activated shell) so `ANDROID_HOME` is set; bare `./gradlew` fails with "SDK location not found".
- Debug APK: `mise exec -- ./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`
- Release + Play bundle: `mise exec -- ./gradlew assembleRelease bundleRelease`
- Tests (JVM only, JUnit4 + Mockito): `mise exec -- ./gradlew testDebugUnitTest`
- Single test: `mise exec -- ./gradlew testDebugUnitTest --tests "com.psadi.apkextractor.SearchUtilTest"`
- Lint: `mise exec -- ./gradlew lint`. No separate typecheck; Kotlin compilation is it.
- No `src/androidTest` source set. `testOptions.unitTests.isReturnDefaultValues = true` lets stubbed Android framework calls return defaults, so pure-JVM tests cannot exercise real `PackageManager`/`ContentResolver` behavior.

## Gotchas

- Signing: both `debug` and `release` use `keystore/release.keystore` with hardcoded creds (`apkextractor123`, alias `apkextractor`, V1–V4). This is intentional — ColorOS/Super Guard silently refuses to sideload APKs signed with the Android debug cert. Do not "fix" it back to debug signing.
- Versioning: `versionCode`/`versionName` live only in `app/build.gradle.kts` (currently `5` / `1.2.0`). Bump there; the Settings dialog reads `BuildConfig.VERSION_NAME/CODE`. Never hardcode a version string.
- `data/package/` maps to the escaped Kotlin package `data.\`package\``; import as `com.psadi.apkextractor.data.\`package\`.AppPackageScanner`.
- Split APKs: gather paths from `ApplicationInfo.splitPublicSourceDirs`/`splitSourceDirs`; bundles are zipped `.apks` and installed via `PackageInstaller.Session` (`base.apk` + `split_N.apk`). Never assume one APK per app.
- Search is fully generic in `SearchUtil` (camelCase, acronyms, normalized/alphanumeric tokens). Do not hardcode app or brand names; add cases to `SearchUtilTest`.
- System classification: `isSystemApp = FLAG_SYSTEM && !FLAG_UPDATED_SYSTEM_APP` (store-updated OEM apps count as user apps).
- Scoped storage (API 29+): delete extracted files through MediaStore/DocumentsContract; `File.delete()` fails silently.
- `AGENT_TROUBLESHOOTING.md` documents root causes for install/extraction/BAL/FUSE/AAPT2 issues. Its claim that `gradle.properties` sets `android.aapt2FromMavenOverride` is stale — it does not, and adding the Termux path would break desktop/CI builds.

## Architecture

- Entry flow: `MainActivity` (registers package-change receiver + onResume refresh) -> `AppListViewModel` (single `StateFlow<AppListUiState>`) -> `AppPackageScanner` (installed apps) and `StorageRepository` (extract/SAF/MediaStore/delete). `PreferencesManager` = DataStore. `IntentUtil` = share/install/open-info; `InstallReceiver` = `PackageInstaller` status callback.
- UI is Compose under `ui/` (`screens/`, `components/`, `theme/`); keep MVVM/UDF — state changes go through the ViewModel.

## Release gating

- Never push a `v*` tag or run `gh release create` without explicit user confirmation.
- CI (`.github/workflows/build-and-release.yml`): pushes/PRs to `main` build artifacts only; tags publish a GitHub Release.
