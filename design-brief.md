# Permission Alerts — design brief

Paste this into any AI design tool (v0, Galileo, Figma AI, Uizard, Claude, etc.)
to generate UI mockups and an app icon. Optionally attach a screenshot of the
current main screen for baseline context.

---

**Product:** Permission Alerts — an Android privacy-watchdog app. It scans every
installed app for *sensitive permissions* it has been granted (microphone,
camera, contacts, SMS, call log, location, phone, plus special perms like
accessibility, draw-over-other-apps, all-files storage) and alerts the user
whenever a new grant appears after a baseline was set. It does **not** revoke
permissions itself; it deep-links the user into Android Settings to do that
manually.

**Audience:** Privacy-conscious Android users, not security experts. Tone
should be calm-and-confident, not alarmist. Think *trustworthy guardian*, not
*antivirus warning screen*.

**Mental model:** baseline + diff. On first launch the app silently records
the current set of granted sensitive permissions as a baseline. Afterwards,
only *new* grants (an app gaining a sensitive perm it didn't have at baseline)
show up as "alerts." The user can **Accept** an alert (promotes that perm into
the baseline), **Manage** it (deep-links to that app's Settings page so they
can revoke), or **Ignore** the app entirely (stops watching it).

**Platform:** Android 8+, Kotlin + Jetpack Compose, Material 3 with Material
You dynamic color on Android 12+. Dark + light themes required. Adaptive
launcher icon (foreground vector on solid background).

**Deliverables requested:**

1. An **adaptive launcher icon** (foreground + background layers) — distinct
   from stock security-app clichés. Think shield/eye/radar, but distinctive.
2. A **notification icon** (single-color status-bar glyph, tintable white).
3. UI mockups for the screens below in light + dark + Material You
   dynamic-color variants.
4. A simple color palette and type scale, or confirmation to rely on MD3
   defaults.

**Screens to design:**

- **Intro / welcome screen (first launch only).** A short, explanatory
  onboarding that introduces the app — *not* a configuration wizard. It
  answers three questions in three concise panels (swipeable carousel or
  single scroll): (1) **What this app does** — "Know the moment any app on
  your phone gets access to your mic, camera, contacts, messages, or
  location." (2) **How it works** — "Permission Alerts quietly scans your installed
  apps in the background. If something gains new sensitive access, you get a
  notification. You decide what to do." (3) **What you can do about it** —
  "Tap 'Manage' on any app to jump to its settings and revoke access.
  Permission Alerts never revokes permissions itself." End with a single "Get
  started" button that takes the user to the main screen. No permissions,
  no config, no account — the app has already silently baselined the current
  state in the background. Use friendly illustrations or icon-based visuals
  rather than screenshots.
- **Main screen (the primary screen).** A top app bar with the app name and
  a "rescan" icon on the right. Below it, a header strip showing alert count
  and an "Accept all" button when alerts exist. Below that, a two-tab
  control: **User** (user-installed apps) / **System** (preinstalled apps),
  each labeled with a count. Below the tabs, a vertical list of app cards.
  Each card shows: app name, package name (small, muted), sensitive-permission
  chips (chips for "newly-granted" perms should visually stand out — these
  are the *alerts*), a Watch/Ignore toggle, a "Manage" text button that
  deep-links to that app's system settings, and an "Accept" button when the
  card has new-perm alerts. Cards with alerts should feel different from
  calm baseline cards but should not scream red.
- **Loading state** (briefly during scan).
- **Empty-tab state** (when a tab has no apps with sensitive perms).

**Persistent notification (ongoing, non-dismissable).** Posted when there are
alerts; updated by a background scanner every ~60s. Single summary
notification, inbox-style. Title: "N apps have new sensitive permissions."
Expanded body lists up to 6 flagged apps — app name + comma-separated perm
labels. Tapping opens the main screen. Small icon is a simple white glyph
(the security/watch mark).

**Categories to be aware of** (for chip grouping / color-coding if useful):
1. SMS / Contacts / Call log / Phone
2. Microphone & Camera
3. Location (foreground + background)
4. Special: Accessibility, Draw-over-apps, All-files storage

**What to avoid:**

- Red alarm aesthetic, siren/warning-triangle clichés.
- Generic "antivirus" shield-with-checkmark icon.
- Configuration-style onboarding (asking the user to pick apps, set up
  accounts, grant runtime permissions as part of setup). The intro screen is
  purely explanatory — the app requires zero setup decisions from the user.
