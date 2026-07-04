# Player Page UI Redesign — Design Spec

**Date:** 2026-07-04  
**Status:** Approved  
**Scope:** Pure Compose UI redesign — Apple Music glassmorphism style  
**Tech:** Jetpack Compose, Material 3, Coil 3.x, Media3 ExoPlayer, Hilt

---

## 1. Visual Direction

Apple Music-inspired glassmorphism with deep dark background, frosted-glass cards, generous
border-radius, and soft spatial layering. All functionality preserved from the current player.

- **Background:** `#0A0A0F` base + animated radial gradient (1500ms transitions on track change)
- **Glass cards:** `surface.copy(alpha = 0.08f–0.12f)` + Android 15 `blur` modifier (compileSdk 37)
- **Accent:** Material You dynamic `primary`
- **Text:** `onSurface` at 100% / 60% / 40% alpha tiers
- **Corners:** 16dp (cards), 28dp (album cover), 14dp (buttons)
- **Touch feedback:** ripple on all interactive elements

---

## 2. Layout (top-to-bottom)

```
┌─────────────────────────────────────┐
│  ← back                    ⋮ menu   │  Transparent TopBar, icon-only
├─────────────────────────────────────┤
│         Immersive Background        │  Radial gradient, animated
│  ┌─────────────────────────────┐    │
│  │   Album Cover (280dp, r28)  │    │  Coil / gradient fallback
│  │   glow shadow               │    │  SharedTransition support
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │  Glass card (surface α=0.08)
│  │  Track Title (bold)         │    │
│  │  Artist (muted)             │    │  key(ringtoneId) → fade-in
│  │  ═══════●═══════════        │    │  SeekBar: 3dp thin line
│  │  01:23           03:45      │    │
│  └─────────────────────────────┘    │
│                                     │
│        ⏮      ▶/⏸      ⏭         │  Circular, primary play btn
│                                     │
│  ┌─────────────────────────────┐    │  Glass card (surface α=0.10)
│  │  🔔  Set Ringtone           │    │
│  ├─────────────────────────────┤    │  Full-width stacked buttons
│  │  💾  Download               │    │  r16, h52, gap 10dp
│  └─────────────────────────────┘    │
│                                     │
│         ♡ Favorite   ≡ Queue       │  Icon row, 22dp
│                                     │
│  ┌─────────────────────────────┐    │  Glass card, collapsible
│  │  🔄 OFF  🔀 Shuffle  1.5x  │    │  2×3 grid of extras
│  │  ⏰ Timer  🎚 EQ  A⟷B      │    │  AnimatedVisibility
│  └─────────────────────────────┘    │
│                                     │
│  ▓▓▓▓▓▓▓ Spectrum ▓▓▓▓▓▓▓▓▓▓    │  Canvas bars, gradient fill
└─────────────────────────────────────┘
```

---

## 3. Component Specs

### 3.1 ImmersiveBackground (reuse existing, minor polish)
- Deep dark base + animated radial gradient
- Remove unused `colorScheme` variable (already done)

### 3.2 AlbumCover (reuse existing, minor polish)
- 280dp, corner radius 28dp
- Coil `coil3.compose.AsyncImage` with crossfade
- Gradient fallback + MusicNote icon when no cover URL
- `shadow(24dp, primary.copy(alpha=0.25f))` glow

### 3.3 TrackInfoCard (NEW — glass card wrapping TrackInfo + SeekBar)
- Glass container: `surface.copy(alpha = 0.08f)`, rounded 20dp
- Padding: 20dp all sides
- Contains TrackInfo + CustomSeekBar vertically

### 3.4 TrackInfo (reuse existing)
- Title: `headlineSmall`, bold, single-line overflow
- Artist: `bodyLarge`, `onSurface.copy(alpha=0.5f)`
- `key(trackKey)` wrapping `AnimatedVisibility` for track-change animation

### 3.5 CustomSeekBar (reuse existing, style tweak)
- Thin line: 3dp (down from 4dp)
- Unplayed: `onSurface.copy(alpha=0.15f)`
- Played: `primary`
- Thumb: 6dp circle, primary fill + white stroke
- Tap-to-seek + drag-to-seek preserved

### 3.6 PlaybackControls (reuse existing)
- Previous: 48dp circle, glass surface
- Play/Pause: 64dp circle, primary solid fill, AnimatedContent crossfade
- Next: 48dp circle, glass surface

### 3.7 ActionButtons (REWRITE — replace horizontal Button row with vertical glass card)
- Two stacked full-width buttons inside a shared glass card
- **Set Ringtone:** `Notifications` icon + "Set Ringtone" text
- **Download:** `Download` icon + "Download" text
- 52dp height, 16dp corners, 10dp gap
- Touch ripple on each button

### 3.8 FavoriteRow (NEW — replaces inline IconButton)
- Small icon row: ♡ Favorite toggle + ≡ Queue
- 22dp icons, muted `onSurface.copy(alpha=0.4f)`
- Favorite filled with primary when active

### 3.9 ExtraControls (reuse existing, inside glass card)
- Collapsible glass card, 2 rows × 3 columns grid
- Repeat cycle, Shuffle toggle, Speed selector
- Sleep timer dialog, EQ selector, A–B loop
- `AnimatedVisibility(expandVertically/shrinkVertically)`

### 3.10 SpectrumVisualizer (reuse existing)
- 40dp height, full width
- Gradient bars: bottom muted → top primary

### 3.11 QueueSheet (reuse existing)
- `ModalBottomSheet`, `surface.copy(alpha=0.95f)`
- Current track highlighted with primary + ▶ icon
- Tap any track to skip

### 3.12 Dialogs (reuse existing)
- SpeedSelector → AlertDialog
- SleepTimerDialog → AlertDialog
- EqSelector → ModalBottomSheet

---

## 4. Files to Modify

| File | Change |
|------|--------|
| `PlayerScreen.kt` | Full layout rewrite — glass card containers, vertical action buttons, new FavoriteRow |
| `CustomSeekBar.kt` | Style tweak: thinner track (3dp), muted unplayed color |
| `presentation/player/components/` | NEW: `TrackInfoCard.kt` — glass wrapper for TrackInfo + SeekBar |
| | NEW: `ActionCard.kt` — vertical glass card with SetRingtone + Download buttons |
| | NEW: `FavoriteRow.kt` — ♡ + ≡ icon row |

No files deleted. All existing components (TrackInfo, SeekBar, PlaybackControls, ExtraControls, SpectrumVisualizer, QueueSheet, dialogs, AlbumCover, ImmersiveBackground) are reused with minimal or no changes.

---

## 5. Non-Functional Requirements

- Pure Compose — zero `AndroidView`, zero XML
- All existing functionality preserved (download, set ringtone, favorite, queue, extras, visualizer)
- `material.icons.extended` already in dependencies
- `coil3.compose.AsyncImage` import (Coil 3.x)
- 4-positional padding form (BOM constraint)
- `colorScheme` access at `@Composable` top-level (no `remember{}` wrapper)