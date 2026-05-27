# Phase 00 — Foundations Plan

## Context
Greenfield native Android (Kotlin) recipe calculator app. Phase 00 lays the foundation: project skeleton, navigation, theme, persistence, and the rational-number math library. Everything downstream depends on these choices being solid.

## Decisions
- **Kotlin + Jetpack Compose + Material 3** (modern Android standard, strong portfolio signal)
- **Single-activity, Navigation Compose** for file-free, type-safe navigation
- **MVVM with Coroutines + Flow**; **Hilt** for DI
- **Room** for persistence (replaces the earlier expo-sqlite plan)
- **Rational math as a pure Kotlin library** with JUnit tests
- Stack picked to set up the later "nice to have" wins from the target listing: on-device ML (ML Kit), camera (CameraX), biometrics, secure storage, Play Integrity — added in their respective phases, not Phase 00.

## Implementation Steps

### 1. Project scaffold
- New Android Studio project, Kotlin, **Gradle Kotlin DSL** + **version catalog** (`libs.versions.toml`)
- Compose enabled, Material 3, Hilt set up, single `MainActivity`
- minSdk 24 / target latest stable; Git init + `.gitignore`

### 2. Navigation shell
- `NavHost` with bottom navigation bar: **Recipes**, **Settings**
- Recipes graph: List → Detail → Editor (placeholder composables)
- Camera/capture route stubbed for OCR later

### 3. Theme system
- `ui/theme/` — Material 3 `ColorScheme`, `Typography`, spacing tokens (light mode only for v1)
- Tokens drawn from the spec's palette (warm neutrals, orange accent)
- Wrap app in app `Theme {}`; verify tokens apply across placeholder screens

### 4. Rational number library
- `core/rational/Rational.kt` — core type `data class Rational(val num: Int, val den: Int)`
- Operations: add, subtract, multiply, divide, simplify (GCD-based), compare (`Comparable`)
- `toDisplay()`: renders "3/4", "1 1/2", whole numbers as "2"
- `fromDecimal()`, `fromString()` parsers ("1 1/2", "3/4", "0.75")
- Full JUnit test suite (this is the load-bearing math — tests are non-negotiable)

### 5. Room setup
- `data/db/` — `RecipeDatabase` (Room), DAOs, versioned migrations
- Migration 001: recipes, ingredients, tags tables
- Schema matches the spec's data model (quantities stored as num/den integer columns)
- Dev-only screen to insert + read back a recipe (Phase 00 "done when" criteria)

## Project structure
```
app/
  src/main/java/<pkg>/
    MainActivity.kt
    RecipeApp.kt              # @HiltAndroidApp Application
    navigation/
      RecipeNavHost.kt        # NavHost + routes
    ui/
      theme/                  # Color, Type, Spacing, Theme
      recipes/                # list / detail / editor screens + ViewModels
      settings/
      dev/                    # dev insert/read-back screen
    core/
      rational/               # Rational.kt (pure, no Android deps)
    data/
      db/                     # RecipeDatabase, DAOs, migrations
      model/                  # Recipe, Ingredient, Quantity, Unit entities
      repository/             # RecipeRepository
  src/test/                   # JUnit — rational math tests
gradle/libs.versions.toml     # version catalog
```

## Verification
- App builds and runs on Android emulator
- Bottom-nav navigation works (Recipes, Settings)
- Rational math tests pass (add, multiply, simplify, display fractions)
- Dev screen can insert a recipe into Room and read it back
- Material 3 theme tokens apply consistently across placeholder screens

## Status — Phase 00 built
Scaffold from Android Studio (Empty Activity, Kotlin DSL), then implemented the foundation.

**Resolved stack:** AGP 9.2.1 · Gradle 9.4.1 · Kotlin 2.2.10 · compileSdk 36.1 / minSdk 24 ·
Compose BOM 2026.02.01 · Hilt 2.59.2 · Room 2.8.4 · Navigation Compose 2.9.8 (type-safe, `@Serializable`
routes) · Lifecycle 2.10.0 · KSP 2.2.10-2.0.2. Package `io.github.chwi.recipecalculator`.

**Built:** Hilt app + DI; editorial theme (light/dark palette, Source Serif 4 + Geist via Downloadable
Fonts, type/spacing tokens exposed through `RecipeTheme`); `core/rational` library + 23 JUnit tests;
Room (recipes/ingredients/tags, exported schema v1, repository, DI); single-activity NavHost with
bottom nav + placeholder screens; dev insert/read-back screen.

**Verified green:** `assembleDebug`, `testDebugUnitTest` (23/23), `compileDebugAndroidTestKotlin`.
**Not yet run here:** the app on an emulator, runtime Downloadable-Fonts fetch, and the instrumented
`RecipeDaoTest` (compiles; needs a device/emulator to execute).

See `memory/build_environment.md` for CLI build notes and the AGP 9 version constraints.
