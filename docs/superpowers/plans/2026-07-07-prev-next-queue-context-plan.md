# Previous/Next Queue Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pass the current list's ringtone IDs as `queueIds` when navigating to PlayerScreen, so previous/next track navigation works within the source list context.

**Architecture:** Change `onRingtoneClick` from `(Ringtone) -> Unit` to `(Ringtone, queueIds: List<Long>) -> Unit` across all 5 caller screens + FeaturedRow component. Each screen passes `ringtones.map { it.id }` at its click sites. AppNavGraph wires the `queueIds` into `Route.Player()`.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose

## Global Constraints

- No new dependencies
- All KDoc updated for changed parameters
- Preview composables updated with new signatures
- Commit per task

---

### Task 1: FeaturedRow and HomeScreen signature change

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/components/FeaturedRow.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt`

**Interfaces:**
- Produces: `FeaturedRow.onRingtoneClick: (Ringtone, List<Long>) -> Unit` — consumed by HomeScreen callers
- Produces: `HomeScreen.onRingtoneClick: (Ringtone, List<Long>) -> Unit` — consumed by AppNavGraph (Task 3)

- [ ] **Step 1: Change FeaturedRow callback signature**

In `FeaturedRow.kt` line 83, change:
```kotlin
fun FeaturedRow(
    ringtones: List<Ringtone>,
    onRingtoneClick: (Ringtone) -> Unit,
    modifier: Modifier = Modifier
)
```
To:
```kotlin
fun FeaturedRow(
    ringtones: List<Ringtone>,
    onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier
)
```

In `FeaturedRow.kt` line 94, change:
```kotlin
onClick = { onRingtoneClick(ringtone) }
```
To:
```kotlin
onClick = { onRingtoneClick(ringtone, ringtones.map { it.id }) }
```

Update the Preview at line 170:
```kotlin
onRingtoneClick = { _, _ -> }
```

- [ ] **Step 2: Change HomeScreen callback signature**

In `HomeScreen.kt`, change the `onRingtoneClick` parameter (line 59):
```kotlin
onRingtoneClick: (Ringtone) -> Unit,
```
To:
```kotlin
onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
```

Update the 4 FeaturedRow call sites in HomeScreen.kt to pass `queueIds`. Each one already has a ringtone list — just pass the same list's IDs:

**FeaturedRow call 1** (featuredRingtones, around line 109):
```kotlin
FeaturedRow(
    ringtones = featuredRingtones,
    onRingtoneClick = { ringtone, queueIds ->
        onRingtoneClick(ringtone, queueIds)
    }
)
```
Wait, this just re-forwards. Since `onRingtoneClick` now matches, change:
```kotlin
FeaturedRow(
    ringtones = featuredRingtones,
    onRingtoneClick = onRingtoneClick
)
```

Same for all 4 FeaturedRow calls (lines ~109, ~126, ~143, ~160) — each becomes:
```kotlin
FeaturedRow(
    ringtones = <list>,
    onRingtoneClick = onRingtoneClick
)
```

Where `<list>` is `featuredRingtones`, `prokeralaRingtones`, `favoriteRingtones`, `recentPlays` respectively.

- [ ] **Step 3: Commit**

```bash
cd "E:\workspace\app\musix2025"
git add app/src/main/java/com/xxh/ringbones/presentation/home/components/FeaturedRow.kt app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt
git commit -m "feat: pass queueIds from FeaturedRow/HomeScreen for prev/next context"
```

---

### Task 2: SearchResult, ProkeralaList, Favorites, History signature changes

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/search/SearchResultScreen.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListScreen.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesScreen.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/history/HistoryScreen.kt`

**Interfaces:**
- Produces: Each screen's `onRingtoneClick: (Ringtone, List<Long>) -> Unit` — consumed by AppNavGraph (Task 3)

- [ ] **Step 1: Change SearchResultScreen**

Signature change (line 54):
```kotlin
onRingtoneClick: (Ringtone) -> Unit,
```
→
```kotlin
onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
```

Click site (line 152):
```kotlin
onClick = { onRingtoneClick(ringtone) }
```
→
```kotlin
onClick = { onRingtoneClick(ringtone, ringtones.map { it.id }) }
```

Update Preview (line 169):
```kotlin
onRingtoneClick = { _, _ -> }
```

- [ ] **Step 2: Change ProkeralaListScreen**

Read the file first. Find `fun ProkeralaListScreen(` signature and `onRingtoneClick: (Ringtone) -> Unit`. Change to:
```kotlin
onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
```

Find the click site inside the LazyColumn where `onRingtoneClick(ringtone)` is called. Change to:
```kotlin
onRingtoneClick(ringtone, ringtones.map { it.id })
```

Update the Preview callback to `{ _, _ -> }`.

- [ ] **Step 3: Change FavoritesScreen**

Signature change (line 60):
```kotlin
onRingtoneClick: (Ringtone) -> Unit,
```
→
```kotlin
onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
```

Click site (line 168):
```kotlin
onClick = { onRingtoneClick(ringtone) },
```
→
```kotlin
onClick = { onRingtoneClick(ringtone, ringtones.map { it.id }) },
```

Update Preview (line 183):
```kotlin
onRingtoneClick = { _, _ -> }
```

- [ ] **Step 4: Change HistoryScreen**

Signature change (line 60):
```kotlin
onRingtoneClick: (Ringtone) -> Unit,
```
→
```kotlin
onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
```

Click site (line 168):
```kotlin
onClick = { onRingtoneClick(ringtone) },
```
→
```kotlin
onClick = { onRingtoneClick(ringtone, ringtones.map { it.id }) },
```

Update Preview (line 181):
```kotlin
onRingtoneClick = { _, _ -> }
```

- [ ] **Step 5: Commit**

```bash
cd "E:\workspace\app\musix2025"
git add app/src/main/java/com/xxh/ringbones/presentation/search/SearchResultScreen.kt app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListScreen.kt app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesScreen.kt app/src/main/java/com/xxh/ringbones/presentation/history/HistoryScreen.kt
git commit -m "feat: pass queueIds from list screens for prev/next context"
```

---

### Task 3: AppNavGraph wiring

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt`

**Interfaces:**
- Consumes: Updated `onRingtoneClick: (Ringtone, List<Long>) -> Unit` from all screens (Tasks 1, 2)

- [ ] **Step 1: Update all Route.Player calls in AppNavGraph**

Find all `navController.navigate(Route.Player(ringtoneId = ringtone.id))` calls and update them to include `queueIds`.

**HomeScreen (around line 41-56):**
```kotlin
HomeScreen(
    onSearch = { query ->
        navController.navigate(Route.Search(query = query, byCategory = false))
    },
    onCategoryClick = { type ->
        navController.navigate(Route.Search(query = type, byCategory = true))
    },
    onRingtoneClick = { ringtone, queueIds ->
        navController.navigate(Route.Player(ringtoneId = ringtone.id, queueIds = queueIds))
    },
    onProkeralaSeeAll = {
        navController.navigate(Route.ProkeralaList)
    },
    onFavoritesSeeAll = {
        navController.navigate(Route.Favorites)
    },
    onPlayHistorySeeAll = {
        navController.navigate(Route.PlayHistory)
    },
    onDownloadsClick = {
        navController.navigate(Route.Downloads)
    }
)
```
Note: `onDownloadsClick` was added in previous work — keep it.

**SearchResultScreen (around line 63):**
```kotlin
SearchResultScreen(
    onRingtoneClick = { ringtone, queueIds ->
        navController.navigate(Route.Player(ringtoneId = ringtone.id, queueIds = queueIds))
    },
    onBackClick = {
        navController.popBackStack()
    }
)
```

**ProkeralaListScreen (around line 74):**
```kotlin
ProkeralaListScreen(
    onRingtoneClick = { ringtone, queueIds ->
        navController.navigate(Route.Player(ringtoneId = ringtone.id, queueIds = queueIds))
    },
    onBackClick = {
        navController.popBackStack()
    }
)
```

**FavoritesScreen (around line 97):**
```kotlin
FavoritesScreen(
    onRingtoneClick = { ringtone, queueIds ->
        navController.navigate(Route.Player(ringtoneId = ringtone.id, queueIds = queueIds))
    },
    onBackClick = {
        navController.popBackStack()
    }
)
```

**HistoryScreen (around line 109):**
```kotlin
HistoryScreen(
    onRingtoneClick = { ringtone, queueIds ->
        navController.navigate(Route.Player(ringtoneId = ringtone.id, queueIds = queueIds))
    },
    onBackClick = {
        navController.popBackStack()
    }
)
```

- [ ] **Step 2: Commit**

```bash
cd "E:\workspace\app\musix2025"
git add app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt
git commit -m "feat: wire queueIds through AppNavGraph for prev/next navigation"
```
