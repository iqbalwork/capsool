# Capsool Architecture

Product scope, the full spec and the milestone plan live in [CAPSOOL_PRD_SPEC_PLAN_V3.md](CAPSOOL_PRD_SPEC_PLAN_V3.md). This file is a map of the code as it stands (end of M2).

## Data flow

```
System (accessibility events, receivers, later: listeners)
   │ SystemEvent
   ▼
CapsuleStore ── CapsuleReducer (pure) ──► Reduction(state, effects)
   │ StateFlow<CapsuleState>                  │ Effect
   ▼                                          ▼
Overlay (CapsuleRootView)               PlatformEffectHandler ──► Android APIs
Companion app (MainActivity)
   │ UserIntent
   └──────────────► CapsuleStore
```

- The reducer never calls Android APIs; it returns effects. The system's answer comes back as a `SystemEvent`.
- Settings live in DataStore (`SettingsRepository`) and reach the store as `SystemEvent.SettingsChanged`.
- Simulated events exist only in the `debug` source set (`debugtools/DiagnosticsPanel`); `release` ships an empty stub.

## Packages (`com.iqbalwork.capsule`)

| Package | Contents |
|---|---|
| `CapsoolApp`, `AppGraph` | Process singletons: store, settings, effect handler, service-running flag |
| `core.settings` | `Settings` model, `SettingsRepository` (DataStore), `SettingsMapper`, legacy prefs migration |
| `core.store` | `Action` (`UserIntent` / `SystemEvent`), `Effect`, `CapsuleState`, `CapsuleReducer`, `CapsuleStore` |
| `platform` | `PlatformEffectHandler`, `SystemStatus` (Advanced Protection check, settings intents) |
| `platform.media` | `MediaSource` (active sessions, transport controls), `MediaSessionSelector` (pure) |
| `platform.notification` | `CapsoolNotificationListener`, `NotificationSource` (parse + reply/read/dismiss/open), `NotificationFilter` (pure) |
| `platform.accessibility` | `CapsoolAccessibilityService`, `OverlayWindowController`, `OverlayPlacement` (pure geometry), `OverlayLifecycleOwner` |
| `ui.overlay` | `OverlayRoot`, `CapsuleRootView`, `capsuleGestures` |
| `ui.components` | Card composables (media, notification, charging, timer, sliders, shortcuts, mini capsule, calibration target) |
| `ui.companion` | Disclosure, status tab, capsule settings tab, main screen |
| `ui.theme` | `CapsuleTokens`, `CapsuleTheme` / `OverlayTheme` (dynamic color or brand palette) |
| `debugtools` | Diagnostics panel, real debug `MediaSession` and chat notifications (debug) / stub (release) |

## Overlay window

One `TYPE_ACCESSIBILITY_OVERLAY` window. On Android 14+ it stays card-sized and only its touchable region changes (pill / card / none), so it is never resized; on Android 12–13 it hugs the capsule while collapsed and is resized once to card size before expanding and once after collapsing. Either way touches outside the capsule reach the app below. Position comes from the display cutout path. When hidden the root view is set `INVISIBLE` and the window `NOT_TOUCHABLE`.
