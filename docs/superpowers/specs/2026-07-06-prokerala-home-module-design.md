# Prokerala Home Module Design

**Date:** 2026-07-06
**Branch:** main
**Feature:** 首页新增 Prokerala 铃音模块

## Overview

在首页新增 Prokerala 推荐模块，展示 7 个精选铃音，点击"更多"进入全量列表页。铃音数据来自 `assets/prokerala/ringtones.json`，支持播放、收藏等完整功能。

## Data Layer

### Seeder Extension

修改 `RingtoneJsonSeeder.seedFromAssets()`，扩展扫描目录：

- 当前仅扫描 `assets/jsonres/`
- 新增扫描 `assets/prokerala/`
- 复用现有 `JsonRingtoneModel`（字段完全匹配：title, author, time, url, type）
- 种子标记 key 升级为 `json_seeded_v6`，触发数据库重新入库

### DAO Query

`RingtoneDao` 新增方法：

```kotlin
@Query("SELECT * FROM ringtones WHERE url LIKE '%' || :domain || '%'")
fun getByUrlDomain(domain: String): Flow<List<RingtoneEntity>>
```

Prokerala 铃音 URL 均以 `dl.prokerala.com` 为域名，通过此查询可精确过滤。

### Repository

- `RingtoneRepository` 接口新增 `getByUrlDomain(domain: String): Flow<List<Ringtone>>`
- `RingtoneRepositoryImpl` 实现该方法，映射 Entity → Domain model

## Presentation

### HomeViewModel

新增 `prokeralaRingtones: StateFlow<List<Ringtone>>`，从 repository 获取前 7 条（在 Flow 上 `map { it.take(7) }`）。

### HomeScreen

在 CategoryGrid 之前插入 Prokerala Section：

- `SectionHeader("Prokerala", onSeeAll = { ... })` — 复用已有组件，激活 `onSeeAll` 参数
- `FeaturedRow(prokeralaRingtones, onRingtoneClick)` — 复用已有组件

### ProkeralaListScreen (新增)

- `Presentaion/prokerala/ProkeralaListScreen.kt`
- `Presentaion/prokerala/ProkeralaListViewModel.kt`
- TopAppBar: 返回按钮 + 标题 "Prokerala"
- LazyColumn + RingtoneCard 列表
- 数据源：`getByUrlDomain("dl.prokerala.com")` 全量数据
- 点击 → `Route.Player(ringtoneId)`

## Navigation

### New Route

```kotlin
@Serializable data object ProkeralaList : Route()
```

### Flow

```
Home → SectionHeader "More" → Route.ProkeralaList → ProkeralaListScreen
ProkeralaList → click item → Route.Player(ringtoneId)
```

## Files Changed

| Action | File |
|--------|------|
| Modify | `data/local/seeder/RingtoneJsonSeeder.kt` |
| Modify | `data/local/dao/RingtoneDao.kt` |
| Modify | `domain/repository/RingtoneRepository.kt` |
| Modify | `data/repository/RingtoneRepositoryImpl.kt` |
| Modify | `presentation/home/HomeViewModel.kt` |
| Modify | `presentation/home/HomeScreen.kt` |
| Modify | `core/navigation/Routes.kt` |
| Modify | `core/navigation/AppNavGraph.kt` |
| Add | `presentation/prokerala/ProkeralaListScreen.kt` |
| Add | `presentation/prokerala/ProkeralaListViewModel.kt` |

## Error Handling

- 若 prokerala JSON 解析失败，跳过该文件，不影响现有数据
- 若数据库无 prokerala 数据，首页 Section 自动隐藏（`isNotEmpty()` guard）
- 列表页空数据时展示空状态提示