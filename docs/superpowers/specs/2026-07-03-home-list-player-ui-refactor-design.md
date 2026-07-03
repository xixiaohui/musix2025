# UI Refactor: Home, List, Player Pages

**Date:** 2026-07-03
**Status:** Approved
**Scope:** 首页、列表页（分类详情/搜索）、播放页视觉重构

---

## 目标

保持现有首页结构（Hero → Chips → Featured → Grid），全面升级视觉表现力：
- 毛玻璃（Glassmorphism）效果
- 流畅动画（SharedTransition、Spring Animation、AnimatedContent）
- 动态渐变背景
- 沉浸式列表（Sticky Header、排名、滑动手势、热度标识）
- Pure Compose 原生实现，不引入 Lottie 等额外重量级依赖

---

## 文件变更范围

### 新增文件
| 文件 | 职责 |
|------|------|
| `presentation/home/components/HeroHeader.kt` | 动态渐变 Hero + 脉冲图标 + 毛玻璃搜索栏 |
| `presentation/home/components/GlassSearchBar.kt` | 封装 blur 效果的搜索输入框（防抖 300ms） |
| `presentation/home/components/CategoryChipRow.kt` | 动画 Chip 横向滚动 |
| `presentation/home/components/FeaturedRow.kt` | 视差 snap 横向滚动卡片 |
| `presentation/home/components/CategoryGrid.kt` | 交错入场分类卡片网格（含曲目数量） |
| `presentation/search/components/CollapsingHeader.kt` | 滚动收缩的渐变 Banner |
| `presentation/search/components/RankedRingtoneCard.kt` | 排名徽章 + 热度标识 + 滑动手势卡片 |
| `presentation/player/components/AlbumArt.kt` | 脉冲光晕封面（SharedTransition 目标） |
| `presentation/player/components/DynamicBackground.kt` | 播放进度驱动的色彩渐变背景 |
| `presentation/common/GlassCard.kt` | 通用毛玻璃容器（blur + 半透明 surface） |
| `presentation/common/GradientBackground.kt` | 动态渐变背景 Composable |
| `presentation/common/AnimationSpecs.kt` | 统一动画 Token 定义 |

### 修改文件
| 文件 | 改动 |
|------|------|
| `presentation/home/HomeScreen.kt` | 拆分子组件，组合新组件 |
| `presentation/home/HomeViewModel.kt` | 增加分类曲目计数、热门标识数据 |
| `presentation/search/SearchResultScreen.kt` | 沉浸式列表：stickyHeader + CollapsingHeader + RankedRingtoneCard |
| `presentation/search/CategoryDetailViewModel.kt` | 增加排序能力、热度标记 |
| `presentation/player/PlayerScreen.kt` | SharedTransition 封面 + DynamicBackground + 动画播放控制 |
| `presentation/common/RingtoneCard.kt` | 玻璃效果升级 + 添加 sharedElement 支持 |
| `core/navigation/AppNavGraph.kt` | 包裹 SharedTransitionLayout |

### 不变文件
- `data/` 整个数据层（Seeder、DAO、Repository、Entity、Mapper）
- `domain/` 领域层（Model、Repository 接口、UseCase）
- `core/datastore/` 偏好存储
- `MyApplication.kt` 启动初始化
- Gradle 配置（除非需要开启 compose 动画实验性 API）

---

## 架构设计

```
HomeScreen
├── HeroHeader              ← 新：动态渐变 + 脉冲图标 + GlassSearchBar
│   └── GlassSearchBar      ← 新：毛玻璃搜索（blur + 防抖搜索）
├── CategoryChipRow         ← 新：动画 Chip 行
├── FeaturedRow             ← 新：Snap 视差卡片行
│   └── GlassCard           ← 新：通用毛玻璃容器
└── CategoryGrid            ← 新：交错入场网格

SearchResultScreen
├── TopAppBar               ← 改：滚动模糊收缩
├── CollapsingHeader        ← 新：收缩 Banner
├── StickyHeader            ← 新：分类标题
└── RankedRingtoneCard[]    ← 新：排名+热度+滑动+sharedElement

PlayerScreen
├── TopAppBar               ← 改：透明背景
├── AlbumArt                ← 新：脉冲光晕 + SharedTransition
├── DynamicBackground       ← 新：播放进度驱动渐变动画
├── WaveformProgressBar     ← 新：Canvas 绘制波形进度
├── PlaybackControls        ← 改：Spring 弹性动画按钮
└── BottomActions           ← 改：下载 + 设铃声
```

---

## 统一动画系统 (AnimationSpecs.kt)

| Token | 规格 | 用途 |
|-------|------|------|
| `springDefault` | `spring(dampingRatio=0.6f, stiffness=400f)` | 按钮点击/卡片缩放 |
| `springBouncy` | `spring(dampingRatio=0.3f, stiffness=200f)` | 列表入场交错 |
| `tweenTransition` | `tween(350ms, FastOutSlowInEasing)` | 页面切换、SharedTransition |
| `enterDelay` | 50ms per item | 交错入场间隔 |

---

## 关键交互

1. **首页点击分类 Chip** → 导航到 SearchResultScreen（按分类过滤）
2. **首页点击 Featured Card** → SharedTransition 到 PlayerScreen，自动播放
3. **首页点击分类网格** → 导航到 SearchResultScreen
4. **列表页点击卡片** → SharedTransition 到 PlayerScreen
5. **列表页左滑卡片** → 收藏/取消收藏
6. **播放页返回** → popBackStack，列表滚动位置保持

---

## 兼容性策略

- `Modifier.blur()` 仅 API 31+ (Android 12) 启用
- 低版本降级：`surface.copy(alpha=0.4f)` + `shadow` 模拟毛玻璃层次感
- `SharedTransition` 需要 Compose BOM 1.7+（当前 gradle 版本检查后确认）
- 如 SharedTransition API 不可用，降级为 `AnimatedContent` + `fadeIn/fadeOut`

---

## 验证清单

- [ ] 首页所有区块正确渲染，LazyColumn 滚动流畅
- [ ] 分类 Chip 点击正确导航到列表页
- [ ] 列表页 StickyHeader 正确吸附
- [ ] 列表页滑动操作（收藏）正常工作
- [ ] 列表页卡片点击 SharedTransition 到播放页
- [ ] 播放页 ExoPlayer 正常播放音频
- [ ] 播放页动态背景随播放进度变化
- [ ] 返回按钮正确 popBackStack
- [ ] Dark Mode / Light Mode 均正常显示
- [ ] API < 31 低版本降级效果可接受