# PermWatch

A privacy watchdog for Android. PermWatch keeps a calm eye on which apps hold
sensitive permissions — microphone, camera, contacts, SMS, call log, location,
phone, accessibility, draw-over-apps, all-files — and signals via a persistent
notification the moment a new grant appears.

It's an **auditor, not an enforcer**: PermWatch never revokes a permission. It
deep-links into Android Settings so you revoke manually, in one tap.

> Status: in active development. Kotlin + Jetpack Compose, Material 3,
> minSdk 26, targetSdk 35.

## What it does

- **Silent baseline on first install.** PermWatch snapshots whatever's already
  granted as your starting point — so the 50+ apps that legitimately use
  sensitive perms don't all alert at once.
- **Diff-driven alerts.** A new grant *above* baseline fires a heads-up
  notification with a count. Equal or lower counts update silently in the
  shade.
- **One-tap manage.** Each card has a "Manage" button that drops you into the
  Android Settings page for that app — revocation happens there.
- **Per-app ignore.** Mute apps you don't want to track without removing them
  from the picture.
- **Per-permission watch.** Settings screen lets you turn off categories of
  permissions you don't care about. Re-enabling silently accepts existing
  grants — only future grants signal.
- **Near-realtime.** A self-chaining `OneTimeWorkRequest` polls every 60 s
  (Android's `PeriodicWorkRequest` floor is 15 minutes — too coarse).
- **Holographic v2 UI.** Deep violet + iridescent sweep gradient + glass
  surfaces. Light and dark palettes, system-following.

## How it works

Three detection mechanisms live in `PermissionScanner`:

1. **Runtime perms** (mic, camera, contacts, …) — `PackageInfo
   .requestedPermissionsFlags & REQUESTED_PERMISSION_GRANTED`.
2. **App-op perms** (SYSTEM_ALERT_WINDOW, MANAGE_EXTERNAL_STORAGE) —
   `AppOpsManager.unsafeCheckOpNoThrow`.
3. **Accessibility services** — parsed out of
   `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`.

The scan output goes through `AlertDiff.compute(current, baseline, ignored,
watched)` — a pure function that filters to watched perms, subtracts the
baseline, drops ignored packages, and emits the flagged list. After every
scan, `PermsStore.pruneBaselineToCurrent` intersects the stored baseline with
what's actually granted now, so a perm you revoke and later re-grant counts
as new.

For the full architecture and the *why* behind each piece, see
[`CLAUDE.md`](./CLAUDE.md).

## Build

```bash
./build.sh build        # debug APK at app/build/outputs/apk/dev/debug/
./build.sh install      # build + adb install + launch on connected device
./build.sh clean
./build.sh release      # release APK (prod flavor)
```

`build.sh` sets `JAVA_HOME` (JBR 21) and `ANDROID_HOME`. Two flavors ship:
`dev` (`com.linetra.permwatch.dev`) and `prod` (`com.linetra.permwatch`).

To exercise the first-run silent-baseline path:

```bash
adb shell pm clear com.linetra.permwatch.dev
```

## Permissions PermWatch itself requests

- `QUERY_ALL_PACKAGES` — required on API 30+ to enumerate other apps' permission
  state. This is the product's core function.
- `POST_NOTIFICATIONS` — API 33+, requested at the moment you tap **Activate**
  on the intro screen.
- `RECEIVE_BOOT_COMPLETED` — declared but unused; WorkManager handles its own
  boot restore.

PermWatch does **not** request any of the sensitive permissions it watches.

## License

MIT — see [`LICENSE`](./LICENSE).
