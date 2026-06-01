# Sleeper

A covert magic performance utility for Android. The app displays a convincingly black "sleeping phone" screen while secretly allowing the performer to input and reveal any playing card using only the hardware volume buttons.

## Architecture

### State Machine

The app uses a three-phase state machine managed by `SleeperViewModel`:

```
┌──────────────┐   Vol Down    ┌──────────────┐   Vol Up     ┌──────────────┐
│  VALUE_ENTRY  │─────────────▶│  SUIT_ENTRY   │────────────▶│   DISPLAY    │
│              │              │              │              │              │
│ Vol Up:      │              │ Vol Down:    │              │ Vol Up:      │
│  count value │              │  count suit  │              │  dismiss     │
└──────────────┘              └──────────────┘              │ Swipe L/R:  │
       ▲                                                    │  dismiss     │
       └────────────────────────────────────────────────────┘
                           reset
```

**States:**
- `VALUE_ENTRY` — Default start state. Volume Up increments the rank counter (1=Ace ... 13=King). Volume Down transitions to suit entry and begins counting at 1.
- `SUIT_ENTRY` — Volume Down increments the suit counter (1=Spades, 2=Hearts, 3=Clubs, 4=Diamonds). Volume Up confirms the card and transitions to display.
- `DISPLAY` — The selected card is rendered on screen. Volume Up or a horizontal swipe dismisses the card and resets back to `VALUE_ENTRY`.

### Volume Button Input

`SleeperActivity` overrides both `onKeyDown` and `onKeyUp` for `KEYCODE_VOLUME_UP` and `KEYCODE_VOLUME_DOWN`, returning `true` to fully consume these events. This prevents the system volume overlay from appearing and routes all presses through the state machine.

### Card Rendering

Cards are drawn programmatically using Jetpack Compose `Canvas` in `CardFace.kt`:

- **Proportions:** Standard poker card ratio of 2.5:3.5 (5:7)
- **Number cards:** Correct pip layouts for Ace through 10, with suit symbols drawn via Bézier `Path` operations (spades, hearts, clubs, diamonds)
- **Face cards (J/Q/K):** Bicycle-style court card designs with:
  - Mirrored double-ended layout (top/bottom halves are 180° rotations)
  - Crown/hat, face, robes, and held items (sword, scepter, leaf)
  - Traditional color scheme: gold accents, blue/red robes, cream background
  - Decorative borders and flourishes
- **Ace of Spades:** Ornate design with decorative rings and dot patterns (Bicycle signature)
- **Corner indices:** Serif-font rank + suit symbol in top-left and bottom-right corners

No bitmap assets are needed — the entire deck is rendered from code, making it easy to swap visual styles later.

### Gesture Handling

When a card is displayed, it supports multi-touch manipulation:

| Gesture | Action |
|---------|--------|
| One-finger drag | Move the card around the screen |
| Pinch (two fingers) | Zoom in/out (clamped 0.3x–5x) |
| Two-finger rotate | Rotate the card |
| Horizontal swipe (>100dp) | Dismiss the card and reset |
| Two-finger long press (1s) | Emergency reset (works in any state) |

Gestures are implemented using Compose `pointerInput` with `detectTransformGestures` for pan/zoom/rotate, and a custom `awaitEachGesture` handler for swipe detection and two-finger long press.

Transform state (position, scale, rotation) persists while the card is displayed and resets on dismiss.

### Immersive Fullscreen

The black screen illusion is maintained through multiple layers:

1. **Theme:** `android:Theme.Material.NoActionBar` with black window background, no animations
2. **Activity flags:** `FLAG_KEEP_SCREEN_ON`, sticky immersive mode via `WindowInsetsControllerCompat`
3. **Legacy fallback:** `SYSTEM_UI_FLAG_IMMERSIVE_STICKY` flags for older devices
4. **Re-application:** Immersive mode is re-applied in `onResume()` and `onWindowFocusChanged()`
5. **Compose layer:** Pure black (`#000000`) background on the root `Box`

### Haptic Feedback

Subtle vibration pulses for state transitions:

| Event | Duration | When |
|-------|----------|------|
| Suit mode entry | 30ms | First Volume Down press transitions from value to suit entry |
| Confirm | 50ms | Volume Up in suit mode confirms the card selection |
| Reset | 25ms | Card dismissed (swipe, Volume Up, or emergency reset) |

Haptic events flow from `SleeperViewModel` via a `StateFlow` and are consumed by the Activity.

## Project Structure

```
app/src/main/java/com/sleeper/app/
├── SleeperActivity.kt          # Main activity: immersive mode, volume handling, haptics
├── model/
│   ├── Card.kt                 # Rank, Suit, PlayingCard data classes
│   └── SleeperState.kt         # InputPhase enum, SleeperUiState, CardTransform
├── viewmodel/
│   └── SleeperViewModel.kt     # State machine logic
└── ui/
    ├── SleeperScreen.kt        # Root composable: black screen + card display + gestures
    └── card/
        ├── CardFace.kt         # Bicycle-style card renderer (Canvas drawing)
        └── PipLayout.kt        # Standard pip position definitions for each rank
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run lint
./gradlew lint
```

Requires Android SDK with API level 34. The project uses Kotlin 1.9.24, Jetpack Compose with BOM 2024.06.00, and Gradle 8.7.

## Requirements

- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Language:** Kotlin
- **UI:** Jetpack Compose

## Swapping Card Art

The card rendering is fully programmatic in `CardFace.kt`. To swap the visual style:

1. Modify the color constants at the top of `CardFace.kt` (`BICYCLE_RED`, `FACE_GOLD`, etc.)
2. Adjust the drawing functions (`drawCourtFigure`, `drawSpadePath`, etc.) for different aesthetics
3. Or replace `CardFace` entirely with an image-based renderer that loads bitmap assets by rank/suit
