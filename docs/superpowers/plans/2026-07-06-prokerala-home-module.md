# Prokerala Home Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Prokerala section to the home screen showing 7 ringtones with a "More" button navigating to a full list screen.

**Architecture:** Extend the Room seeder to scan `assets/prokerala/`, add a URL-domain DAO query for filtering, create a new ProkeralaList route/screen/ViewModel, and wire the home section using the existing `SectionHeader(onSeeAll=)` + `FeaturedRow` components.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, Navigation Compose, kotlinx.serialization

## Global Constraints

- All code must compile
- KDoc on all new classes and public methods
- No Magic Numbers — use named constants
- All business logic in ViewModel; Composable must be lightweight
- Must include `@Preview` for new Composables
- Follow existing Clean Architecture patterns exactly
- Reuse existing UI components (`SectionHeader`, `FeaturedRow`, `RingtoneCard`)

---

### Task 1: Extend Seeder to Scan prokerala/ Directory

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/data/local/seeder/RingtoneJsonSeeder.kt`

**Interfaces:**
- Consumes: nothing new
- Produces: `seedFromAssets()` now also parses `assets/prokerala/*.json`

- [ ] **Step 1: Add constants for the prokerala directory**

In `RingtoneJsonSeeder.kt`, add a companion object with the prokerala directory constant alongside the existing `JSON_ASSETS_DIR`:

```kotlin
/** Directory under assets/ that contains prokerala ringtone JSON. */
private const val PROKERALA_ASSETS_DIR = "prokerala"
```

- [ ] **Step 2: Modify seedFromAssets() to scan both directories**

Replace the single-directory scan loop in `seedFromAssets()` with logic that scans both `JSON_ASSETS_DIR` and `PROKERALA_ASSETS_DIR`. The cleanest approach: extract the per-directory parsing into a private helper and call it for both directories.

Full replacement of `seedFromAssets()`:

```kotlin
suspend fun seedFromAssets(): Int = withContext(Dispatchers.IO) {
    val assetManager = context.assets
    val allEntities = mutableListOf<RingtoneEntity>()

    // Seed from both asset directories
    allEntities.addAll(parseJsonFilesFrom(assetManager, JSON_ASSETS_DIR))
    allEntities.addAll(parseJsonFilesFrom(assetManager, PROKERALA_ASSETS_DIR))

    if (allEntities.isNotEmpty()) {
        ringtoneDao.deleteAll()
        ringtoneDao.insertAll(allEntities)
    }

    allEntities.size
}

/**
 * Parses all .json files in [dirPath] under assets into [RingtoneEntity] list.
 * Returns empty list if the directory does not exist or contains no JSON files.
 */
private fun parseJsonFilesFrom(
    assetManager: android.content.res.AssetManager,
    dirPath: String
): List<RingtoneEntity> {
    val fileNames = assetManager.list(dirPath) ?: return emptyList()

    val entities = mutableListOf<RingtoneEntity>()
    for (fileName in fileNames) {
        if (!fileName.endsWith(".json")) continue

        val filePath = "$dirPath/$fileName"
        val jsonString = assetManager.open(filePath).use { stream ->
            InputStreamReader(stream).readText()
        }

        val models = jsonParser.decodeFromString<List<JsonRingtoneModel>>(jsonString)

        models.mapTo(entities) { model ->
            RingtoneEntity(
                title = model.title,
                author = model.author,
                duration = model.time,
                url = model.url,
                mimeType = inferMimeType(model.url),
                category = model.type
            )
        }
    }
    return entities
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/data/local/seeder/RingtoneJsonSeeder.kt
git commit -m "feat: extend seeder to scan prokerala/ assets directory"
```

---

### Task 2: Bump Seed Flag to Trigger Re-seed

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/datastore/UserPreferences.kt`

**Interfaces:**
- Consumes: nothing new
- Produces: `JSON_SEEDED_V6` key replaces `JSON_SEEDED_V5`

- [ ] **Step 1: Add new seed flag**

Add a new key constant alongside the existing `JSON_SEEDED_V5` and update the `isJsonSeeded` / `setJsonSeeded` methods to reference it:

In `PreferenceKeys`:
```kotlin
/** Guard flag: true after JSON ringtone data has been seeded into Room. Bump suffix to force re-seed. */
val JSON_SEEDED_V6 = booleanPreferencesKey("json_seeded_v6")
```

In `UserPreferences` — update `isJsonSeeded`:
```kotlin
val isJsonSeeded: Flow<Boolean> = context.userPreferencesStore.data.map { preferences ->
    preferences[PreferenceKeys.JSON_SEEDED_V6] ?: false
}
```

In `UserPreferences` — update `setJsonSeeded`:
```kotlin
suspend fun setJsonSeeded(seeded: Boolean) {
    context.userPreferencesStore.edit { preferences ->
        preferences[PreferenceKeys.JSON_SEEDED_V6] = seeded
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/datastore/UserPreferences.kt
git commit -m "feat: bump seed flag to v6 for prokerala re-seed"
```

---

### Task 3: Add DAO Query for URL Domain Filtering

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/data/local/dao/RingtoneDao.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `fun getByUrlDomain(domain: String): Flow<List<RingtoneEntity>>`

- [ ] **Step 1: Add the query method**

Add to `RingtoneDao` interface, before the closing `}`:

```kotlin
/** Returns ringtones whose download URL contains the given domain string. */
@Query("SELECT * FROM ringtones WHERE url LIKE '%' || :domain || '%' ORDER BY id ASC")
fun getByUrlDomain(domain: String): Flow<List<RingtoneEntity>>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/data/local/dao/RingtoneDao.kt
git commit -m "feat: add getByUrlDomain query for prokerala ringtones"
```

---

### Task 4: Add Repository Interface + Implementation

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/domain/repository/RingtoneRepository.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/data/repository/RingtoneRepositoryImpl.kt`

**Interfaces:**
- Consumes: `RingtoneDao.getByUrlDomain(domain: String): Flow<List<RingtoneEntity>>`
- Produces: `fun getByUrlDomain(domain: String): Flow<List<Ringtone>>`

- [ ] **Step 1: Add interface method**

In `RingtoneRepository`, after `getCategoryCounts()`:

```kotlin
/** Returns ringtones whose download URL contains the given domain string. */
fun getByUrlDomain(domain: String): Flow<List<Ringtone>>
```

- [ ] **Step 2: Add implementation**

In `RingtoneRepositoryImpl`, after `getCategoryCounts()`:

```kotlin
override fun getByUrlDomain(domain: String): Flow<List<Ringtone>> =
    ringtoneDao.getByUrlDomain(domain).map { entities ->
        entities.map { RingtoneMapper.toDomain(it) }
    }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/domain/repository/RingtoneRepository.kt
git add app/src/main/java/com/xxh/ringbones/data/repository/RingtoneRepositoryImpl.kt
git commit -m "feat: add getByUrlDomain to repository for prokerala filtering"
```

---

### Task 5: Add ProkeralaList Route

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/Routes.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `Route.ProkeralaList` data object

- [ ] **Step 1: Add route**

In the `Route` sealed class, after `Player`:

```kotlin
/** Prokerala ringtone list screen. */
@Serializable
data object ProkeralaList : Route()
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/navigation/Routes.kt
git commit -m "feat: add ProkeralaList navigation route"
```

---

### Task 6: Create ProkeralaListViewModel

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListViewModel.kt`

**Interfaces:**
- Consumes: `RingtoneRepository.getByUrlDomain(domain: String)`
- Produces: `ringtones: StateFlow<List<Ringtone>>`, `isLoading: StateFlow<Boolean>`

- [ ] **Step 1: Create the file**

```kotlin
package com.xxh.ringbones.presentation.prokerala

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Domain string used to filter prokerala ringtones by URL. */
private const val PROKERALA_DOMAIN = "dl.prokerala.com"

/**
 * ViewModel for the Prokerala ringtone list screen.
 *
 * Loads all ringtones whose download URL contains the prokerala domain
 * from the Room database.
 */
@HiltViewModel
class ProkeralaListViewModel @Inject constructor(
    private val ringtoneRepository: RingtoneRepository
) : ViewModel() {

    /** Observable list of all prokerala ringtones. */
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    /** Whether data is still loading. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRingtones()
    }

    /** Observes prokerala ringtones from the repository. */
    private fun loadRingtones() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                ringtoneRepository.getByUrlDomain(PROKERALA_DOMAIN).collect { list ->
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
git add app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListViewModel.kt
git commit -m "feat: add ProkeralaListViewModel"
```

---

### Task 7: Create ProkeralaListScreen

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListScreen.kt`

**Interfaces:**
- Consumes: `ProkeralaListViewModel` (ringtones, isLoading), `RingtoneCard`, `LoadingIndicator`
- Produces: Full list screen composable with back navigation

- [ ] **Step 1: Create the file**

```kotlin
package com.xxh.ringbones.presentation.prokerala

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

/** Number of ringtones to show in the preview. */
private const val PREVIEW_COUNT = 5

/**
 * Full list screen displaying all prokerala ringtones.
 *
 * Shows a top app bar with back navigation, a loading indicator while data
 * loads, an empty state message if no ringtones are found, and a scrollable
 * list of [RingtoneCard] items when data is available.
 *
 * @param onRingtoneClick Called when a ringtone card is tapped, navigates to Player
 * @param onBackClick Called when the back arrow is pressed
 * @param viewModel Hilt-injected ViewModel providing prokerala ringtone data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProkeralaListScreen(
    onRingtoneClick: (Ringtone) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProkeralaListViewModel = hiltViewModel(),
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
                        text = stringResource(R.string.prokerala),
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
                        text = stringResource(R.string.no_ringtones_found),
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
private fun PreviewProkeralaListScreen() {
    MaterialTheme {
        ProkeralaListScreen(
            onRingtoneClick = {},
            onBackClick = {}
        )
    }
}
```

- [ ] **Step 2: Add the `prokerala` string resource**

The screen references `R.string.prokerala`. Add it to `app/src/main/res/values/strings.xml`:

```xml
<string name="prokerala">Prokerala</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListScreen.kt
git add app/src/main/res/values/strings.xml
git commit -m "feat: add ProkeralaListScreen with back navigation"
```

---

### Task 8: Update HomeViewModel with Prokerala State

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeViewModel.kt`

**Interfaces:**
- Consumes: `RingtoneRepository.getByUrlDomain("dl.prokerala.com")`
- Produces: `prokeralaRingtones: StateFlow<List<Ringtone>>` (capped at 7 items)

- [ ] **Step 1: Add constant**

Add next to `TOP_RINGTONE_LIMIT`:

```kotlin
/** Number of prokerala ringtones to preview on the home screen. */
private const val PROKERALA_PREVIEW_LIMIT = 7

/** Domain string used to filter prokerala ringtones by URL. */
private const val PROKERALA_DOMAIN = "dl.prokerala.com"
```

- [ ] **Step 2: Add state property**

Add after `recentRingtones` property:

```kotlin
/** Observable list of prokerala ringtones for the home section, capped at 7. */
private val _prokeralaRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
val prokeralaRingtones: StateFlow<List<Ringtone>> = _prokeralaRingtones.asStateFlow()
```

- [ ] **Step 3: Add loading method and call from init**

Add method after `loadFeatured()`:

```kotlin
/** Observes prokerala ringtones, capped at the preview limit. */
private fun loadProkerala() {
    viewModelScope.launch {
        ringtoneRepository.getByUrlDomain(PROKERALA_DOMAIN).collect { list ->
            _prokeralaRingtones.value = list.take(PROKERALA_PREVIEW_LIMIT)
        }
    }
}
```

In `init {}`, add `loadProkerala()` call:

```kotlin
init {
    loadCategories()
    loadCategoryCounts()
    loadFeatured()
    loadProkerala()
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/home/HomeViewModel.kt
git commit -m "feat: add prokerala ringtones state to HomeViewModel"
```

---

### Task 9: Add Prokerala Section to HomeScreen

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `HomeViewModel.prokeralaRingtones: StateFlow<List<Ringtone>>`
- Produces: New prokerala section in the LazyColumn

- [ ] **Step 1: Collect state and add callback parameter**

In `HomeScreen()`, add new parameter:
```kotlin
onProkeralaSeeAll: () -> Unit,
```

Add state collection after `featuredRingtones`:
```kotlin
val prokeralaRingtones by viewModel.prokeralaRingtones.collectAsState()
```

- [ ] **Step 2: Add prokerala section in LazyColumn**

Add between the `featured_row` and `section_browse` blocks:

```kotlin
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
```

- [ ] **Step 3: Update KDoc**

Add `@param onProkeralaSeeAll` to the KDoc comment:

```kotlin
 * @param onProkeralaSeeAll Callback when user taps "See All" in the prokerala section
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt
git commit -m "feat: add prokerala section to home screen with See All"
```

---

### Task 10: Wire Navigation in AppNavGraph

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt`

**Interfaces:**
- Consumes: `Route.ProkeralaList`, `ProkeralaListScreen`
- Produces: New composable destination in NavHost

- [ ] **Step 1: Add import**

```kotlin
import com.xxh.ringbones.presentation.prokerala.ProkeralaListScreen
```

- [ ] **Step 2: Pass onProkeralaSeeAll to HomeScreen**

In the `composable<Route.Home>` block, add the callback:

```kotlin
onProkeralaSeeAll = {
    navController.navigate(Route.ProkeralaList)
},
```

- [ ] **Step 3: Add ProkeralaList destination**

Add after the Search composable block, before Player:

```kotlin
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
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt
git commit -m "feat: wire ProkeralaList route into navigation graph"
```

---

### Task 11: Build Verification

- [ ] **Step 1: Clean build**

```bash
cd E:/workspace/app/musix2025 && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all files exist**

```bash
ls app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListScreen.kt
ls app/src/main/java/com/xxh/ringbones/presentation/prokerala/ProkeralaListViewModel.kt
```

- [ ] **Step 3: If build fails, fix compilation errors and re-verify**

---

## Task Dependencies

```
Task 1 (Seeder) ──┐
Task 2 (Flag)  ──┤
Task 3 (DAO)   ──┼──► Task 4 (Repository) ──┬──► Task 6 (ListViewModel) ──► Task 7 (ListScreen) ──┐
Task 5 (Route) ──┘                           │                                                      │
                                             └──► Task 8 (HomeViewModel) ──► Task 9 (HomeScreen) ──┤
                                                                                                    │
                                                                          Task 10 (NavGraph) ───────┤
                                                                                                    │
                                                                          Task 11 (Build Verify) ◄──┘
```

Tasks 1, 2, 3, and 5 can run in parallel. Task 4 depends on 3. Tasks 6 and 8 both depend on 4. Tasks 7 and 9 are leaf UI tasks. Task 10 depends on 5, 7, and 9. Task 11 is the final verification.