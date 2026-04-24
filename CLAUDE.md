# PermWatch

Android privacy watchdog. Scans every installed app for *sensitive permissions*
it has been granted (mic, camera, contacts, SMS, call log, location, phone,
plus special perms: accessibility, draw-over-apps, all-files) and alerts via a
persistent notification when a new grant appears. Does **not** revoke — deep-
links into Android Settings for manual revocation. Kotlin + Compose, Material
3, minSdk 26, targetSdk 35.

## Build & run

Always use `./build.sh` — it sets `JAVA_HOME` (JBR 21) and `ANDROID_HOME`.

| Command | Effect |
|---|---|
| `./build.sh build` | Debug APK → `app/build/outputs/apk/dev/debug/app-dev-debug.apk` |
| `./build.sh install` | Build + adb install + launch on connected device |
| `./build.sh clean` | `gradlew clean` |
| `./build.sh release` | Release APK (prod flavor) |

Flavors: `dev` (applicationIdSuffix `.dev`) and `prod`. The installed package
on-device for dev is `com.linetra.permwatch.dev`; Activity launch target is
`com.linetra.permwatch.MainActivity` (namespace stays flavor-agnostic).

Exercise the first-run silent-baseline path via:
```
adb shell pm clear com.linetra.permwatch.dev
```

## Architecture

```
com.linetra.permwatch
├── PermWatchApp.kt          Application — channel + ScanScheduler.ensureScheduled on startup
├── MainActivity.kt           Compose single-activity; onResume triggers vm.refresh()
├── data/
│   ├── SensitivePermissions.kt   Catalog: 23 perms across 4 categories
│   ├── InstalledAppPerms.kt      Scanner output model
│   ├── PermsStore.kt             DataStore ("perms") — baseline, ignored set, onboarded flag
│   └── AlertDiff.kt              Pure-function diff: current - baseline - ignored → FlaggedApp list
├── scanner/PermissionScanner.kt  PackageManager + AppOpsManager + Settings.Secure
├── worker/
│   ├── PermissionScanWorker.kt   CoroutineWorker — scan + notify + schedule next
│   └── ScanScheduler.kt          Self-chaining OneTimeWorkRequest wrapper
├── notify/AlertNotifier.kt       Single persistent inbox-style summary notification
└── ui/
    ├── MainViewModel.kt          AndroidViewModel — refresh/accept/ignore actions
    └── Screens.kt                AppScaffold + AppListWithTabs + AppCard + ...
```

## Core concepts

### Baseline + diff model

First run silently captures current granted sensitive perms as the baseline
(`PermsStore.acceptCurrentAsBaseline`). Alerts fire only on grants *above*
baseline. Without this, every pre-existing grant would alert at install — all
50+ apps already using sensitive perms would swamp the UI. Everything alert-
related in the codebase is derivation of this diff.

### Scheduling

Android's `PeriodicWorkRequest` floor is 15 minutes. For faster testing and
near-realtime alerts, `ScanScheduler` uses a **self-chaining
`OneTimeWorkRequest`** — the worker re-enqueues itself at the end of
`doWork()`. Interval lives in `ScanScheduler.INTERVAL_SECONDS` (currently 60).
WorkManager auto-restores the chain after reboot; no `BOOT_COMPLETED` receiver
needed.

### How perms are detected

Three different mechanisms, handled inside `PermissionScanner`:

- **Runtime perms** (mic, camera, contacts, etc.): `PackageInfo.requestedPermissions`
  + `requestedPermissionsFlags & REQUESTED_PERMISSION_GRANTED`.
- **Special app-op perms** (SYSTEM_ALERT_WINDOW, MANAGE_EXTERNAL_STORAGE):
  `AppOpsManager.unsafeCheckOpNoThrow(permissionToOp(perm), uid, pkg) == MODE_ALLOWED`.
- **Accessibility services**: the set of enabled services comes from
  `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (colon-separated `pkg/cls`
  strings). Accessibility is never surfaced as an app-op or manifest grant.

Adding a new sensitive permission: append to `SensitivePermissions.all` with
the right category and (if applicable) `isAppOp`/`isAccessibility` flag.

### No auto-revoke, by design

Android does not allow a non-system, non-device-owner app to revoke another
app's permissions. The "Manage" button on each card fires
`Settings.ACTION_APPLICATION_DETAILS_SETTINGS` with the target package — the
user revokes manually. Positioning: PermWatch is an **auditor**, not an
enforcer. (Bouncer owns the revoke-via-accessibility-service lane.)

## Manifest quirks

- `QUERY_ALL_PACKAGES` — required on API 30+ to enumerate other apps' perm
  state. Tagged `tools:ignore="QueryAllPackagesPermission"` because Play Store
  requires justification; our justification is the product's core function.
- `POST_NOTIFICATIONS` — API 33+ runtime, requested on first `onCreate`.
- `RECEIVE_BOOT_COMPLETED` — declared but not actively used; WorkManager
  handles its own boot restore.

## Design direction (v0.2 UI work)

- Brief: `design-brief.md` (intro + main + notification; tone = calm guardian,
  not alarmist antivirus).
- Two prototypes at `/opt/dev/PermWatch/`:
  - `PermWatch.jsx` — v1, **editorial/paper** aesthetic (Instrument Serif
    italic, Inter, JetBrains Mono; warm + amber; 4 themes inc. Material You).
  - `PermWatch v2.jsx` — v2, **holographic/future** aesthetic (Space Grotesk +
    IBM Plex Mono; dark violet + iridescent gradient; glassmorphism; animated
    "Iris" mark).
- Agreed direction: **hybrid** — adopt v2's Iris (as launcher + notification
  glyph) and voice ("Field is quiet", "Signal · since baseline", "Got it"),
  but keep v1's foundation (light + dark, Material You, solid surfaces,
  Instrument Serif for the `PermWatch` wordmark). Avoids v2's dark-only,
  minSdk-31-blur, and dating risks.
- Fonts to bundle: Instrument Serif (italic display), Inter (UI body),
  JetBrains Mono (metadata/package IDs).
- OKLCH values in the prototype need converting to sRGB `Color()` for Compose.

## Conventions

- Package namespace: `com.linetra.permwatch` — **not** `com.trogo.*` (see
  memory `project_package_namespace`).
- Code style: `kotlin.code.style=official` (gradle.properties); no comments
  unless the *why* is non-obvious.
- No unit tests yet — manual testing is the current loop (`./build.sh install`
  → interact on device → check logcat / notification shade).
- BuildConfig is globally disabled (`android.defaults.buildfeatures.buildconfig=false`);
  re-enable per-module if you need `BuildConfig.DEBUG` branching.

## Git

Per-repo credential helper is configured so pushes authenticate as the
`ivivek` GitHub account, regardless of which account `gh auth switch` is
currently active on. Commits authored as `Vivek K <vivek.oss@linetra.com>`.
