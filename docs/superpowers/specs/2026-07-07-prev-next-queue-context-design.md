# Previous/Next Track Queue Context Design

## Overview

Fix previous/next track navigation on the Player screen. Currently all callers pass only `ringtoneId` (no `queueIds`), so the queue has a single track and previous/next are ineffective. Pass the current list's ringtone IDs as `queueIds` so the player can navigate within the list context.

## Approach

Change `onRingtoneClick` signature across all 5 caller screens from `(Ringtone) -> Unit` to `(Ringtone, queueIds: List<Long>) -> Unit`. Each screen passes its current ringtone list's IDs.

## Files

| File | Change |
|------|--------|
| `SearchResultScreen.kt` | Signature: `(Ringtone, List<Long>) -> Unit`. Pass `ringtones.map { it.id }` |
| `ProkeralaListScreen.kt` | Same pattern |
| `FavoritesScreen.kt` | Same pattern |
| `HistoryScreen.kt` | Same pattern |
| `HomeScreen.kt` | Signature: `(Ringtone, List<Long>) -> Unit`. 4 FeaturedRow callers each pass their respective list IDs |
| `AppNavGraph.kt` | All `Route.Player(...)` calls use `queueIds` parameter |
| `presentation/home/components/FeaturedRow.kt` | Update callback signature if it wraps the click |

## Edge Cases

- Empty list: `queueIds` is empty list, Player creates single-track queue (existing behavior)
- HomeScreen has 4 separate lists (featured, prokerala, favorites, recentPlays) — each passes its own IDs independently
