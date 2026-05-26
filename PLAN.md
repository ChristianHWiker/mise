# Phase 00 — Foundations Plan

## Context
Greenfield React Native (Expo) recipe calculator app. Phase 00 lays the foundation: project skeleton, navigation, theme, SQLite, and rational-number math library. Everything downstream depends on these choices being solid.

## Decisions
- **Expo SDK 52** (latest stable)
- **Expo Router** (file-based navigation — modern standard, portfolio signal)
- **Custom theme system** (lightweight tokens, no UI library dependency — shows design system skill)
- **expo-sqlite** for persistence
- **Rational math as a pure TS library** with tests

## Implementation Steps

### 1. Project scaffold
- `npx create-expo-app@latest` with TypeScript template
- Configure Expo Router (app/ directory structure)
- Git init + .gitignore

### 2. Navigation shell
- Tab navigator: Recipes, Settings (minimal for now)
- Stack screens within Recipes tab: List, Detail, Editor (placeholder screens)
- Camera tab placeholder (for OCR later)

### 3. Theme system
- `src/theme/` — color tokens (light mode only for v1), spacing scale, typography scale
- ThemeProvider via React Context
- `useTheme()` hook
- Tokens inspired by the spec's CSS variables (warm neutrals, orange accent)

### 4. Rational number library
- `src/lib/rational.ts` — core type `{ num: number; den: number }`
- Operations: add, subtract, multiply, divide, simplify (GCD-based), compare
- `toFraction()` display: renders as "3/4", "1 1/2", whole numbers as "2"
- `fromDecimal()`, `fromString()` parsers ("1 1/2", "3/4", "0.75")
- Full test suite (this is the load-bearing math — tests are non-negotiable)

### 5. SQLite setup
- `src/db/` — database module using expo-sqlite
- Migration system (versioned, forward-only)
- Migration 001: recipes table, ingredients table, tags table
- Schema matches the spec's data model (quantities stored as num/den integer columns)
- Dev-only screen to insert + read back a recipe (Phase 00 "done when" criteria)

## Project structure
```
app/                    # Expo Router file-based routes
  (tabs)/
    recipes/
      index.tsx         # Recipe list
      [id].tsx          # Recipe detail
      editor.tsx        # Create/edit
    settings.tsx
  _layout.tsx           # Root layout + tab navigator
src/
  lib/
    rational.ts         # Rational number math
    rational.test.ts    # Tests
  db/
    database.ts         # DB init + migration runner
    migrations/         # Numbered SQL migrations
    recipes.ts          # Recipe CRUD queries
  theme/
    tokens.ts           # Colors, spacing, typography
    ThemeProvider.tsx
    useTheme.ts
  types/
    recipe.ts           # Recipe, Ingredient, Quantity, Unit types
```

## Verification
- App builds and runs on Android emulator
- Tab navigation works (Recipes, Settings)
- Rational math tests pass (add, multiply, simplify, display fractions)
- Dev screen can insert a recipe into SQLite and read it back
- Theme tokens apply consistently across placeholder screens
