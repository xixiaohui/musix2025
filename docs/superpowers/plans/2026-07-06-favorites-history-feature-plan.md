# Favorites & Play History Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add favorites and play history UI features — home screen preview sections + dedicated full-list screens — reusing the existing data layer (DAOs, repositories, use cases) and following the Prokerala module pattern exactly.

**Architecture:** Reuses existing data layer without modification. Adds 4 new files (2 ViewModels + 2 Screens) and modifies 5 files (Routes, AppNavGraph, HomeViewModel, HomeScreen, strings.xml). Each feature mirrors the Prokerala pattern: ViewModel observes a Flow from a UseCase, Screen renders Scaffold + LazyColumn + RingtoneCard, Home previews via SectionHeader + FeaturedRow.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, Navigation Compose, Coroutines/Flow

## Global Constraints

- All code must compile
- KDoc on all new classes and public methods
- No Magic Numbers — use named constants
- All business logic in ViewModel; Composable must be lightweight
- Must include `@Preview` for new Composables
- Follow existing Clean Architecture patterns exactly
- Reuse existing UI components (`SectionHeader`, `FeaturedRow`, `RingtoneCard`, `LoadingIndicator`)
- Follow Prokerala module pattern (ProkeralaListViewModel / ProkeralaListScreen) as the exact template

---

### Task 1: Add String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: nothing
- Produces: `R.string.favorites`, `R.string.play_history`, `R.string.no_favorites_yet`, `R.string.no_play_history_yet`

- [ ] **Step 1: Add four string resources**

Add after the `<string name="prokerala">Prokerala</string>` line (line 52):

```xml
<string name="favorites">Favorites</string>
<string name="play_history">Play History</string>
<string name="no_favorites_yet">No favorites yet</string>
<string name="no_play_history_yet">No play history yet</string>
```

The exact insertion point in `strings.xml` is after line 52 (`<string name="prokerala">Prokerala</string>`) and before line 53 (blank line before `</resources>`).

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add favorites and play history string resources"
```

---

### Task 2: Add Navigation Routes

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/Routes.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `Route.Favorites` data object, `Route.PlayHistory` data object

- [ ] **Step 1: Add Favorites and PlayHistory routes**

Add after `Route.ProkeralaList` (line 33, before the closing `}` of the sealed class):

```kotlin
/** Favorites list screen — all user-favorited ringtones. */
@Serializable
data object Favorites : Route()

/** Play history screen — recently played ringtones. */
@Serializable
data object PlayHistory : Route()
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/navigation/Routes.kt
git commit -m "feat: add Favorites and PlayHistory navigation routes"
```

---

### Task 3: Create FavoritesViewModel

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesViewModel.kt`

**Interfaces:**
- Consumes: `GetFavoriteRingtonesUseCase.invoke(): Flow<List<Ringtone>>`
- Produces: `ringtones: StateFlow<List<Ringtone>>`, `isLoading: StateFlow<Boolean>`

- [ ] **Step 1: Create the file with package directory**

Create directory `app/src/main/java/com/xxh/ringbones/presentation/favorites/` and write `FavoritesViewModel.kt`:

```kotlin
package com.xxh.ringbones.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.usecase.GetFavoriteRingtonesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Favorites list screen.
 *
 * Observes all user-favorited ringtones from the database via
 * [GetFavoriteRingtonesUseCase]. The use case returns a reactive
 * [Flow] that emits whenever favorites are added or removed.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteRingtonesUseCase: GetFavoriteRingtonesUseCase
) : ViewModel() {

    /** Observable list of all favorited ringtones. */
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    /** Whether the initial data load is still in progress. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFavorites()
    }

    /** Observes favorited ringtones from the use case. */
    private fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getFavoriteRingtonesUseCase().collect { list ->
                    _ringtones.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesViewModel.kt
git commit -m "feat: add FavoritesViewModel"
```

---

### Task 4: Create FavoritesScreen

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesScreen.kt`

**Interfaces:**
- Consumes: `FavoritesViewModel` (ringtones, isLoading), `RingtoneCard`, `LoadingIndicator`
- Produces: Full list screen composable with back navigation

- [ ] **Step 1: Create FavoritesScreen**

```kotlin
package com.xxh.ringbones.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.common.LoadingIndicator
import com.xxh.ringbones.presentation.common.RingtoneCard

/** Vertical spacing between ringtone cards in the list. */
private val CARD_SPACING = 4.dp

/**
 * Full list screen displaying all user-favorited ringtones.
 *
 * Shows a top app bar with back navigation, a loading indicator while data
 * loads, an empty state message if no ringtones have been favorited, and a
 * scrollable list of [RingtoneCard] items when data is available.
 *
 * @param onRingtoneClick Called when a ringtone card is tapped, navigates to Player
 * @param onBackClick Called when the back arrow is pressed
 * @param viewModel Hilt-injected ViewModel providing favorited ringtone data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onRingtoneClick: (Ringtone) -> Unit,
    onBackClick: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtones by viewModel.ringtones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.favorites),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            ringtones.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_favorites_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(CARD_SPACING)
                ) {
                    itemsIndexed(ringtones) { _, ringtone ->
                        RingtoneCard(
                            ringtone = ringtone,
                            onClick = { onRingtoneClick(ringtone) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavoritesScreen() {
    MaterialTheme {
        FavoritesScreen(
            onRingtoneClick = {},
            onBackClick = {}
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesScreen.kt
git commit -m "feat: add FavoritesScreen with back navigation"
```

---

### Task 5: Create HistoryViewModel

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/history/HistoryViewModel.kt`

**Interfaces:**
- Consumes: `GetPlayHistoryUseCase.getRecentPlays(): Flow<List<Ringtone>>`
- Produces: `ringtones: StateFlow<List<Ringtone>>`, `isLoading: StateFlow<Boolean>`

- [ ] **Step 1: Create the file with package directory**

Create directory `app/src/main/java/com/xxh/ringbones/presentation/history/` and write `HistoryViewModel.kt`:

```kotlin
package com.xxh.ringbones.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.usecase.GetPlayHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Play History list screen.
 *
 * Observes recently played ringtones from the database via
 * [GetPlayHistoryUseCase]. The use case returns a reactive
 * [Flow] that emits whenever a new play event is recorded.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getPlayHistoryUseCase: GetPlayHistoryUseCase
) : ViewModel() {

    /** Observable list of recently played ringtones, most recent first. */
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    /** Whether the initial data load is still in progress. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHistory()
    }

    /** Observes recently played ringtones from the use case. */
    private fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getPlayHistoryUseCase.getRecentPlays().collect { list ->
                    _ringtones.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/history/HistoryViewModel.kt
git commit -m "feat: add HistoryViewModel"
```

---

### Task 6: Create HistoryScreen

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/history/HistoryScreen.kt`

**Interfaces:**
- Consumes: `HistoryViewModel` (ringtones, isLoading), `RingtoneCard`, `LoadingIndicator`
- Produces: Full list screen composable with back navigation

- [ ] **Step 1: Create HistoryScreen**

```kotlin
package com.xxh.ringbones.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.common.LoadingIndicator
import com.xxh.ringbones.presentation.common.RingtoneCard

/** Vertical spacing between ringtone cards in the list. */
private val CARD_SPACING = 4.dp

/**
 * Full list screen displaying recently played ringtones.
 *
 * Shows a top app bar with back navigation, a loading indicator while data
 * loads, an empty state message if no ringtones have been played, and a
 * scrollable list of [RingtoneCard] items when data is available.
 *
 * @param onRingtoneClick Called when a ringtone card is tapped, navigates to Player
 * @param onBackClick Called when the back arrow is pressed
 * @param viewModel Hilt-injected ViewModel providing play history data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onRingtoneClick: (Ringtone) -> Unit,
    onBackClick: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtones by viewModel.ringtones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.play_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            ringtones.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_play_history_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(CARD_SPACING)
                ) {
                    itemsIndexed(ringtones) { _, ringtone ->
                        RingtoneCard(
                            ringtone = ringtone,
                            onClick = { onRingtoneClick(ringtone) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHistoryScreen() {
    MaterialTheme {
        HistoryScreen(
            onRingtoneClick = {},
            onBackClick = {}
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/history/HistoryScreen.kt
git commit -m "feat: add HistoryScreen with back navigation"
```

---

### Task 7: Extend HomeViewModel with Favorites and Play History State

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeViewModel.kt`

**Interfaces:**
- Consumes: `GetFavoriteRingtonesUseCase`, `GetPlayHistoryUseCase`
- Produces: `favoriteRingtones: StateFlow<List<Ringtone>>`, `recentPlays: StateFlow<List<Ringtone>>`

- [ ] **Step 1: Replace the entire file**

The current file is at `E:/workspace/app/musix2025/app/src/main/java/com/xxh/ringbones/presentation/home/HomeViewModel.kt`. Replace its content with:

```kotlin
package com.xxh.ringbones.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import com.xxh.ringbones.domain.usecase.GetFavoriteRingtonesUseCase
import com.xxh.ringbones.domain.usecase.GetPlayHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Number of top ringtones to display in the featured section. */
private const val TOP_RINGTONE_LIMIT = 10

/** Number of prokerala ringtones to preview on the home screen. */
private const val PROKERALA_PREVIEW_LIMIT = 7

/** Number of items to preview in favorites and recent plays sections. */
private const val HOME_PREVIEW_LIMIT = 7

/** Domain string used to filter prokerala ringtones by URL. */
private const val PROKERALA_DOMAIN = "dl.prokerala.com"

/**
 * ViewModel for the Home screen.
 *
 * Observes category names from Room and loads ringtone counts per category,
 * top-played featured ringtones, prokerala ringtones, favorited ringtones,
 * and recently played ringtones.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ringtoneRepository: RingtoneRepository,
    private val getFavoriteRingtonesUseCase: GetFavoriteRingtonesUseCase,
    private val getPlayHistoryUseCase: GetPlayHistoryUseCase,
) : ViewModel() {

    /** Observable list of distinct category names from the database. */
    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    /** Category name → ringtone count mapping for the category grid. */
    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String, Int>> = _categoryCounts.asStateFlow()

    /** Observable list of top-played ringtones for the featured section. */
    private val _featuredRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val featuredRingtones: StateFlow<List<Ringtone>> = _featuredRingtones.asStateFlow()

    /** Observable list of recent ringtones across all categories. */
    private val _recentRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val recentRingtones: StateFlow<List<Ringtone>> = _recentRingtones.asStateFlow()

    /** Observable list of prokerala ringtones for the home section, capped at 7. */
    private val _prokeralaRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val prokeralaRingtones: StateFlow<List<Ringtone>> = _prokeralaRingtones.asStateFlow()

    /** Observable list of favorited ringtones for the home section, capped at 7. */
    private val _favoriteRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val favoriteRingtones: StateFlow<List<Ringtone>> = _favoriteRingtones.asStateFlow()

    /** Observable list of recently played ringtones for the home section, capped at 7. */
    private val _recentPlays = MutableStateFlow<List<Ringtone>>(emptyList())
    val recentPlays: StateFlow<List<Ringtone>> = _recentPlays.asStateFlow()

    init {
        loadCategories()
        loadCategoryCounts()
        loadFeatured()
        loadProkerala()
        loadFavorites()
        loadRecentPlays()
    }

    /** Observes distinct categories from Room. */
    private fun loadCategories() {
        viewModelScope.launch {
            ringtoneRepository.getDistinctCategories().collect { list ->
                _availableCategories.value = list
            }
        }
    }

    /** Observes ringtone counts grouped by category. */
    private fun loadCategoryCounts() {
        viewModelScope.launch {
            ringtoneRepository.getCategoryCounts().collect { map ->
                _categoryCounts.value = map
            }
        }
    }

    /** Observes top-played ringtones from Room. */
    private fun loadFeatured() {
        viewModelScope.launch {
            ringtoneRepository.getTopPlayed(TOP_RINGTONE_LIMIT).collect { list ->
                _featuredRingtones.value = list
            }
        }
    }

    /** Observes prokerala ringtones, capped at the preview limit. */
    private fun loadProkerala() {
        viewModelScope.launch {
            ringtoneRepository.getByUrlDomain(PROKERALA_DOMAIN).collect { list ->
                _prokeralaRingtones.value = list.take(PROKERALA_PREVIEW_LIMIT)
            }
        }
    }

    /** Observes favorited ringtones from the use case, capped at the preview limit. */
    private fun loadFavorites() {
        viewModelScope.launch {
            getFavoriteRingtonesUseCase().collect { list ->
                _favoriteRingtones.value = list.take(HOME_PREVIEW_LIMIT)
            }
        }
    }

    /** Observes recently played ringtones from the use case, capped at the preview limit. */
    private fun loadRecentPlays() {
        viewModelScope.launch {
            getPlayHistoryUseCase.getRecentPlays().collect { list ->
                _recentPlays.value = list.take(HOME_PREVIEW_LIMIT)
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/home/HomeViewModel.kt
git commit -m "feat: add favorites and play history state to HomeViewModel"
```

---

### Task 8: Add Favorites and Play History Sections to HomeScreen

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `HomeViewModel.favoriteRingtones`, `HomeViewModel.recentPlays`
- Produces: Two new sections in the LazyColumn (Favorites row + Recently Played row)

- [ ] **Step 1: Replace the entire file**

```kotlin
package com.xxh.ringbones.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.home.components.CategoryChipRow
import com.xxh.ringbones.presentation.home.components.CategoryGrid
import com.xxh.ringbones.presentation.home.components.FeaturedRow
import com.xxh.ringbones.presentation.home.components.HeroHeader

/** Unified horizontal padding for all home sections. */
private val HOME_HORIZONTAL = 24.dp

/** Vertical spacing between major sections. */
private val SECTION_SPACING = 28.dp

/**
 * Modern glassmorphism HomeScreen with dynamic gradient hero, animated category chips,
 * snap-scroll featured ringtones, prokerala section, favorites section, play history
 * section, and staggered category grid.
 *
 * All sections use a unified 24dp horizontal padding and consistent vertical
 * spacing rhythm for a polished, professional feel.
 *
 * @param onSearch Callback when user submits a search query
 * @param onCategoryClick Callback when a category chip or grid card is clicked
 * @param onRingtoneClick Callback when a featured ringtone card is tapped → navigates to Player
 * @param onProkeralaSeeAll Callback when user taps "See All" in the prokerala section
 * @param onFavoritesSeeAll Callback when user taps "See All" in the favorites section
 * @param onPlayHistorySeeAll Callback when user taps "See All" in the play history section
 * @param viewModel Hilt-injected ViewModel providing dynamic data
 */
@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onRingtoneClick: (Ringtone) -> Unit,
    onProkeralaSeeAll: () -> Unit,
    onFavoritesSeeAll: () -> Unit,
    onPlayHistorySeeAll: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val availableCategories by viewModel.availableCategories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val featuredRingtones by viewModel.featuredRingtones.collectAsState()
    val prokeralaRingtones by viewModel.prokeralaRingtones.collectAsState()
    val favoriteRingtones by viewModel.favoriteRingtones.collectAsState()
    val recentPlays by viewModel.recentPlays.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Hero Header ──
        item(key = "hero") {
            HeroHeader(onSearch = onSearch)
        }

        // ── Category Chips ──
        if (availableCategories.isNotEmpty()) {
            item(key = "section_categories") {
                SectionHeader(
                    title = stringResource(R.string.browse_categories),
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "chip_row") {
                CategoryChipRow(
                    categories = availableCategories,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        // ── Featured / Popular Now ──
        if (featuredRingtones.isNotEmpty()) {
            item(key = "section_popular") {
                SectionHeader(
                    title = stringResource(R.string.popular_now),
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "featured_row") {
                FeaturedRow(
                    ringtones = featuredRingtones,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Prokerala Ringtones ──
        if (prokeralaRingtones.isNotEmpty()) {
            item(key = "section_prokerala") {
                SectionHeader(
                    title = stringResource(R.string.prokerala),
                    onSeeAll = onProkeralaSeeAll,
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "prokerala_featured_row") {
                FeaturedRow(
                    ringtones = prokeralaRingtones,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Favorites ──
        if (favoriteRingtones.isNotEmpty()) {
            item(key = "section_favorites") {
                SectionHeader(
                    title = stringResource(R.string.favorites),
                    onSeeAll = onFavoritesSeeAll,
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "favorites_row") {
                FeaturedRow(
                    ringtones = favoriteRingtones,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Recently Played ──
        if (recentPlays.isNotEmpty()) {
            item(key = "section_recent_plays") {
                SectionHeader(
                    title = stringResource(R.string.play_history),
                    onSeeAll = onPlayHistorySeeAll,
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "recent_plays_row") {
                FeaturedRow(
                    ringtones = recentPlays,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Browse All Categories Grid ──
        if (categoryCounts.isNotEmpty()) {
            item(key = "section_browse") {
                SectionHeader(
                    title = stringResource(R.string.browse_all),
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "category_grid") {
                CategoryGrid(
                    categories = categoryCounts,
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

/**
 * Consistent section title with unified horizontal padding.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HOME_HORIZONTAL)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = stringResource(R.string.see_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt
git commit -m "feat: add favorites and play history sections to home screen"
```

---

### Task 9: Wire Navigation — AppNavGraph

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt`

**Interfaces:**
- Consumes: `Route.Favorites`, `Route.PlayHistory`, `FavoritesScreen`, `HistoryScreen`
- Produces: Two new composable destinations, new callbacks passed to HomeScreen

- [ ] **Step 1: Replace the entire file**

```kotlin
package com.xxh.ringbones.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.xxh.ringbones.presentation.favorites.FavoritesScreen
import com.xxh.ringbones.presentation.history.HistoryScreen
import com.xxh.ringbones.presentation.home.HomeScreen
import com.xxh.ringbones.presentation.player.PlayerScreen
import com.xxh.ringbones.presentation.prokerala.ProkeralaListScreen
import com.xxh.ringbones.presentation.search.SearchResultScreen

/**
 * Root navigation graph that wires all top-level destinations.
 *
 * Navigation flow:
 * - Home → Search/Category (via query param)
 * - Home → ProkeralaList / Favorites / PlayHistory (via See All)
 * - Any list → Player (via ringtone ID)
 * - Player can go back to previous screen
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Home) {
        // ── Home Screen ──
        composable<Route.Home> {
            HomeScreen(
                onSearch = { query ->
                    navController.navigate(Route.Search(query = query, byCategory = false))
                },
                onCategoryClick = { type ->
                    navController.navigate(Route.Search(query = type, byCategory = true))
                },
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onProkeralaSeeAll = {
                    navController.navigate(Route.ProkeralaList)
                },
                onFavoritesSeeAll = {
                    navController.navigate(Route.Favorites)
                },
                onPlayHistorySeeAll = {
                    navController.navigate(Route.PlayHistory)
                }
            )
        }

        // ── Search / Category Detail Screen ──
        composable<Route.Search> { backStackEntry ->
            backStackEntry.toRoute<Route.Search>()
            SearchResultScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Prokerala List Screen ──
        composable<Route.ProkeralaList> {
            ProkeralaListScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Favorites List Screen ──
        composable<Route.Favorites> {
            FavoritesScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Play History Screen ──
        composable<Route.PlayHistory> {
            HistoryScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Player Screen ──
        composable<Route.Player> { backStackEntry ->
            backStackEntry.toRoute<Route.Player>()
            PlayerScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt
git commit -m "feat: wire Favorites and PlayHistory routes into navigation graph"
```

---

### Task 10: Build Verification

- [ ] **Step 1: Clean build**

```bash
cd E:/workspace/app/musix2025 && ./gradlew assembleDebug 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all new files exist**

```bash
ls app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesViewModel.kt
ls app/src/main/java/com/xxh/ringbones/presentation/favorites/FavoritesScreen.kt
ls app/src/main/java/com/xxh/ringbones/presentation/history/HistoryViewModel.kt
ls app/src/main/java/com/xxh/ringbones/presentation/history/HistoryScreen.kt
```

- [ ] **Step 3: If build fails, fix compilation errors and re-verify**

---

## Task Dependencies

```
Task 1 (Strings)  ──┐
Task 2 (Routes)   ──┤
                    ├──► Task 3 (FavoritesVM) ──► Task 4 (FavoritesScreen) ──┐
                    │                                                         │
                    ├──► Task 5 (HistoryVM)   ──► Task 6 (HistoryScreen)   ──┤
                    │                                                         │
                    └──► Task 7 (HomeVM)      ──► Task 8 (HomeScreen)      ──┤
                                                                              │
                                              Task 9 (NavGraph) ──────────────┤
                                                                              │
                                              Task 10 (Build Verify) ◄────────┘
```

Tasks 1, 2 can run in parallel. Tasks 3, 5, 7 can run in parallel (after 1). Tasks 4, 6, 8 depend on their respective VMs. Task 9 depends on 2, 4, 6, 8. Task 10 is final verification.