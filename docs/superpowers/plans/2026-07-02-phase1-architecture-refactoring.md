# Phase 1: Clean Architecture 重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有项目重构为 Clean Architecture + MVVM + Hilt + 类型安全导航 + 全新 Room Schema，保留 3 个页面（首页/搜索/播放器）的现有功能。

**Architecture:** 水平分层构建 — core → data → domain → presentation → 集成。每层完成后可独立编译，第 4 层开始可运行看 UI。

**Tech Stack:** Kotlin 2.1.10, Jetpack Compose, Material 3, Hilt 2.51.1, Room 2.6.1, Media3 1.5.1, Navigation Compose 2.8.8, OkHttp 4.12.0, Coil 3.1.0, DataStore 1.1.1

## Global Constraints

- 禁止 XML — 全部使用 Jetpack Compose
- 禁止 TODO / 占位代码 — 所有代码必须完整实现
- 禁止重复代码 — 公共逻辑提取到 core/util 或 domain/usecase
- 禁止 Magic Number — 常量统一在 Constants.kt
- 所有 public 类和方法必须有 KDoc
- ViewModel 持有所有业务逻辑 — Composable 保持轻量纯 UI
- 每步结束后代码必须可编译
- 使用 libs.versions.toml 版本目录管理依赖

---

## 前置任务: 依赖与环境准备

### Task 0: 添加 Hilt 和 DataStore 依赖

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`

**Produces:** Hilt + DataStore 可用于后续所有任务

- [ ] **Step 1: 在 libs.versions.toml 中添加版本和库声明**

在 `[versions]` 下添加:
```toml
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
datastorePreferences = "1.1.1"
```

在 `[libraries]` 下添加:
```toml
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastorePreferences" }
```

在 `[plugins]` 下添加:
```toml
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "kotlin" }
```

- [ ] **Step 2: 在 build.gradle.kts 中添加 Hilt 插件**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false  // 新增
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.devtools.ksp") version "2.1.10-1.0.30" apply false
}
```

- [ ] **Step 3: 在 app/build.gradle.kts 中添加插件和依赖**

在 plugins 块添加:
```kotlin
id("com.google.devtools.ksp")
alias(libs.plugins.hilt)
```

在 dependencies 块添加:
```kotlin
// Hilt
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.hilt.navigation.compose)

// DataStore
implementation(libs.datastore.preferences)
```

- [ ] **Step 4: Sync 项目验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (可能有旧代码的 deprecated 警告，忽略)

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: add Hilt and DataStore dependencies"
```

---

## Layer 1: Core 基础设施层

### Task 1: 创建 Hilt Application 和 Activity

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/MyApplication.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/MainActivity.kt`
- Create: `app/src/main/java/com/xxh/ringbones/core/di/AppModule.kt`
- Create: `app/src/main/java/com/xxh/ringbones/core/di/RepositoryModule.kt`

**Produces:** `MyApplication` 标注 `@HiltAndroidApp`，`MainActivity` 标注 `@AndroidEntryPoint`，DI 模块骨架就位

- [ ] **Step 1: 更新 MyApplication.kt**

```kotlin
package com.xxh.ringbones

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point configured for Hilt dependency injection.
 */
@HiltAndroidApp
class MyApplication : Application()
```

- [ ] **Step 2: 更新 MainActivity.kt（骨架版本）**

```kotlin
package com.xxh.ringbones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Placeholder — 后续 Task 替换
        }
    }
}
```

- [ ] **Step 3: 创建 AppModule.kt**

```kotlin
package com.xxh.ringbones.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides application-scoped singleton dependencies:
 * database, DataStore, network client.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
```

- [ ] **Step 4: 创建 RepositoryModule.kt（骨架）**

```kotlin
package com.xxh.ringbones.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds repository interfaces to their data-layer implementations.
 * Populated in Tasks 11-13 after data + domain layers exist.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule
```

- [ ] **Step 5: 删除旧文件**

```bash
rm app/src/main/java/com/xxh/ringbones/data/RingtoneViewModelFactory.kt
rm app/src/main/java/com/xxh/ringbones/data/DatabaseHelper.kt
```

- [ ] **Step 6: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(core): add Hilt Application, Activity, and DI module skeletons"
```

---

### Task 2: 迁移和升级 Theme

**Files:**
- Move+Modify: `ui/theme/Color.kt` → `core/theme/Color.kt`
- Move: `ui/theme/Type.kt` → `core/theme/Type.kt`
- Move: `ui/theme/Shape.kt` → `core/theme/Shape.kt`
- Move+Modify: `ui/theme/Theme.kt` → `core/theme/Theme.kt`
- Delete: old `ui/theme/` directory

**Produces:** `MusixTheme` composable with `dynamicColor` parameter, ready for Dark/Light Mode switching

- [ ] **Step 1: 创建 core/theme/ 目录，移动 4 个文件**

```bash
mkdir -p app/src/main/java/com/xxh/ringbones/core/theme
mv app/src/main/java/com/xxh/ringbones/ui/theme/Color.kt app/src/main/java/com/xxh/ringbones/core/theme/
mv app/src/main/java/com/xxh/ringbones/ui/theme/Type.kt app/src/main/java/com/xxh/ringbones/core/theme/
mv app/src/main/java/com/xxh/ringbones/ui/theme/Shape.kt app/src/main/java/com/xxh/ringbones/core/theme/
mv app/src/main/java/com/xxh/ringbones/ui/theme/Theme.kt app/src/main/java/com/xxh/ringbones/core/theme/
rm -rf app/src/main/java/com/xxh/ringbones/ui/theme/
```

- [ ] **Step 2: 更新所有 4 个文件的 package 声明**

将 `package com.xxh.ringbones.ui.theme` 改为 `package com.xxh.ringbones.core.theme`

- [ ] **Step 3: 在 Theme.kt 中重命名顶层函数并添加 KDoc**

```kotlin
/**
 * Unified theme entry point with Material You dynamic color support.
 *
 * @param darkTheme Use dark color scheme when true, light when false
 * @param dynamicColor Enable Material You dynamic color on Android 12+
 * @param content Composable content
 */
@Composable
fun MusixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
```

将旧的 `Musix2025Theme` 函数替换为上述新签名。

- [ ] **Step 4: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (MainActivity 中引用了旧的 com.xxh.ringbones.ui.theme 所以可能报错，后续 Task 修复)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(core): migrate theme to core/theme with MusixTheme entry point"
```

---

### Task 3: 创建类型安全导航

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/navigation/Routes.kt`
- Create: `app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt`

**Produces:** `@Serializable` 路由密封类 + NavHost 装配，Player 路由只传 `ringtoneId: Long`

- [ ] **Step 1: 创建 Routes.kt**

```kotlin
package com.xxh.ringbones.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 *
 * Each route is a @Serializable data class or object that the Navigation
 * Compose library uses for compile-time safe route resolution.
 */
@Serializable
sealed class Route {
    /** Home screen — category browsing and search entry. */
    @Serializable
    data object Home : Route()

    /** Search results screen. */
    @Serializable
    data class Search(
        val query: String,
        val byCategory: Boolean = true
    ) : Route()

    /** Player screen — loads ringtone by ID from the repository. */
    @Serializable
    data class Player(val ringtoneId: Long) : Route()
}
```

- [ ] **Step 2: 创建 AppNavGraph.kt（骨架）**

```kotlin
package com.xxh.ringbones.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

/**
 * Root navigation graph that wires all top-level destinations.
 * Screens are filled in during presentation layer tasks.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home
    ) {
        composable<Route.Home> {
            // Placeholder — replaced by Task 15
            androidx.compose.material3.Text("Home")
        }
        composable<Route.Search> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Search>()
            // Placeholder — replaced by Task 16
            androidx.compose.material3.Text("Search: ${route.query}")
        }
        composable<Route.Player> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Player>()
            // Placeholder — replaced by Task 17
            androidx.compose.material3.Text("Player: ${route.ringtoneId}")
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（需要确保 navigation-compose 包含 kotlin-serialization 支持）

在 app/build.gradle.kts 的 plugins 中添加: `kotlin("plugin.serialization")`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(core): add type-safe navigation routes and NavGraph skeleton"
```

---

### Task 4: 创建 Network 和 DataStore

**Files:**
- Move+Modify: `network/HttpClient.kt` → `core/network/HttpClient.kt`
- Create: `app/src/main/java/com/xxh/ringbones/core/datastore/UserPreferences.kt`
- Delete: old `network/` directory

**Produces:** OkHttp 客户端 + DataStore 偏好存储（主题模式、音量等）

- [ ] **Step 1: 移动 HttpClient.kt**

```bash
mkdir -p app/src/main/java/com/xxh/ringbones/core/network
mv app/src/main/java/com/xxh/ringbones/network/HttpClient.kt app/src/main/java/com/xxh/ringbones/core/network/
rm -rf app/src/main/java/com/xxh/ringbones/network/
```

更新 package 为 `com.xxh.ringbones.core.network`。

- [ ] **Step 2: 创建 UserPreferences.kt**

```kotlin
package com.xxh.ringbones.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Top-level DataStore extension property — used only inside this file. */
private val Context.userPreferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * Key constants for user preference entries stored in DataStore.
 */
private object PreferenceKeys {
    val DARK_THEME = booleanPreferencesKey("dark_theme")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
}

/**
 * Manages user-level preferences backed by Jetpack DataStore (Preferences).
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Observable: whether the user has selected dark theme. */
    val isDarkTheme: Flow<Boolean> = context.userPreferencesStore.data.map { preferences ->
        preferences[PreferenceKeys.DARK_THEME] ?: false
    }

    /** Observable: whether dynamic color (Material You) is enabled. */
    val isDynamicColor: Flow<Boolean> = context.userPreferencesStore.data.map { preferences ->
        preferences[PreferenceKeys.DYNAMIC_COLOR] ?: true
    }

    /** Persist dark theme preference. */
    suspend fun setDarkTheme(enabled: Boolean) {
        context.userPreferencesStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_THEME] = enabled
        }
    }

    /** Persist dynamic color preference. */
    suspend fun setDynamicColor(enabled: Boolean) {
        context.userPreferencesStore.edit { preferences ->
            preferences[PreferenceKeys.DYNAMIC_COLOR] = enabled
        }
    }
}
```

- [ ] **Step 3: 更新 AppModule.kt 添加 DataStore provider**

在 `AppModule` 中添加:
```kotlin
@Provides
@Singleton
fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
    return UserPreferences(context)
}
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(core): add HttpClient migration, UserPreferences DataStore"
```

---

### Task 5: 创建工具类和移动 RingtoneHelper

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/util/Constants.kt`
- Create: `app/src/main/java/com/xxh/ringbones/core/util/Extensions.kt`
- Move+Modify: `util/RingtoneHelper.kt` → `core/util/RingtoneHelper.kt`
- Delete: old `util/` directory

**Produces:** 全局常量 + 通用扩展函数 + 铃声工具类移入 core

- [ ] **Step 1: 创建 Constants.kt**

```kotlin
package com.xxh.ringbones.core.util

/**
 * Application-wide constants. No magic numbers in business code.
 */
object Constants {
    /** Default pagination page size for Room PagingSource and API calls. */
    const val PAGE_SIZE = 20

    /** Maximum number of recent search history entries to keep. */
    const val MAX_SEARCH_HISTORY = 20

    /** Maximum number of recent play history entries to show. */
    const val MAX_RECENT_PLAYS = 50

    /** Maximum number of "most played" entries to compute. */
    const val MAX_MOST_PLAYED = 20

    /** Database file name. */
    const val DATABASE_NAME = "ringtones_v2.db"
}
```

- [ ] **Step 2: 创建 Extensions.kt**

```kotlin
package com.xxh.ringbones.core.util

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity

/**
 * Walk up the Context chain to find the hosting Activity.
 * Returns null if called from a non-Activity context (e.g. Service).
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
```

- [ ] **Step 3: 移动 RingtoneHelper.kt**

```bash
mkdir -p app/src/main/java/com/xxh/ringbones/core/util
mv app/src/main/java/com/xxh/ringbones/util/RingtoneHelper.kt app/src/main/java/com/xxh/ringbones/core/util/
rm -rf app/src/main/java/com/xxh/ringbones/util/
```

更新 package 为 `com.xxh.ringbones.core.util`，同时更新文件中引用的 `com.xxh.ringbones.network.HttpClient` 为 `com.xxh.ringbones.core.network.HttpClient`。

- [ ] **Step 4: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（如果 Media3PlayerView 引用了旧的 package，先忽略 — 后续 Task 处理）

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(core): add Constants, Extensions, migrate RingtoneHelper"
```

---

## Layer 2: Data 数据层

### Task 6: 创建 Room Entities

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/data/local/entity/RingtoneEntity.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/local/entity/FavoriteEntity.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/local/entity/PlayHistoryEntity.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/local/entity/SearchHistoryEntity.kt`
- Delete: `app/src/main/java/com/xxh/ringbones/data/Ringtone.kt` (old entity)

**Produces:** 4 张新表对应的 Room Entity 类

- [ ] **Step 1: 创建 RingtoneEntity.kt**

```kotlin
package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the ringtones table.
 * Represents a downloadable ringtone track.
 */
@Entity(tableName = "ringtones")
data class RingtoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val duration: String,
    val url: String,
    val mimeType: String,
    val category: String,
    val coverImageUrl: String? = null,
    val fileSize: Long = 0,
    val downloadPath: String? = null,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: 创建 FavoriteEntity.kt**

```kotlin
package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for user-favorited ringtones. One entry per ringtone. */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["ringtoneId"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ringtoneId: Long,
    val favoritedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: 创建 PlayHistoryEntity.kt**

```kotlin
package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room entity recording each ringtone playback event. */
@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ringtoneId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val playDuration: Long = 0
)
```

- [ ] **Step 4: 创建 SearchHistoryEntity.kt**

```kotlin
package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room entity for user search queries. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 5: 删除旧 Ringtone.kt**

```bash
rm app/src/main/java/com/xxh/ringbones/data/Ringtone.kt
```

- [ ] **Step 6: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（如果其他文件引用了旧 Ringtone 类，报错是正常的 — 后续 Task 逐个修复）

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(data): add Room entities for ringtones, favorites, history, search"
```

---

### Task 7: 创建 Room DAOs

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/data/local/dao/RingtoneDao.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/local/dao/FavoriteDao.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/local/dao/PlayHistoryDao.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/local/dao/SearchHistoryDao.kt`
- Delete: `app/src/main/java/com/xxh/ringbones/data/RingtoneDao.kt` (old DAO)

**Produces:** 4 个 DAO 接口，包含分页、搜索、聚合等查询

- [ ] **Step 1: 创建 RingtoneDao.kt**

```kotlin
package com.xxh.ringbones.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for the ringtones table. */
@Dao
interface RingtoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ringtones: List<RingtoneEntity>)

    @Query("SELECT * FROM ringtones ORDER BY id ASC")
    fun getAllPaged(): PagingSource<Int, RingtoneEntity>

    @Query("SELECT * FROM ringtones WHERE id = :id")
    fun getById(id: Long): Flow<RingtoneEntity?>

    @Query(
        "SELECT * FROM ringtones WHERE title LIKE :query OR author LIKE :query ORDER BY id ASC"
    )
    fun searchByTitle(query: String): Flow<List<RingtoneEntity>>

    @Query("SELECT * FROM ringtones WHERE category = :category ORDER BY id ASC")
    fun searchByCategory(category: String): Flow<List<RingtoneEntity>>

    @Query("UPDATE ringtones SET playCount = :count WHERE id = :id")
    suspend fun updatePlayCount(id: Long, count: Int)

    @Query("UPDATE ringtones SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    @Query("UPDATE ringtones SET downloadPath = :path WHERE id = :id")
    suspend fun updateDownloadPath(id: Long, path: String)
}
```

- [ ] **Step 2: 创建 FavoriteDao.kt**

```kotlin
package com.xxh.ringbones.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.FavoriteEntity
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE ringtoneId = :ringtoneId")
    suspend fun deleteByRingtoneId(ringtoneId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE ringtoneId = :ringtoneId)")
    fun isFavorite(ringtoneId: Long): Flow<Boolean>

    @Query("SELECT ringtoneId FROM favorites ORDER BY favoritedAt DESC")
    fun getFavoriteIds(): Flow<List<Long>>

    @Query("""
        SELECT r.* FROM ringtones r
        INNER JOIN favorites f ON r.id = f.ringtoneId
        ORDER BY f.favoritedAt DESC
    """)
    fun getFavoriteRingtones(): Flow<List<RingtoneEntity>>
}
```

- [ ] **Step 3: 创建 PlayHistoryDao.kt**

```kotlin
package com.xxh.ringbones.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.PlayHistoryEntity
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun insert(history: PlayHistoryEntity)

    @Query("""
        SELECT r.* FROM ringtones r
        INNER JOIN play_history ph ON r.id = ph.ringtoneId
        GROUP BY r.id
        ORDER BY MAX(ph.playedAt) DESC
        LIMIT :limit
    """)
    fun getRecentPlays(limit: Int): Flow<List<RingtoneEntity>>

    @Query("""
        SELECT r.* FROM ringtones r
        INNER JOIN play_history ph ON r.id = ph.ringtoneId
        GROUP BY r.id
        ORDER BY COUNT(ph.id) DESC
        LIMIT :limit
    """)
    fun getMostPlayed(limit: Int): Flow<List<RingtoneEntity>>

    @Query("""
        SELECT ph.* FROM play_history ph
        WHERE ph.ringtoneId = :ringtoneId
        ORDER BY ph.playedAt DESC
        LIMIT :limit
    """)
    fun getHistoryForRingtone(ringtoneId: Long, limit: Int = 20): Flow<List<PlayHistoryEntity>>
}
```

- [ ] **Step 4: 创建 SearchHistoryDao.kt**

```kotlin
package com.xxh.ringbones.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Insert
    suspend fun insert(history: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecentSearches(limit: Int): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
```

- [ ] **Step 5: 删除旧 DAO 和 MusixRingtonesList**

```bash
rm app/src/main/java/com/xxh/ringbones/data/RingtoneDao.kt
rm app/src/main/java/com/xxh/ringbones/data/MusixRingtonesList.kt
```

- [ ] **Step 6: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（除未更新的引用外）

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(data): add Room DAOs for ringtones, favorites, history, search"
```

---

### Task 8: 创建 AppDatabase

**Files:**
- Replace: `app/src/main/java/com/xxh/ringbones/data/AppDatabase.kt` → `data/local/database/AppDatabase.kt`
- Modify: `core/di/AppModule.kt` (add database provider)

**Produces:** Hilt-managed Room database with all 4 DAOs

- [ ] **Step 1: 重写 AppDatabase.kt**

```kotlin
package com.xxh.ringbones.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xxh.ringbones.core.util.Constants
import com.xxh.ringbones.data.local.dao.FavoriteDao
import com.xxh.ringbones.data.local.dao.PlayHistoryDao
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.local.dao.SearchHistoryDao
import com.xxh.ringbones.data.local.entity.FavoriteEntity
import com.xxh.ringbones.data.local.entity.PlayHistoryEntity
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import com.xxh.ringbones.data.local.entity.SearchHistoryEntity

/**
 * Room database holding all ringtone-related tables.
 * Managed by Hilt as a singleton.
 */
@Database(
    entities = [
        RingtoneEntity::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ringtoneDao(): RingtoneDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
```

- [ ] **Step 2: 更新 AppModule.kt 添加 Database provider**

```kotlin
@Provides
@Singleton
fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        Constants.DATABASE_NAME
    )
        .fallbackToDestructiveMigration()
        .build()
}
```

需要添加 import:
```kotlin
import androidx.room.Room
import com.xxh.ringbones.core.util.Constants
import com.xxh.ringbones.data.local.database.AppDatabase
```

- [ ] **Step 3: 删除旧 AppDatabase.kt**

```bash
rm app/src/main/java/com/xxh/ringbones/data/AppDatabase.kt
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(data): add Hilt-managed AppDatabase with 4 DAOs"
```

---

### Task 9: 创建 RingtoneMapper

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/data/mapper/RingtoneMapper.kt`

**Produces:** Entity ↔ Domain Model 双向映射

- [ ] **Step 1: 创建 RingtoneMapper.kt**

```kotlin
package com.xxh.ringbones.data.mapper

import com.xxh.ringbones.data.local.entity.RingtoneEntity
import com.xxh.ringbones.domain.model.Ringtone

/**
 * Maps between Room entity and domain model.
 * The domain model adds `isFavorite` which is resolved at the repository level.
 */
object RingtoneMapper {

    /** Convert Room entity to domain model, with favorite flag. */
    fun toDomain(entity: RingtoneEntity, isFavorite: Boolean = false): Ringtone = Ringtone(
        id = entity.id,
        title = entity.title,
        author = entity.author,
        duration = entity.duration,
        url = entity.url,
        mimeType = entity.mimeType,
        category = entity.category,
        coverImageUrl = entity.coverImageUrl,
        fileSize = entity.fileSize,
        downloadPath = entity.downloadPath,
        playCount = entity.playCount,
        lastPlayedAt = entity.lastPlayedAt,
        isFavorite = isFavorite
    )

    /** Convert domain model to Room entity. */
    fun toEntity(domain: Ringtone): RingtoneEntity = RingtoneEntity(
        id = domain.id,
        title = domain.title,
        author = domain.author,
        duration = domain.duration,
        url = domain.url,
        mimeType = domain.mimeType,
        category = domain.category,
        coverImageUrl = domain.coverImageUrl,
        fileSize = domain.fileSize,
        downloadPath = domain.downloadPath,
        playCount = domain.playCount,
        lastPlayedAt = domain.lastPlayedAt
    )
}
```

注意：此文件依赖 `domain.model.Ringtone`（Task 12 创建）。编译在本 Task 会失败，在 Task 12 完成后通过。

- [ ] **Step 2: Commit（先暂存，Task 12 后一并验证）**

```bash
git add data/mapper/RingtoneMapper.kt
git commit -m "feat(data): add RingtoneMapper for Entity ↔ Domain conversion"
```

---

### Task 10: 创建 Repository 实现

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/data/repository/RingtoneRepositoryImpl.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/repository/FavoriteRepositoryImpl.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/repository/PlayHistoryRepositoryImpl.kt`
- Create: `app/src/main/java/com/xxh/ringbones/data/repository/SearchHistoryRepositoryImpl.kt`

**Produces:** 4 个 repository 实现，实现 domain 层定义的接口

- [ ] **Step 1: 创建 RingtoneRepositoryImpl.kt**

```kotlin
package com.xxh.ringbones.data.repository

import androidx.paging.PagingSource
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.mapper.RingtoneMapper
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneRepositoryImpl @Inject constructor(
    private val ringtoneDao: RingtoneDao
) : RingtoneRepository {

    override fun searchByTitle(query: String): Flow<List<Ringtone>> =
        ringtoneDao.searchByTitle(query).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun searchByCategory(category: String): Flow<List<Ringtone>> =
        ringtoneDao.searchByCategory(category).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun getById(id: Long): Flow<Ringtone?> =
        ringtoneDao.getById(id).map { entity -> entity?.let { RingtoneMapper.toDomain(it) } }

    override fun getAllPaged(): PagingSource<Int, Ringtone> =
        ringtoneDao.getAllPaged().map { RingtoneMapper.toDomain(it) } as PagingSource<Int, Ringtone>

    override suspend fun insertAll(ringtones: List<Ringtone>) {
        ringtoneDao.insertAll(ringtones.map { RingtoneMapper.toEntity(it) })
    }

    override suspend fun updatePlayCount(id: Long, count: Int) {
        ringtoneDao.updatePlayCount(id, count)
    }

    override suspend fun updateLastPlayed(id: Long, timestamp: Long) {
        ringtoneDao.updateLastPlayed(id, timestamp)
    }
}
```

注意：`getAllPaged()` 的 PagingSource 映射在 Room 2.6 中可能不直接支持 `.map()`。需要用 `.map {}` + `as PagingSource` — 如果编译报错，改用 `by lazy {}` 包裹。

- [ ] **Step 2: 创建 FavoriteRepositoryImpl.kt**

```kotlin
package com.xxh.ringbones.data.repository

import com.xxh.ringbones.data.local.dao.FavoriteDao
import com.xxh.ringbones.data.local.entity.FavoriteEntity
import com.xxh.ringbones.data.mapper.RingtoneMapper
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<Ringtone>> =
        favoriteDao.getFavoriteRingtones().map { entities ->
            entities.map { RingtoneMapper.toDomain(it, isFavorite = true) }
        }

    override fun isFavorite(ringtoneId: Long): Flow<Boolean> =
        favoriteDao.isFavorite(ringtoneId)

    override suspend fun removeFavorite(ringtoneId: Long) {
        favoriteDao.deleteByRingtoneId(ringtoneId)
    }

    override suspend fun addFavorite(ringtoneId: Long) {
        favoriteDao.insert(FavoriteEntity(ringtoneId = ringtoneId))
    }
}
```

- [ ] **Step 3: 创建 PlayHistoryRepositoryImpl.kt**

```kotlin
package com.xxh.ringbones.data.repository

import com.xxh.ringbones.data.local.dao.PlayHistoryDao
import com.xxh.ringbones.data.local.entity.PlayHistoryEntity
import com.xxh.ringbones.data.mapper.RingtoneMapper
import com.xxh.ringbones.domain.model.PlayHistory
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayHistoryRepositoryImpl @Inject constructor(
    private val playHistoryDao: PlayHistoryDao,
    private val ringtoneDao: RingtoneDao  // for getById in recent plays
) : PlayHistoryRepository {

    override fun getRecentPlays(limit: Int): Flow<List<Ringtone>> =
        playHistoryDao.getRecentPlays(limit).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun getMostPlayed(limit: Int): Flow<List<Ringtone>> =
        playHistoryDao.getMostPlayed(limit).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override suspend fun record(ringtoneId: Long, duration: Long) {
        playHistoryDao.insert(
            PlayHistoryEntity(ringtoneId = ringtoneId, playDuration = duration)
        )
    }
}
```

需要添加 import `com.xxh.ringbones.data.local.dao.RingtoneDao`。

- [ ] **Step 4: 创建 SearchHistoryRepositoryImpl.kt**

```kotlin
package com.xxh.ringbones.data.repository

import com.xxh.ringbones.data.local.dao.SearchHistoryDao
import com.xxh.ringbones.data.local.entity.SearchHistoryEntity
import com.xxh.ringbones.domain.model.SearchHistory
import com.xxh.ringbones.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun getRecentSearches(limit: Int): Flow<List<SearchHistory>> =
        searchHistoryDao.getRecentSearches(limit).map { entities ->
            entities.map { entity ->
                SearchHistory(
                    id = entity.id,
                    query = entity.query,
                    searchedAt = entity.searchedAt
                )
            }
        }

    override suspend fun insertSearch(query: String) {
        searchHistoryDao.insert(SearchHistoryEntity(query = query))
    }

    override suspend fun clearHistory() {
        searchHistoryDao.clearAll()
    }
}
```

- [ ] **Step 5: 更新 RepositoryModule.kt 绑定接口**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRingtoneRepository(impl: RingtoneRepositoryImpl): RingtoneRepository

    @Binds
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    abstract fun bindPlayHistoryRepository(impl: PlayHistoryRepositoryImpl): PlayHistoryRepository

    @Binds
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository
}
```

- [ ] **Step 6: 删除旧 RingtoneViewModel.kt（依赖旧 DAO，编译会报错）**

```bash
rm app/src/main/java/com/xxh/ringbones/data/RingtoneViewModel.kt
```

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(data): add repository implementations with Hilt bindings"
```

注意：Task 10 的代码依赖 domain 层的接口（Task 13 创建），需在 domain 层完成后才能编译通过。当前先提交，后续 Task 验证。

---

## Layer 3: Domain 领域层

### Task 11: 创建 Domain Models

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/domain/model/Ringtone.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/model/PlayHistory.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/model/SearchHistory.kt`

**Produces:** 3 个纯 Kotlin domain models，无 Android/Room 依赖

- [ ] **Step 1: 创建 Ringtone.kt**

```kotlin
package com.xxh.ringbones.domain.model

/**
 * Domain model representing a ringtone track.
 * Pure Kotlin — no Room, no Android dependencies.
 */
data class Ringtone(
    val id: Long,
    val title: String,
    val author: String,
    val duration: String,
    val url: String,
    val mimeType: String,
    val category: String,
    val coverImageUrl: String? = null,
    val fileSize: Long = 0,
    val downloadPath: String? = null,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
    val isFavorite: Boolean = false
)
```

- [ ] **Step 2: 创建 PlayHistory.kt**

```kotlin
package com.xxh.ringbones.domain.model

/** Domain model for a playback history entry. */
data class PlayHistory(
    val id: Long,
    val ringtone: Ringtone,
    val playedAt: Long,
    val playDuration: Long
)
```

- [ ] **Step 3: 创建 SearchHistory.kt**

```kotlin
package com.xxh.ringbones.domain.model

/** Domain model for a search query history entry. */
data class SearchHistory(
    val id: Long,
    val query: String,
    val searchedAt: Long
)
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: domain model 纯 Kotlin，应该无误。data 层引用 domain 的地方报错会在 Task 12 后解决。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(domain): add Ringtone, PlayHistory, SearchHistory domain models"
```

---

### Task 12: 创建 Repository 接口

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/domain/repository/RingtoneRepository.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/repository/FavoriteRepository.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/repository/PlayHistoryRepository.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/repository/SearchHistoryRepository.kt`

**Produces:** 4 个 repository 接口 — data 层的实现类依赖这些接口

- [ ] **Step 1: 创建 RingtoneRepository.kt**

```kotlin
package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.flow.Flow

/** Contract for ringtone data access. Implemented by the data layer. */
interface RingtoneRepository {
    fun searchByTitle(query: String): Flow<List<Ringtone>>
    fun searchByCategory(category: String): Flow<List<Ringtone>>
    fun getById(id: Long): Flow<Ringtone?>
    fun getAllPaged(): androidx.paging.PagingSource<Int, Ringtone>
    suspend fun insertAll(ringtones: List<Ringtone>)
    suspend fun updatePlayCount(id: Long, count: Int)
    suspend fun updateLastPlayed(id: Long, timestamp: Long)
}
```

- [ ] **Step 2: 创建 FavoriteRepository.kt**

```kotlin
package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.flow.Flow

/** Contract for favorite ringtone management. */
interface FavoriteRepository {
    fun getFavorites(): Flow<List<Ringtone>>
    fun isFavorite(ringtoneId: Long): Flow<Boolean>
    suspend fun addFavorite(ringtoneId: Long)
    suspend fun removeFavorite(ringtoneId: Long)
}
```

- [ ] **Step 3: 创建 PlayHistoryRepository.kt**

```kotlin
package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.flow.Flow

/** Contract for playback history tracking. */
interface PlayHistoryRepository {
    fun getRecentPlays(limit: Int): Flow<List<Ringtone>>
    fun getMostPlayed(limit: Int): Flow<List<Ringtone>>
    suspend fun record(ringtoneId: Long, duration: Long)
}
```

- [ ] **Step 4: 创建 SearchHistoryRepository.kt**

```kotlin
package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.SearchHistory
import kotlinx.coroutines.flow.Flow

/** Contract for search history persistence. */
interface SearchHistoryRepository {
    fun getRecentSearches(limit: Int): Flow<List<SearchHistory>>
    suspend fun insertSearch(query: String)
    suspend fun clearHistory()
}
```

- [ ] **Step 5: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — domain 和 data 层现在应该全部可编译

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(domain): add repository interfaces"
```

---

### Task 13: 创建 UseCases

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/domain/usecase/SearchRingtonesUseCase.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/usecase/GetHomeCategoriesUseCase.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/usecase/GetFavoriteRingtonesUseCase.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/usecase/ToggleFavoriteUseCase.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/usecase/RecordPlayHistoryUseCase.kt`
- Create: `app/src/main/java/com/xxh/ringbones/domain/usecase/GetPlayHistoryUseCase.kt`

**Produces:** 6 个 use case 类，ViewModel 通过这些类调用业务逻辑

- [ ] **Step 1: 创建 SearchRingtonesUseCase.kt**

```kotlin
package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Searches ringtones by title (fuzzy) or by category (exact match).
 */
class SearchRingtonesUseCase @Inject constructor(
    private val ringtoneRepository: RingtoneRepository
) {
    operator fun invoke(query: String, byCategory: Boolean): Flow<List<Ringtone>> =
        if (byCategory) {
            ringtoneRepository.searchByCategory(query)
        } else {
            ringtoneRepository.searchByTitle("%$query%")
        }
}
```

- [ ] **Step 2: 创建 GetHomeCategoriesUseCase.kt**

```kotlin
package com.xxh.ringbones.domain.usecase

import javax.inject.Inject

/**
 * Provides the list of home screen categories.
 * Currently static; will be dynamic when online API is available (Phase 4).
 */
class GetHomeCategoriesUseCase @Inject constructor() {

    /** Category display name → database category value. */
    val categories: Map<Int, String> = mapOf(
        com.xxh.ringbones.R.string.hindi_bollywood to "Bollywood / Hindi",
        com.xxh.ringbones.R.string.tamil to "Tamil",
        com.xxh.ringbones.R.string.sms to "SMS  / Message Alert",
        com.xxh.ringbones.R.string.music to "Music",
        com.xxh.ringbones.R.string.malayalam to "Malayalam",
        com.xxh.ringbones.R.string.funny to "Funny",
        com.xxh.ringbones.R.string.sound to "Sound Effects",
        com.xxh.ringbones.R.string.miscellaneous to "Miscellaneous",
        com.xxh.ringbones.R.string.devotional to "Devotional",
        com.xxh.ringbones.R.string.baby to "Baby",
        com.xxh.ringbones.R.string.iphone to "Iphone"
    )

    operator fun invoke(): Map<Int, String> = categories
}
```

- [ ] **Step 3: 创建 GetFavoriteRingtonesUseCase.kt**

```kotlin
package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteRingtonesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(): Flow<List<Ringtone>> = favoriteRepository.getFavorites()
}
```

- [ ] **Step 4: 创建 ToggleFavoriteUseCase.kt**

```kotlin
package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(ringtoneId: Long) {
        val isFav = favoriteRepository.isFavorite(ringtoneId).first()
        if (isFav) {
            favoriteRepository.removeFavorite(ringtoneId)
        } else {
            favoriteRepository.addFavorite(ringtoneId)
        }
    }
}
```

- [ ] **Step 5: 创建 RecordPlayHistoryUseCase.kt**

```kotlin
package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import javax.inject.Inject

class RecordPlayHistoryUseCase @Inject constructor(
    private val playHistoryRepository: PlayHistoryRepository
) {
    suspend operator fun invoke(ringtoneId: Long, duration: Long = 0) {
        playHistoryRepository.record(ringtoneId, duration)
    }
}
```

- [ ] **Step 6: 创建 GetPlayHistoryUseCase.kt**

```kotlin
package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.core.util.Constants
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayHistoryUseCase @Inject constructor(
    private val playHistoryRepository: PlayHistoryRepository
) {
    fun getRecentPlays(): Flow<List<Ringtone>> =
        playHistoryRepository.getRecentPlays(Constants.MAX_RECENT_PLAYS)

    fun getMostPlayed(): Flow<List<Ringtone>> =
        playHistoryRepository.getMostPlayed(Constants.MAX_MOST_PLAYED)
}
```

- [ ] **Step 7: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(domain): add 6 use cases for search, favorites, history"
```

---

## Layer 4: Presentation 表现层

### Task 14: 迁移 Common UI Components

**Files:**
- Move+Modify: `ui/components/RingtoneCard.kt` → `presentation/common/RingtoneCard.kt`
- Move+Modify: `ui/components/SearchBar.kt` → `presentation/common/SearchBar.kt`
- Move+Modify: `ui/components/LoadingIndicator.kt` → `presentation/common/LoadingIndicator.kt`
- Move+Modify: `ui/components/RingtoneList.kt` → `presentation/search/components/RingtoneList.kt`
- Move+Modify: `ui/components/CategoryGrid.kt` → `presentation/home/components/CategorySection.kt`
- Delete: old `ui/components/` and `ui/screens/` directories

**Produces:** 5 个 UI 组件移入新目录结构，更新 package 和 import

- [ ] **Step 1: 创建目录并移动文件**

```bash
mkdir -p app/src/main/java/com/xxh/ringbones/presentation/common
mkdir -p app/src/main/java/com/xxh/ringbones/presentation/search/components
mkdir -p app/src/main/java/com/xxh/ringbones/presentation/home/components
mkdir -p app/src/main/java/com/xxh/ringbones/presentation/player/components

mv app/src/main/java/com/xxh/ringbones/ui/components/RingtoneCard.kt app/src/main/java/com/xxh/ringbones/presentation/common/
mv app/src/main/java/com/xxh/ringbones/ui/components/SearchBar.kt app/src/main/java/com/xxh/ringbones/presentation/common/
mv app/src/main/java/com/xxh/ringbones/ui/components/LoadingIndicator.kt app/src/main/java/com/xxh/ringbones/presentation/common/
mv app/src/main/java/com/xxh/ringbones/ui/components/RingtoneList.kt app/src/main/java/com/xxh/ringbones/presentation/search/components/
mv app/src/main/java/com/xxh/ringbones/ui/components/CategoryGrid.kt app/src/main/java/com/xxh/ringbones/presentation/home/components/CategorySection.kt
rm -rf app/src/main/java/com/xxh/ringbones/ui/
```

- [ ] **Step 2: 更新每个文件的 package 声明**

| 文件 | 新 package |
|---|---|
| `RingtoneCard.kt` | `com.xxh.ringbones.presentation.common` |
| `SearchBar.kt` | `com.xxh.ringbones.presentation.common` |
| `LoadingIndicator.kt` | `com.xxh.ringbones.presentation.common` |
| `RingtoneList.kt` | `com.xxh.ringbones.presentation.search.components` |
| `CategorySection.kt` | `com.xxh.ringbones.presentation.home.components` |

- [ ] **Step 3: 更新 RingtoneCard.kt 引用 domain 模型**

```kotlin
// 旧: import com.xxh.ringbones.data.Ringtone
// 新: import com.xxh.ringbones.domain.model.Ringtone
```

- [ ] **Step 4: 更新 RingtoneList.kt 引用**

```kotlin
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.common.LoadingIndicator
import com.xxh.ringbones.presentation.common.RingtoneCard
```

- [ ] **Step 5: 更新 CategorySection.kt 引用新 package**

保留现有的 `DrawableStringPair`、`CategoryItem`、`CategoryCard`、`CategoryRow`、`CategoryGrid` composable 定义。重命名文件即可。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(presentation): migrate UI components to feature-based packages"
```

---

### Task 15: 创建 HomeViewModel 和 HomeScreen

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt`（从旧 `ui/screens/HomeScreen.kt` 迁移）

**Produces:** Home 页面带 Hilt ViewModel，支持搜索 + 分类浏览

- [ ] **Step 1: 迁移旧 HomeScreen.kt**

```bash
mv app/src/main/java/com/xxh/ringbones/ui/screens/HomeScreen.kt app/src/main/java/com/xxh/ringbones/presentation/home/
```

更新 package 为 `com.xxh.ringbones.presentation.home`，更新 import 路径。

- [ ] **Step 2: 创建 HomeViewModel.kt**

```kotlin
package com.xxh.ringbones.presentation.home

import androidx.lifecycle.ViewModel
import com.xxh.ringbones.domain.usecase.GetHomeCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 * Provides category data for the category grid and row.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeCategoriesUseCase: GetHomeCategoriesUseCase
) : ViewModel() {

    /** Category name string-res → database type string. */
    val categories: Map<Int, String> = getHomeCategoriesUseCase()
}
```

- [ ] **Step 3: 更新 HomeScreen.kt**

```kotlin
@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        SearchBar(
            onSearch = onSearch,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HomeSection(title = R.string.recommended_collections) {
            CategoryRow(
                items = alignYourBodyData,
                onCategoryClick = onCategoryClick,
                typeNameMap = viewModel.categories
            )
        }
        HomeSection(title = R.string.favorite_collections) {
            CategoryGrid(
                items = favoriteCollectionsData,
                onCategoryClick = onCategoryClick,
                typeNameMap = viewModel.categories
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
```

同时在此文件中保留 `HomeSection`、`TypeNameToText`（现用 ViewModel 提供）、`alignYourBodyData`、`favoriteCollectionsData`。

- [ ] **Step 4: 删除旧的 SearchResultScreen.kt 和 PlayerScreen.kt（后续重建）**

```bash
rm -f app/src/main/java/com/xxh/ringbones/ui/screens/SearchResultScreen.kt
rm -f app/src/main/java/com/xxh/ringbones/ui/screens/PlayerScreen.kt
rm -rf app/src/main/java/com/xxh/ringbones/ui/screens/
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(presentation): add HomeViewModel and migrate HomeScreen"
```

---

### Task 16: 创建 SearchViewModel 和 SearchResultScreen

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/search/SearchViewModel.kt`
- Create: `app/src/main/java/com/xxh/ringbones/presentation/search/SearchResultScreen.kt`

**Produces:** 搜索结果页，使用新的 ViewModel + Repository + UseCase

- [ ] **Step 1: 创建 SearchViewModel.kt**

```kotlin
package com.xxh.ringbones.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.usecase.SearchRingtonesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRingtonesUseCase: SearchRingtonesUseCase
) : ViewModel() {

    private val query: String = savedStateHandle["query"] ?: ""
    private val byCategory: Boolean = savedStateHandle["byCategory"] ?: true

    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    init {
        viewModelScope.launch {
            searchRingtonesUseCase(query, byCategory).collect { list ->
                _ringtones.value = list
            }
        }
    }
}
```

注意：SavedStateHandle 的 key 读取方式取决于 Navigation Compose + Hilt 的版本。如果 `savedStateHandle["query"]` 不工作，可以使用 `savedStateHandle.toRoute<Route.Search>()` 方式。

- [ ] **Step 2: 创建 SearchResultScreen.kt**

```kotlin
package com.xxh.ringbones.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.search.components.RingtoneList

@Composable
fun SearchResultScreen(
    onRingtoneClick: (Ringtone) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtones by viewModel.ringtones.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            RingtoneList(
                ringtoneList = ringtones,
                loading = false,
                onRingtoneClick = onRingtoneClick
            )
        }
    }
}
```

- [ ] **Step 3: 删除旧的 SearchResultScreen.kt（已在 Task 15 删除）**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(presentation): add SearchViewModel and SearchResultScreen"
```

---

### Task 17: 创建 PlayerViewModel 和 PlayerScreen

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerViewModel.kt`
- Create: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt`
- Move+Modify: `media3/Media3PlayerView.kt` → `presentation/player/components/PlayerView.kt`
- Move+Modify: `media3/PlayerViewModel.kt` → 合并到 `PlayerViewModel.kt`
- Delete: old `media3/` directory

**Produces:** 播放器页，ViewModel 通过 ringtoneId 加载数据，UI 使用 Media3

- [ ] **Step 1: 创建 PlayerViewModel.kt**

```kotlin
package com.xxh.ringbones.presentation.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import com.xxh.ringbones.domain.usecase.RecordPlayHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
    private val recordPlayHistoryUseCase: RecordPlayHistoryUseCase
) : ViewModel() {

    private val ringtoneId: Long = savedStateHandle["ringtoneId"] ?: 0L

    private val _ringtone = MutableStateFlow<Ringtone?>(null)
    val ringtone: StateFlow<Ringtone?> = _ringtone.asStateFlow()

    private val _exoPlayer = MutableStateFlow<ExoPlayer?>(null)
    val exoPlayer: StateFlow<ExoPlayer?> = _exoPlayer.asStateFlow()

    private var currentPosition: Long = 0L

    init {
        viewModelScope.launch {
            ringtoneRepository.getById(ringtoneId).collect { ringtone ->
                _ringtone.value = ringtone
                ringtone?.let { initializePlayer(it) }
            }
        }
    }

    private fun initializePlayer(ringtone: Ringtone) {
        if (_exoPlayer.value != null) return
        val player = ExoPlayer.Builder(context).build().apply {
            val uri = resolveUri(ringtone)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            seekTo(currentPosition)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    android.util.Log.e("PlayerViewModel", "Playback error: ${error.message}")
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        viewModelScope.launch {
                            recordPlayHistoryUseCase(ringtone.id)
                        }
                    }
                }
            })
        }
        _exoPlayer.value = player
    }

    private fun resolveUri(ringtone: Ringtone): Uri {
        return ringtone.downloadPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) Uri.parse("file://$path")
            else Uri.parse(ringtone.url)
        } ?: Uri.parse(ringtone.url)
    }

    fun savePlayerState() {
        _exoPlayer.value?.let { currentPosition = it.currentPosition }
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayer.value?.release()
        _exoPlayer.value = null
    }
}
```

- [ ] **Step 2: 创建 PlayerScreen.kt**

```kotlin
package com.xxh.ringbones.presentation.player

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtone by viewModel.ringtone.collectAsState()
    val exoPlayer by viewModel.exoPlayer.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePlayerState()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (ringtone != null && exoPlayer != null) {
                PlayerView(exoPlayer = exoPlayer!!)
            }
        }
    }
}
```

- [ ] **Step 3: 迁移 Media3PlayerView 为 PlayerView**

```bash
mkdir -p app/src/main/java/com/xxh/ringbones/presentation/player/components
mv app/src/main/java/com/xxh/ringbones/media3/Media3PlayerView.kt app/src/main/java/com/xxh/ringbones/presentation/player/components/PlayerView.kt
```

重命名为 `PlayerView`，更新 package 为 `com.xxh.ringbones.presentation.player.components`，简化签名为只接收 `ExoPlayer`。

- [ ] **Step 4: 删除旧的 media3 目录**

```bash
rm app/src/main/java/com/xxh/ringbones/media3/PlayerViewModel.kt
rm -rf app/src/main/java/com/xxh/ringbones/media3/
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(presentation): add PlayerViewModel and PlayerScreen with Hilt"
```

---

## Layer 5: 集成与清理

### Task 18: 连接导航与全功能集成

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/MainActivity.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/core/theme/Theme.kt`

**Produces:** 完整的可编译、可运行应用

- [ ] **Step 1: 更新 AppNavGraph.kt 填充所有目的地**

```kotlin
package com.xxh.ringbones.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.xxh.ringbones.presentation.home.HomeScreen
import com.xxh.ringbones.presentation.player.PlayerScreen
import com.xxh.ringbones.presentation.search.SearchResultScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Home) {
        composable<Route.Home> {
            HomeScreen(
                onSearch = { query ->
                    navController.navigate(Route.Search(query = query, byCategory = false))
                },
                onCategoryClick = { type ->
                    navController.navigate(Route.Search(query = type, byCategory = true))
                }
            )
        }
        composable<Route.Search> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Search>()
            SearchResultScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                }
            )
        }
        composable<Route.Player> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Player>()
            PlayerScreen()
        }
    }
}
```

- [ ] **Step 2: 更新 MainActivity.kt 完整实现**

```kotlin
package com.xxh.ringbones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.xxh.ringbones.core.navigation.AppNavGraph
import com.xxh.ringbones.core.theme.MusixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSize = currentWindowAdaptiveInfo().windowSizeClass
            MusixTheme {
                Musix2025App(windowSize)
            }
        }
    }
}

@Composable
fun Musix2025App(windowSize: WindowSizeClass) {
    val navController = rememberNavController()

    when (windowSize.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            AppNavGraph(navController = navController)
        }
        WindowWidthSizeClass.EXPANDED -> {
            Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 5.dp) {
                Row {
                    SootheNavigationRail()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun SootheNavigationRail(modifier: Modifier = Modifier) {
    NavigationRail(
        modifier = modifier.padding(start = 8.dp, end = 8.dp),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavigationRailItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.bottom_navigation_home)) },
                selected = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            NavigationRailItem(
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                label = { Text(stringResource(R.string.bottom_navigation_profile)) },
                selected = false,
                onClick = {}
            )
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 构建 APK 验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL，生成 APK

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: integrate navigation, MainActivity, full app compilation"
```

---

### Task 19: 最终验证与清理

**Files:**
- Check: 无残留旧代码引用
- Verify: `./gradlew :app:assembleDebug` 成功

- [ ] **Step 1: 运行完整编译**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 检查未使用的 import 和旧引用**

Run: `./gradlew :app:lintDebug`
Expected: 无严重 error

- [ ] **Step 3: 确认删除所有旧文件**

确认以下文件已删除:
- `data/Ringtone.kt` (旧 entity)
- `data/RingtoneDao.kt` (旧 DAO)
- `data/AppDatabase.kt` (旧 database)
- `data/DatabaseHelper.kt` (assets DB 复制器)
- `data/MusixRingtonesList.kt` (硬编码数据)
- `data/RingtoneViewModel.kt` (旧 ViewModel)
- `data/RingtoneViewModelFactory.kt` (手动工厂)
- `media3/PlayerViewModel.kt` (旧播放器 VM)
- `media3/PlayerControls.kt` (已 staged delete)
- `network/` (已合并到 core)
- `util/` (已合并到 core)
- `ui/` (已拆分到 presentation)
- `LandActivity.kt` (已 staged delete)
- `PlayActivity.kt` (已 staged delete)
- `SearchResultActivity.kt` (已 staged delete)

- [ ] **Step 4: 运行 lint 检查**

Run: `./gradlew :app:lintDebug`
Expected: 无新引入的 error

- [ ] **Step 5: 最终 Commit**

```bash
git add -A
git commit -m "chore: final cleanup and verification for Phase 1"
```

---

## 任务依赖图

```
Task 0 (Dependencies)
 └─┬─ Task 1 (Hilt App/Activity/DI)
   ├─ Task 2 (Theme)
   ├─ Task 3 (Navigation)
   ├─ Task 4 (Network + DataStore)
   └─ Task 5 (Utils)
       └─┬─ Task 6 (Entities)
         ├─ Task 7 (DAOs)
         └─ Task 8 (AppDatabase)
             └─ Task 9 (Mapper) ─┐
             └─ Task 10 (Repos)  ─┤
                                  ├─ Task 11 (Domain Models)
                                  ├─ Task 12 (Repo Interfaces)
                                  └─ Task 13 (UseCases)
                                      └─ Task 14 (Common UI)
                                          ├─ Task 15 (Home)
                                          ├─ Task 16 (Search)
                                          └─ Task 17 (Player)
                                              └─ Task 18 (Integration)
                                                  └─ Task 19 (Final Cleanup)
```

---

## 预计文件变更汇总

| 操作 | 数量 |
|---|---|
| 新建文件 | ~35 |
| 修改文件 | ~8 |
| 删除文件 | ~15 |
| 移动文件 | ~10 |
| **合计** | **~68** |