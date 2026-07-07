# Favorites & Play History Feature Design

**Date:** 2026-07-06
**Branch:** main
**Feature:** 新增收藏和播放历史功能 — 首页预览 Section + 独立全量列表页

## Overview

为应用增加收藏（Favorites）和播放历史（Play History）两个功能模块。数据层（Entity、DAO、Repository、UseCase）已全部就绪，本设计仅涉及 UI 层集成。

架构模式完全遵循现有 Prokerala 模块：首页用 `SectionHeader(onSeeAll=)` + `FeaturedRow` 展示预览，点击"See All"进入独立全量列表页。

## Data Layer

**无需改动。** 以下组件已存在且功能完备：

| 组件 | 文件 |
|------|------|
| FavoriteEntity | `data/local/entity/FavoriteEntity.kt` |
| PlayHistoryEntity | `data/local/entity/PlayHistoryEntity.kt` |
| FavoriteDao | `data/local/dao/FavoriteDao.kt` |
| PlayHistoryDao | `data/local/dao/PlayHistoryDao.kt` |
| FavoriteRepository (interface) | `domain/repository/FavoriteRepository.kt` |
| PlayHistoryRepository (interface) | `domain/repository/PlayHistoryRepository.kt` |
| FavoriteRepositoryImpl | `data/repository/FavoriteRepositoryImpl.kt` |
| PlayHistoryRepositoryImpl | `data/repository/PlayHistoryRepositoryImpl.kt` |
| ToggleFavoriteUseCase | `domain/usecase/ToggleFavoriteUseCase.kt` |
| GetFavoriteRingtonesUseCase | `domain/usecase/GetFavoriteRingtonesUseCase.kt` |
| RecordPlayHistoryUseCase | `domain/usecase/RecordPlayHistoryUseCase.kt` |
| GetPlayHistoryUseCase | `domain/usecase/GetPlayHistoryUseCase.kt` |

PlayerScreen 已正确调用 `ToggleFavoriteUseCase`（通过 `RingtoneRepository.toggleFavorite()`）和 `RecordPlayHistoryUseCase`，无需修改。

## Presentation

### HomeViewModel (扩展)

新增两个状态流，注入已有 UseCase：

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ringtoneRepository: RingtoneRepository,
    private val getFavoriteRingtonesUseCase: GetFavoriteRingtonesUseCase,
    private val getPlayHistoryUseCase: GetPlayHistoryUseCase,
) : ViewModel() {

    /** Observable list of favorited ringtones, capped at preview limit. */
    private val _favoriteRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val favoriteRingtones: StateFlow<List<Ringtone>> = _favoriteRingtones.asStateFlow()

    /** Observable list of recently played ringtones, capped at preview limit. */
    private val _recentPlays = MutableStateFlow<List<Ringtone>>(emptyList())
    val recentPlays: StateFlow<List<Ringtone>> = _recentPlays.asStateFlow()

    init {
        // ... existing loads ...
        loadFavorites()
        loadRecentPlays()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            getFavoriteRingtonesUseCase().collect { list ->
                _favoriteRingtones.value = list.take(HOME_PREVIEW_LIMIT)
            }
        }
    }

    private fun loadRecentPlays() {
        viewModelScope.launch {
            getPlayHistoryUseCase.getRecentPlays().collect { list ->
                _recentPlays.value = list.take(HOME_PREVIEW_LIMIT)
            }
        }
    }
}
```

### HomeScreen (扩展)

在 Prokerala Section 之后、Browse All 之前插入两个 Section：

```
LazyColumn:
  HeroHeader
  Category Chips        (已有)
  Popular Now           (已有)
  Prokerala             (已有)
  Favorites             (新增) ← 有数据时才显示
  Recently Played       (新增) ← 有数据时才显示
  Browse All            (已有)
```

每个 Section 结构：
```kotlin
if (favoriteRingtones.isNotEmpty()) {
    item(key = "section_favorites") {
        SectionHeader(title = "Favorites", onSeeAll = onFavoritesSeeAll)
    }
    item(key = "favorites_row") {
        FeaturedRow(ringtones = favoriteRingtones, onRingtoneClick = onRingtoneClick)
    }
}
```

### FavoritesScreen (新增)

- `presentation/favorites/FavoritesViewModel.kt`
- `presentation/favorites/FavoritesScreen.kt`

```
TopAppBar: 返回按钮 + 标题 "Favorites"
LazyColumn + RingtoneCard 列表
数据源: GetFavoriteRingtonesUseCase()
空状态: "No favorites yet" 提示
点击 → Route.Player(ringtoneId)
```

### HistoryScreen (新增)

- `presentation/history/HistoryViewModel.kt`
- `presentation/history/HistoryScreen.kt`

```
TopAppBar: 返回按钮 + 标题 "Play History"
LazyColumn + RingtoneCard 列表
数据源: GetPlayHistoryUseCase.getRecentPlays()
空状态: "No play history yet" 提示
点击 → Route.Player(ringtoneId)
```

## Navigation

### New Routes

```kotlin
@Serializable data object Favorites : Route()
@Serializable data object PlayHistory : Route()
```

### Flow

```
Home → Favorites Section "See All"  → Route.Favorites → FavoritesScreen
Home → History Section "See All"    → Route.PlayHistory → HistoryScreen
FavoritesScreen → click item → Route.Player(ringtoneId)
HistoryScreen → click item → Route.Player(ringtoneId)
```

## String Resources

新增 4 个字符串：
- `favorites` — "Favorites"
- `play_history` — "Play History"
- `no_favorites_yet` — "No favorites yet"
- `no_play_history_yet` — "No play history yet"

## Files Summary

| Action | File |
|--------|------|
| **Add** | `presentation/favorites/FavoritesViewModel.kt` |
| **Add** | `presentation/favorites/FavoritesScreen.kt` |
| **Add** | `presentation/history/HistoryViewModel.kt` |
| **Add** | `presentation/history/HistoryScreen.kt` |
| **Modify** | `core/navigation/Routes.kt` |
| **Modify** | `core/navigation/AppNavGraph.kt` |
| **Modify** | `presentation/home/HomeViewModel.kt` |
| **Modify** | `presentation/home/HomeScreen.kt` |
| **Modify** | `app/src/main/res/values/strings.xml` |

## Error Handling

- 收藏/历史为空时，首页 Section 自动隐藏（`isNotEmpty()` guard）
- 列表页空数据时展示空状态提示
- 所有数据流使用 `try-catch`，异常时展示错误状态
- PlayerScreen 已有收藏/历史记录逻辑，无需额外处理

## Not In Scope

- `RingtoneCard` 上显示收藏图标 — 后续迭代
- 收藏/历史的删除/批量操作 — 后续迭代
- 云端同步 — Phase 4