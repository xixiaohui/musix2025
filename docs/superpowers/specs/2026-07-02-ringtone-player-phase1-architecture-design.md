# Phase 1: Clean Architecture 重构设计文档

> 日期: 2026-07-02
> 状态: 已确认
> 项目: 铃音播放器（Ringtone Player）

---

## 1. 概述

### 1.1 背景

现有项目 `musix2025` 是一个基于 Kotlin/Jetpack Compose 的 Android 铃音播放器，已有基本的播放、搜索、分类浏览功能。但代码架构存在以下问题：

- 无 Clean Architecture 分层，ViewModel 直接调用 DAO
- 使用手动单例 + ViewModelFactory，未使用 Hilt DI
- 导航使用字符串路由 + SavedStateHandle 传对象
- 数据库 Schema 设计简陋，缺少收藏/历史等核心表
- 部分 UI 使用 AndroidView 混合，未完全 Compose 化

### 1.2 目标

按 **Clean Architecture + MVVM + Hilt** 全面重构项目架构，为后续 UI 升级、功能扩展奠定坚实基础。

### 1.3 策略选择

- **策略**: 分阶段迭代（4 阶段完整交付）
- **模块化**: 单模块 + 包分层
- **导航**: 类型安全 Navigation Compose（`@Serializable` 路由）
- **数据库**: 全新 Schema 设计
- **Phase 1 实施**: 方案 B（水平分层: core → data → domain → presentation）

---

## 2. 目录结构

```
com.xxh.ringbones/
│
├── core/                              # 基础设施层
│   ├── di/
│   │   ├── AppModule.kt              # Database, DataStore, OkHttp
│   │   └── RepositoryModule.kt       # Repository 接口绑定
│   ├── theme/
│   │   ├── Color.kt                  # MD3 动态颜色 (light/dark)
│   │   ├── Type.kt                   # 字体系统
│   │   ├── Shape.kt                  # 圆角系统
│   │   └── Theme.kt                  # MusixTheme(darkTheme, dynamicColor)
│   ├── navigation/
│   │   ├── Routes.kt                 # @Serializable 路由密封类
│   │   └── AppNavGraph.kt            # NavHost 装配
│   ├── network/
│   │   └── HttpClient.kt             # OkHttp 单例
│   ├── datastore/
│   │   └── UserPreferences.kt        # 用户偏好（主题/缓存等）
│   └── util/
│       ├── Constants.kt
│       └── Extensions.kt
│
├── data/                              # 数据层
│   ├── local/
│   │   ├── entity/
│   │   │   ├── RingtoneEntity.kt
│   │   │   ├── FavoriteEntity.kt
│   │   │   ├── PlayHistoryEntity.kt
│   │   │   └── SearchHistoryEntity.kt
│   │   ├── dao/
│   │   │   ├── RingtoneDao.kt
│   │   │   ├── FavoriteDao.kt
│   │   │   ├── PlayHistoryDao.kt
│   │   │   └── SearchHistoryDao.kt
│   │   └── database/
│   │       └── AppDatabase.kt
│   ├── remote/
│   │   └── api/
│   │       └── RingtoneApiService.kt  # 在线资源 API（Phase 4）
│   ├── repository/
│   │   ├── RingtoneRepositoryImpl.kt
│   │   ├── FavoriteRepositoryImpl.kt
│   │   ├── PlayHistoryRepositoryImpl.kt
│   │   └── SearchHistoryRepositoryImpl.kt
│   └── mapper/
│       └── RingtoneMapper.kt          # Entity ↔ Domain Model
│
├── domain/                            # 领域层（纯 Kotlin）
│   ├── model/
│   │   ├── Ringtone.kt
│   │   ├── PlayHistory.kt
│   │   └── SearchHistory.kt
│   ├── repository/
│   │   ├── RingtoneRepository.kt     # 接口
│   │   ├── FavoriteRepository.kt
│   │   ├── PlayHistoryRepository.kt
│   │   └── SearchHistoryRepository.kt
│   └── usecase/
│       ├── SearchRingtonesUseCase.kt
│       ├── GetHomeCategoriesUseCase.kt
│       ├── GetFavoriteRingtonesUseCase.kt
│       ├── ToggleFavoriteUseCase.kt
│       ├── RecordPlayHistoryUseCase.kt
│       └── GetPlayHistoryUseCase.kt
│
├── presentation/                      # 表现层
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   └── components/
│   │       └── CategorySection.kt
│   ├── search/
│   │   ├── SearchResultScreen.kt
│   │   ├── SearchViewModel.kt
│   │   └── components/
│   │       └── RingtoneList.kt
│   ├── player/
│   │   ├── PlayerScreen.kt
│   │   ├── PlayerViewModel.kt
│   │   └── components/
│   │       └── PlayerControls.kt
│   └── common/
│       ├── RingtoneCard.kt
│       ├── SearchBar.kt
│       └── LoadingIndicator.kt
│
├── MainActivity.kt                    # @AndroidEntryPoint
└── MyApplication.kt                   # @HiltAndroidApp
```

---

## 3. 数据库 Schema

### 3.1 RingtoneEntity

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long (PK, autoGenerate) | 主键 |
| title | String (NOT NULL) | 标题 |
| author | String (NOT NULL) | 作者 |
| duration | String (NOT NULL) | 时长 |
| url | String (NOT NULL) | 网络地址 |
| mimeType | String (NOT NULL) | MIME 类型 |
| category | String (NOT NULL) | 业务分类 |
| coverImageUrl | String? | 封面图 |
| fileSize | Long | 文件大小 |
| downloadPath | String? | 本地缓存路径 |
| playCount | Int | 播放次数 |
| lastPlayedAt | Long | 最后播放时间戳 |
| createdAt | Long | 创建时间戳 |

### 3.2 FavoriteEntity

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long (PK) | 主键 |
| ringtoneId | Long (UNIQUE INDEX) | 关联 ringtones.id |
| favoritedAt | Long | 收藏时间戳 |

### 3.3 PlayHistoryEntity

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long (PK) | 主键 |
| ringtoneId | Long | 关联 ringtones.id |
| playedAt | Long | 播放时间戳 |
| playDuration | Long | 播放时长(ms) |

### 3.4 SearchHistoryEntity

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long (PK) | 主键 |
| query | String | 搜索关键字 |
| searchedAt | Long | 搜索时间戳 |

---

## 4. Domain 层设计

### 4.1 领域模型

`Ringtone`、`PlayHistory`、`SearchHistory` 均为纯 Kotlin data class，不含任何 Room 注解。

`Ringtone` 额外包含 `isFavorite: Boolean` 字段，由 Repository 层通过关联查询填充。

### 4.2 Repository 接口

```
RingtoneRepository     → searchByTitle, searchByCategory, getById, getAllPaged, insertAll, updatePlayCount
FavoriteRepository     → getFavorites, isFavorite, toggleFavorite, removeFavorite
PlayHistoryRepository  → getRecentPlays, getMostPlayed, record
SearchHistoryRepository → insertSearch, getRecentSearches, clearHistory
```

### 4.3 UseCase

| UseCase | 职责 |
|---|---|
| SearchRingtonesUseCase | 按 title 或 category 搜索 |
| GetHomeCategoriesUseCase | 聚合首页分类数据 |
| GetFavoriteRingtonesUseCase | 获取收藏列表 |
| ToggleFavoriteUseCase | 切换收藏状态 |
| RecordPlayHistoryUseCase | 播放时记录历史 |
| GetPlayHistoryUseCase | 获取最近/最常播放 |

### 4.4 依赖流向

```
Presentation → Domain (UseCase + Repository 接口) ← Data (Repository 实现)
                              ↑
                         纯 Kotlin
```

---

## 5. Core 层设计

### 5.1 Hilt DI

- `AppModule`: 提供 Database、DataStore、OkHttpClient (Singleton)
- `RepositoryModule`: 绑定 Repository 接口与实现
- `MyApplication`: `@HiltAndroidApp`
- `MainActivity`: `@AndroidEntryPoint`
- ViewModel 统一使用 `@HiltViewModel` + `@Inject`

### 5.2 Theme

- MD3 动态颜色，支持 Dark/Light Mode
- `MusixTheme(darkTheme: Boolean, dynamicColor: Boolean, content)`
- 完整 Type Scale + Shape Scale

### 5.3 Navigation

- `@Serializable sealed class Route`
- `Route.Home` / `Route.Search(query, byCategory)` / `Route.Player(ringtoneId)`
- Player 只传 `ringtoneId: Long`，ViewModel 通过 Repository 加载数据

---

## 6. 迁移策略

### 6.1 旧文件处理

| 文件 | 处理方式 |
|---|---|
| `data/Ringtone.kt` | 删除 → 重构为 `data/local/entity/RingtoneEntity.kt` |
| `data/RingtoneDao.kt` | 重写 → 新增 4 个 DAO |
| `data/AppDatabase.kt` | 重写 → 新增表 + Hilt 管理 |
| `data/DatabaseHelper.kt` | 删除 → 不再需要预置 DB |
| `data/RingtoneViewModel.kt` | 删除 → 按 feature 拆分 ViewModel |
| `data/RingtoneViewModelFactory.kt` | 删除 → Hilt 自动注入 |
| `data/MusixRingtonesList.kt` | 删除 → 种子数据改为 `core/util/SeedData.kt` |
| `media3/Media3PlayerView.kt` | 重写为 Compose 优先 |
| `media3/PlayerViewModel.kt` | 重构 → `presentation/player/PlayerViewModel.kt` |
| `network/HttpClient.kt` | 保留 → 移入 `core/network/` |
| `util/RingtoneHelper.kt` | 保留 → 移入 `core/util/` |
| `ui/theme/*` | 保留 → 移入 `core/theme/` |
| `ui/components/*` | 保留 → 移入 `presentation/common/` |
| `ui/screens/*` | 保留 → 按 feature 移入 `presentation/{feature}/` |
| `MainActivity.kt` | 重构 → 添加 Hilt + 新导航 |
| `MyApplication.kt` | 重构 → 添加 `@HiltAndroidApp` |

### 6.2 删除的 Activity

| 文件 | 说明 |
|---|---|
| `LandActivity.kt` | 已 staged as deleted |
| `PlayActivity.kt` | 已 staged as deleted |
| `SearchResultActivity.kt` | 已 staged as deleted |
| `PlayerControls.kt` | 已 staged as deleted |

### 6.3 保留的外部依赖

```
OkHttp, Room, Coil, Media3 ExoPlayer, Navigation Compose, 
Material 3, Firebase BOM, Gson, KSP
```

### 6.4 新增依赖

```
Hilt (hilt-android, hilt-compiler, hilt-navigation-compose)
DataStore (preferences)
```

---

## 7. 构建顺序（Phase 1）

```
Step 1: core/     → DI + Theme + Navigation + Network + DataStore + Util
Step 2: data/     → Entity + DAO + Database + Repository + Mapper
Step 3: domain/   → Model + Repository Interface + UseCase
Step 4: presentation/ → ViewModel + Screen + Component
Step 5: 集成验证 → 编译 + 运行 + 全功能回归
```

每步结束后代码必须可编译。Step 1-3 可仅通过单元测试验证，Step 4 开始可运行看 UI。

---

## 8. Phase 2-4 规划概要

| Phase | 内容 | 预计涉及 |
|---|---|---|
| Phase 2 | UI/UX 升级 | Glassmorphism 特效、动效系统、Dark/Light Mode 切换、Hero Animation、Mini Player、Bottom Sheet、完整 10+ 页面 |
| Phase 3 | 播放器增强 | MediaSession、通知栏控制、锁屏控制、Widget、音频可视化、铃声裁剪 |
| Phase 4 | 高级数据功能 | AI 推荐、在线资源库 (Supabase/REST API)、云同步、高级搜索、离线缓存 |

---

## 9. 约束与原则

- **禁止 XML** — 全部使用 Jetpack Compose
- **禁止 TODO/占位代码** — 所有代码必须完整实现
- **禁止重复代码** — 公共逻辑提取到 core/util 或 domain/usecase
- **禁止 Magic Number** — 常量统一在 `Constants.kt`
- **所有代码必须有 KDoc** — 类和方法级别
- **ViewModel 持有所有业务逻辑** — Composable 保持轻量纯 UI
- **保证 60FPS** — 使用 `remember`、`derivedStateOf`、`LazyColumn` 等优化