# Capsool Architecture

Product scope, the full spec and the milestone plan live in [CAPSOOL_PRD_SPEC_PLAN_V3.md](CAPSOOL_PRD_SPEC_PLAN_V3.md). This file is a map of the code as it stands (end of M0).

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
| `platform.accessibility` | `CapsoolAccessibilityService`, `OverlayWindowController`, `OverlayPlacement` (pure geometry), `OverlayLifecycleOwner` |
| `ui.overlay` | `OverlayRoot`, `CapsuleRootView`, `capsuleGestures` |
| `ui.components` | Card composables (media, notification, charging, timer, sliders, shortcuts, mini capsule, calibration target) |
| `ui.companion` | Disclosure, status tab, capsule settings tab, main screen |
| `ui.theme` | `CapsuleTokens`, `CapsuleTheme` / `OverlayTheme` (dynamic color or brand palette) |
| `debugtools` | Diagnostics panel (debug) / stub (release) |

## Overlay window

One `TYPE_ACCESSIBILITY_OVERLAY` window, sized to the capsule while collapsed so touches elsewhere reach the app below, resized once to card size before expanding and once after collapsing. Position comes from the display cutout path. When hidden the root view is set `INVISIBLE` and the window `NOT_TOUCHABLE`.
