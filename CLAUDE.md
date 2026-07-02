# Android 项目开发规范

## 项目定位

打造 Google Play 商业级 Android 应用。

所有代码必须达到可上线标准。

禁止 Demo 风格代码。

---

# 技术栈

语言：

- Kotlin

UI：

- Jetpack Compose

设计：

- Material Design 3
- Material You

架构：

- Clean Architecture
- MVVM
- Repository Pattern

DI：

- Hilt

数据库：

- Room

播放器：

- Media3 ExoPlayer

图片：

- Coil

异步：

- Coroutines
- Flow
- StateFlow

数据存储：

- DataStore

网络：

- Retrofit
- OkHttp

JSON：

- Kotlin Serialization

禁止 XML。

---

# UI 设计规范

整体风格：

现代

时尚

科技

高品质

高级感

Glassmorphism

Material You

圆角

渐变

毛玻璃

动态背景

流畅动画

支持：

Dark Mode

Light Mode

Dynamic Color

Edge To Edge

沉浸式体验

动画统一使用：

AnimatedContent

AnimatedVisibility

SharedTransition

Material Motion

Spring Animation

Lottie

---

# 代码规范

所有代码：

必须可编译。

必须有注释。

必须有 KDoc。

禁止重复代码。

禁止 Magic Number。

禁止 Deprecated API。

禁止 TODO。

禁止占位实现。

所有变量命名必须具有语义。

所有业务逻辑放入 ViewModel。

Composable 必须保持轻量。

Composable 必须支持 Preview。

每个页面拆分多个可复用组件。

---

# 项目目录

presentation/

domain/

data/

core/

common/

feature/

每个 Feature：

ui/

component/

viewmodel/

repository/

model/

usecase/

state/

event/

navigation/

---

# 性能规范

使用：

remember

derivedStateOf

StateFlow

LazyColumn

LazyGrid

避免：

不必要重组

内存泄漏

阻塞主线程

支持：

120Hz

大数据

分页

离线缓存

---

# 高级功能（必须实现）

## AI 推荐

根据：

播放历史

收藏记录

搜索记录

自动推荐铃声。

---

## 在线资源库

支持：

Supabase

Firebase

REST API

支持：

在线下载

分页

缓存

专题推荐

排行榜

猜你喜欢

每日更新。

---

## 音频裁剪

支持：

波形

拖动裁剪

试听

导出

淡入

淡出

设置铃声。

---

## 音频可视化

支持：

实时频谱

波形动画

动态背景

节奏动画

呼吸灯

---

## 智能播放列表

自动生成：

最近播放

收藏

热门

猜你喜欢

AI 推荐

支持用户创建播放列表。

---

## 云同步

同步：

收藏

播放历史

设置

主题

播放列表

账号登录后自动恢复。

---

## Widget

支持：

桌面播放器

播放控制

Mini Player

快捷收藏

---

## 通知栏播放器

支持：

Media Session

锁屏控制

通知栏控制

封面

播放控制

收藏

上一首

下一首

---

# 输出要求

Claude 必须始终生成：

完整文件。

完整 Import。

完整目录。

完整 Gradle。

完整代码。

保证项目可以直接运行。

如果回复长度达到限制，应自动继续生成，直到整个项目完成，而不是等待用户再次要求。

