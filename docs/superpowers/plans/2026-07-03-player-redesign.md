# Player Page Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete rewrite of the music player page — pure Compose UI, full playback engine with queue/FX/visualizer, real album cover support.

**Architecture:** `PlayerEngine` (interface + ExoPlayer implementation) manages all playback, queue, and visualizer state via a single `StateFlow<PlayerState>`. `PlayerViewModel` routes `PlayerEvent` → `PlayerEngine` and exposes state + effects to `PlayerScreen`. All UI is pure Compose with no `AndroidView`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Media3 ExoPlayer, Coil, Hilt, StateFlow, Coroutines

## Global Constraints

- All code must compile. No TODO, TBD, placeholder implementations.
- Pure Compose UI — zero `AndroidView`, zero XML.
- Follow existing project patterns: Clean Architecture, MVVM, Repository Pattern.
- `compileSdk = 37`, `minSdk = 24`, `targetSdk = 35`.
- Kotlin 2.3.21, KSP 2.3.9, Compose BOM 2026.06.01.
- All padding must use 4-positional-arg form (named-direction overloads are `@Composable` in this BOM).
- MaterialTheme.colorScheme access must be at `@Composable` top-level, not inside `remember{}`.

---