# Copilot Instructions

## Build Commands

```bash
# Build the library AAR
./gradlew :upgrader:assembleRelease

# Build the test app
./gradlew :app:assembleDebug

# Run unit tests (upgrader module)
./gradlew :upgrader:test

# Run a single unit test class
./gradlew :upgrader:test --tests "com.ahao.upgrader.ExampleUnitTest"

# Run instrumented tests (requires connected device)
./gradlew :upgrader:connectedAndroidTest

# Full build
./gradlew build --no-daemon

# CI: write sdk.dir before building (no local.properties on CI)
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

## Architecture

This is a two-module Android project:

- **`:upgrader`** — the publishable Android library. Contains all update logic.
- **`:app`** — a Compose test harness UI that exercises the library. Not part of the published artifact.

### upgrader module internals

```
Upgrader (Builder)          ← public API, constructed with 4 required params
  ├── Service                ← creates Retrofit + OkHttpClient; injects x-access-token via interceptor
  │     └── ServiceApi       ← Retrofit interface, single POST /access/check endpoint
  ├── Models                 ← UpdateCheckRequest / UpdateCheckResponse / UpdateState / DownloadProgress
  ├── BaseResponse<T>        ← generic API envelope: { code, msg, data }
  ├── Constants              ← shared literals (no secrets; timeouts, MIME types, paths)
  └── Utils                  ← APK install (FileProvider), MD5, download dir, clean-up
```

**Update flow (all through `Upgrader`):**
1. `checkUpdate()` → POST `/access/check` → emits `UpdateState` via `StateFlow`
2. `downloadApk()` → raw OkHttp download with progress → emits `Downloading` / `DownloadCompleted`
3. `verifyApkFile()` → optional MD5 check
4. `installApk()` → FileProvider intent (Android N+) or `Uri.fromFile` (older)

## Key Conventions

### Upgrader must be built with Builder — no public constructor
All four parameters are mandatory; `build()` throws `IllegalArgumentException` if any is blank:
```kotlin
val upgrader = Upgrader.Builder(context)
    .accessToken("YOUR_TOKEN")
    .baseUrl("https://your-server.com")
    .project("project-name")
    .packageName("com.your.app")
    .build()
```

### Auth token is injected by OkHttp interceptor, never in annotations
`Service.createUpdateApi()` adds `x-access-token` via an `Interceptor`, not via `@Headers`. This is because `@Headers` only accepts compile-time constants. Do not revert to annotation-based headers.

### API response envelope
All server responses are wrapped in `BaseResponse<T>` (`code`, `msg`, `data`). Access the payload via `.data`, not the root response object. `BaseResponse.success()` returns `code == 0`.

### Serialization: kotlinx.serialization, not Gson/Moshi
Use `@Serializable` + `@SerialName("snake_case")` on all data classes. The Retrofit converter is `jakewharton-retrofit-serialization-converter`. Do not add Gson or Moshi.

### UpdateState is a sealed class — exhaustive when()
`UpdateState`: `Idle`, `Checking`, `UpdateAvailable`, `NoUpdateAvailable`, `Downloading`, `DownloadCompleted`, `Error`. Always handle all branches; the UI in `MainActivity` uses them all.

### Storage strategy is Android-version-aware
`Utils.getDownloadDir()` branches on SDK version (Q+, N–P, pre-N). When adding download-related features, maintain this pattern rather than hardcoding a single path.

### FileProvider authority
APK installs use `"${context.packageName}.provider"` as the FileProvider authority (defined by `Constants.FILEPROVIDER_AUTHORITY_SUFFIX`). The consuming app must declare a matching `<provider>` in its `AndroidManifest.xml`.

### compileSdk uses preview API syntax (AGP 9.x)
```kotlin
compileSdk {
    version = release(36) { minorApiLevel = 1 }
}
```
This is AGP 9.1.0 syntax. Do not flatten it to `compileSdk = 36`.

## Publishing

The library is published via JitPack. A GitHub Actions workflow (`.github/workflows/publish.yml`) triggers on `v*` tags:
1. Builds `:upgrader:assembleRelease`
2. Creates a GitHub Release with the AAR attached
3. Pre-warms JitPack for the tagged version

To release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

Consumers add:
```gradle
repositories { maven { url("https://jitpack.io") } }
implementation("com.github.YOUR_GITHUB_USERNAME:pms-upgrader:v1.0.0")
```

The `groupId` in `upgrader/build.gradle.kts` `afterEvaluate` block must match the actual GitHub username.
