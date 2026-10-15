# Capsool Architecture Documentation

## Overview
**Capsool** is a clean-room, high-performance implementation of an interactive Dynamic Island for Android built on **Jetpack Compose + Material Design 3 Expressive**. It provides 100% feature parity with Material Capsule (MT Island), supporting multi-card carousel navigation, 5-way global gestures, hardware/system status tracking, and accessibility overlay rendering without requiring `SYSTEM_ALERT_WINDOW`.

---

## Directory & Package Structure

```
app/src/main/java/
├── com/iqbalwork/capsule/
│   ├── MainActivity.kt                      # Companion application entry point (3 tabs + preview)
│   ├── engine/
│   │   ├── accessibility/
│   │   │   └── CapsoolAccessibilityService.kt # TYPE_ACCESSIBILITY_OVERLAY service
│   │   ├── preferences/
│   │   │   └── CutoutPreferencesRepository.kt # SharedPreferences repository for calibration
│   │   ├── stack/
│   │   │   └── CardStackManager.kt          # Continuous carousel card stack manager
│   │   └── state/
│   │       ├── CapsuleState.kt              # Immutable state representation
│   │       ├── CapsuleStateEngine.kt        # Reactive state holder & Coroutine coordinator
│   │       ├── CapsuleStateReducer.kt       # Pure functional reducer
│   │       └── UserAction.kt                # 5-way gestures & system action signals
│   ├── model/
│   │   └── CapsuleModels.kt                 # Domain data models & enums
│   └── ui/
│       ├── CapsuleRootView.kt               # Root island view with 5-way gesture recognizer
│       ├── companion/
│       │   ├── AppSettingsTab.kt            # Diagnostics simulator & accessibility settings
│       │   ├── CapsuleSettingsTab.kt        # Cutout calibration & opacity settings
│       │   ├── CompanionMainScreen.kt       # Companion app shell with live island preview
│       │   └── FeaturesTab.kt               # Carousel card stack toggles & previews
│       ├── components/
│       │   ├── ExpandedChargingCard.kt      # Battery percentage, AC/USB/Wireless, temperature
│       │   ├── ExpandedMediaCard.kt         # Rotating album art, equalizer, scrubber, playback
│       │   ├── ExpandedNotificationCard.kt  # Avatar, app badges, inline reply, actions
│       │   ├── ExpandedShortcutsCard.kt     # App/system shortcuts, ChatGPT, barcode simulation
│       │   ├── ExpandedSliderCard.kt        # Volume (A15, A16, OneUI), ringer, brightness, torch
│       │   ├── ExpandedTimerCard.kt         # Countdown timer, +1m, pause/resume, Google Clock sync
│       │   ├── MiniCapsuleView.kt           # Idle compact pill with rotating disc & equalizer wave
│       │   └── RedTargetCalibratorView.kt   # Interactive #FF3B30 target for physical alignment
│       └── theme/
│           ├── CapsuleTokens.kt             # M3 Expressive tokens & FruitSpring curves
│           ├── Color.kt                     # Base color palette
│           ├── Theme.kt                     # Dynamic Material3 theme
│           └── Type.kt                      # Typography
└── com/iqbalf/capsule/ui/
    └── CapsuleRootView.kt                   # Forwarding facade for com.iqbalf import compatibility
```

---

## Core Subsystems

### 1. Accessibility Service Overlay (`CapsoolAccessibilityService`)
- Renders the interactive island using `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`.
- **Zero Permission Bypass**: Does NOT check `Settings.canDrawOverlays(this)` because accessibility overlay permissions are granted automatically by the accessibility service binding.
- Houses a custom `OverlayLifecycleOwner` providing `LifecycleOwner`, `SavedStateRegistryOwner`, and `ViewModelStoreOwner` to host Compose UI inside an Android `AccessibilityService`.

### 2. 5-Way Gesture Recognizer (`capsule5WayGestures`)
The root container implements a custom, non-conflicting gesture detection pipeline:
- **Single Tap**: Toggles Expand / Collapse (`UserAction.Tap`).
- **Double Tap**: Fast action trigger (Media Play/Pause or Torch toggle) (`UserAction.DoubleTap`).
- **Long Press**: Expands to full card with haptic feedback (`UserAction.LongPress`).
- **Swipe Left**: Cycles to the next card in `CardStackManager` (`UserAction.SwipeLeft`).
- **Swipe Right**: Cycles to the previous card in `CardStackManager` (`UserAction.SwipeRight`).

### 3. Card Stack Carousel (`CardStackManager` & `FruitSpring`)
- Maintains an active ordered stack of cards:
  1. `MEDIA`: Media Player
  2. `SLIDERS`: System Sliders (Volume, Brightness, Flashlight)
  3. `CHARGING`: Battery & Power Delivery
  4. `NOTIFICATION`: Heads-up Notifications with Inline Reply
  5. `TIMER`: Timer & Stopwatch (Google Clock sync)
  6. `SHORTCUTS`: Quick Shortcuts, ChatGPT launcher, Barcode visualizer
- Supports continuous cyclic carousel navigation (`nextCard()` and `previousCard()`).
- Applies `CapsuleTokens.FruitSpring` / `FruitSpringIntOffset` (`dampingRatio = 0.72f, stiffness = 380f`) for physics-based fluid card transitions.
