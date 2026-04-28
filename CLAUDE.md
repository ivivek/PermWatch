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
├── MainActivity.kt           Compose single-activity; switches Intro/scaffold on
│                             vm.onboarded; toggles Settings overlay; onResume
│                             triggers vm.refresh()
├── data/
│   ├── SensitivePermissions.kt   Catalog: 23 perms across 4 categories;
│   │                             watchedSet(unwatched) helper
│   ├── InstalledAppPerms.kt      Scanner output model
│   ├── PermsStore.kt             DataStore ("perms") — baseline, ignored set,
│   │                             unwatched perms, onboarded flag, lastAlertCount,
│   │                             intervalSeconds (scan cadence)
│   └── AlertDiff.kt              Pure-function diff: current ∩ watched − baseline
│                                 − ignored → FlaggedApp list
├── scanner/PermissionScanner.kt  PackageManager + AppOpsManager + Settings.Secure
├── worker/
│   ├── PermissionScanWorker.kt   CoroutineWorker — prune + scan + notify + schedule next
│   └── ScanScheduler.kt          Self-chaining OneTimeWorkRequest wrapper +
│                                 ScanCadence presets (1m → Daily)
├── notify/AlertNotifier.kt       HIGH-importance channel (perm_alerts_v2); heads-up only
│                                 when count escalates; large Iris icon + accent tint
└── ui/
    ├── MainViewModel.kt          AndroidViewModel — refresh/accept/ignore +
    │                             activate + setWatched (with silent re-baseline) +
    │                             setIntervalSeconds (persists + reschedules)
    ├── Intro.kt                  3-slide first-run onboarding (Signal/Change/You)
    ├── Settings.kt               Per-permission watch toggles + "How often"
    │                             summary row → ModalBottomSheet picker
    │                             (ScanCadence.presets) + "Ignored apps"
    │                             summary row → sheet to re-watch muted apps
    ├── Screens.kt                Hero + AlertStrip + Tabs + AppCard + chips + buttons
    ├── atoms/
    │   ├── Iris.kt               Animated sweep-gradient ring (configurable size/speed/still)
    │   └── Glass.kt              Translucent surface + hairline border (optional gradient)
    └── theme/
        ├── Color.kt              HoloDark + HoloLight palettes, LocalHolo CompositionLocal
        ├── Type.kt               Space Grotesk (variable) + IBM Plex Mono families
        └── Theme.kt              System-following dark/light, edge-to-edge insets

res/font/                         Bundled: space_grotesk_var, ibm_plex_mono_{regular,medium,bold}
res/drawable/
├── ic_iris_mark.xml              Full-bleed Iris on rounded violet — large icon + launcher source
├── ic_stat_iris.xml              Monochrome ring + dot — notification small icon
└── ic_launcher_{foreground,background}.xml   Adaptive launcher icon
```

## Core concepts

### Baseline + diff model

First run silently captures current granted sensitive perms as the baseline
(`PermsStore.acceptCurrentAsBaseline`). Alerts fire only on grants *above*
baseline. Without this, every pre-existing grant would alert at install — all
50+ apps already using sensitive perms would swamp the UI. Everything alert-
related in the codebase is derivation of this diff.

After every scan (UI refresh + worker tick), `PermsStore.pruneBaselineToCurrent`
intersects the stored baseline with what's actually granted now: revoked perms
drop out, uninstalled packages drop out. So a perm the user revokes in Settings
leaves the baseline immediately, and a later re-grant of that same perm counts
as new and fires an alert. Without prune, the baseline only ever grew, and
re-grants of previously-revoked perms were silently accepted.

### Watched permissions

`PermsStore.unwatched: Set<String>` is the user's opt-out list (manifest names),
empty by default — first install watches the full `SensitivePermissions.all`
catalog, so existing UX is unchanged. `SensitivePermissions.watchedSet(unwatched)
= all − unwatched` is the derived set passed into both `AlertDiff.compute` and
`MainViewModel.toRows`. An unwatched perm can't fire an alert *and* doesn't
appear on cards or in chip lists — it's invisible to the rest of the pipeline.

The Settings screen (gear icon in the main header) is a per-permission toggle
list grouped by the 4 categories. Toggling routes through
`MainViewModel.setWatched(perm, on)`:

- **Off → on (silent re-baseline):** trigger a scan, then
  `PermsStore.mergeIntoBaseline(perm, currentGrants)` adds `perm` to the
  baseline of every package that currently has it granted. Only *future* grants
  of that perm fire as new. Without this, every existing grant of a freshly-
  re-watched perm would alert at once.
- **On → off:** just persist the unwatched flag. The diff filters the perm out
  next refresh; baseline entries for it stay put (idempotent — they're either
  re-used on next toggle-on or pruned by `pruneBaselineToCurrent` if the perm
  is later revoked at the OS level).

The mental model: the unwatched window doesn't count as a "miss" — toggling on
is treated like a fresh first-install for that one permission.

### First-run gate

`MainViewModel.onboarded: StateFlow<Boolean?>` reflects the persisted flag —
`null` until the DataStore has emitted at least once (so `MainActivity` renders
a brief blank-on-bg splash rather than flashing the wrong screen on cold
start), then `false` (Intro) or `true` (scaffold).

`vm.refresh()` is a no-op while `!onboarded`, so onResume during the intro
doesn't kick off a scan. The Activate button on the last Intro slide calls
`vm.activate()`: snapshots current grants as the baseline, schedules the
worker, flips the flag, and triggers the first refresh. POST_NOTIFICATIONS
(API 33+) is requested from the same handler — the prompt lands when the user
opts in, not on `onCreate`.

### Scheduling

Android's `PeriodicWorkRequest` floor is 15 minutes. To allow sub-15m cadences,
`ScanScheduler` uses a **self-chaining `OneTimeWorkRequest`** — the worker
re-enqueues itself at the end of `doWork()`. Interval is user-configurable
(persisted in `PermsStore.intervalSeconds`, default
`PermsStore.DEFAULT_INTERVAL_SECONDS` = 900s = 15m). The user-facing presets
are in `ScanCadence.presets` (1m → Daily). `PermissionScanWorker` reads the
current interval from the store before calling
`ScanScheduler.scheduleNext(ctx, intervalSeconds)`, so changing cadence in
Settings takes effect at the next tick. `MainViewModel.setIntervalSeconds`
also calls `scheduleNext` immediately so the change isn't held up by the
in-flight delay. WorkManager auto-restores the chain after reboot; no
`BOOT_COMPLETED` receiver needed.

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
the right category and (if applicable) `isAppOp`/`isAccessibility` flag. It
becomes watched-by-default (any user not in `unwatched`) and shows up in the
Settings list automatically.

### Ignoring apps

The `WatchToggle` on each card mutes a single package — writes the package name
into `PermsStore.ignored`, which `AlertDiff` and `MainViewModel.toRows` both
filter out. Ignored apps **disappear from the main feed entirely** (the feed is
the signal stream — once you've reacted to a signal, it should clear). Tapping
ignore from a card shows a 5s "Ignored {label} · Undo" snackbar
(`SnackbarHost` in `AppScaffold`) so accidents are recoverable in the moment.
After that, the only way to re-watch is **Settings → Ignored apps**, a summary
row that opens a `ModalBottomSheet` listing every currently-ignored app still
present in the latest scan; toggling its switch routes through
`MainViewModel.toggleIgnore(pkg, false)`. The Settings sheet is skipped by the
snackbar wrapper — re-watching there *is* the undo, no need for a redundant
toast.

`MainViewModel.ignoredApps` is derived from `state.rows` (filtered to
`isIgnored`), so orphan entries (apps that were ignored then uninstalled) are
invisible in the sheet — they sit in the DataStore inertly.

### No auto-revoke, by design

Android does not allow a non-system, non-device-owner app to revoke another
app's permissions. The "Manage" button on each card fires
`Settings.ACTION_APPLICATION_DETAILS_SETTINGS` with the target package — the
user revokes manually. Positioning: PermWatch is an **auditor**, not an
enforcer. (Bouncer owns the revoke-via-accessibility-service lane.)

### Notification — calm summary, loud on change

A single ongoing notification (`NOTIF_ID_SUMMARY`, channel `perm_alerts_v2` at
`IMPORTANCE_HIGH`). On every refresh `AlertNotifier.updateSummary(flagged,
previousCount)` re-renders the body. If `flagged.size > previousCount` it first
`cancel()`s the live notification — the next `notify()` is then treated as a
new post and the channel's HIGH importance triggers heads-up + sound. Equal or
lower counts use `setOnlyAlertOnce(true)` so updates land silently in the
shade. `previousCount` lives in PermsStore as `lastAlertCount` and is written
after each notify by both the ViewModel and the worker.

Brand presence on the shade: `setLargeIcon` is fed a Bitmap rasterised from
`ic_iris_mark.xml` (the launcher icon Android caches can be stale; explicit
large-icon bypasses that). `setColor(0xFF8F8CFF)` tints the small icon and
chrome with `accentC` violet.

## Manifest quirks

- `QUERY_ALL_PACKAGES` — required on API 30+ to enumerate other apps' perm
  state. Tagged `tools:ignore="QueryAllPackagesPermission"` because Play Store
  requires justification; our justification is the product's core function.
- `POST_NOTIFICATIONS` — API 33+ runtime, requested on first `onCreate`.
- `RECEIVE_BOOT_COMPLETED` — declared but not actively used; WorkManager
  handles its own boot restore.

## Design — shipped v2 holographic

The shipped UI is the v2 holographic direction (deep violet + iridescent
sweep gradient + glass surfaces + Iris mark + Space Grotesk / IBM Plex Mono).
Both palettes ship: `HoloDark` for system dark, `HoloLight` for system light
(designer's lifted variant — accents desaturated and darkened, glass opacity
raised from 5–9% to 60–95% so it reads on paper). System light/dark choice
flips the palette; same components, no branching downstream.

Voice: "Field is quiet" / "Signal · since baseline" / "N apps have new
access" / "Got it" (instead of "Accept") / "Watching" / "Ignored".

Prototype source-of-truth at `/opt/dev/PermWatch/`:
- `PermWatch v2.jsx` — dark, what HoloDark is derived from.
- `PermWatch v2 Light.jsx` — light, what HoloLight is derived from.
- `Moodboard.html` / `Moodboard v2.html` — alternative directions explored
  but not shipped (Terminal, Brutalist, Nature, Newspaper, Muji, Risograph,
  Swiss, Cozy/Mascot, Holographic).

Conversion notes:
- OKLCH values are converted to sRGB hex via the python snippet that produced
  `Color.kt`. Re-run if palette shifts.
- Compose has no native sweep gradient with rotation animation; `Iris.kt`
  composes `Brush.sweepGradient` with `rememberInfiniteTransition` rotating
  the Box.
- Backdrop blur (the prototype's `backdrop-filter: blur`) is *not* implemented:
  Compose `Modifier.blur()` blurs the layer's own content, not what's behind.
  A real backdrop pass needs a separate render pipeline (background snapshot →
  blur → composite under glass). Translucent surfaces ship without blur and
  read fine.

Fonts bundled: Space Grotesk (variable TTF from google/fonts upstream) +
IBM Plex Mono (regular/medium/bold statics). ~547 KB total.

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
