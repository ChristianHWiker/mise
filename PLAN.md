# Recipe Calculator — Roadmap

This is the **living roadmap** (the *how/when*). `spec.html` remains the product spec (the *what*).
The phase numbering here reflects the **actual build order** and is aligned with the git tags, so it
intentionally diverges from the original 7-phase list in `spec.html` (see "Divergence" note below).

## Current state — `v0.3.0`
A usable single-user recipe app: open onto a seeded recipe, browse/search/filter the library,
scale forward (yield) and backward (limiting ingredient), and **create / edit / delete recipes**
with structured ingredient + step editors, drag-to-reorder, density-aware metric conversion,
free-form tags, and a photo picker. Fraction style + unit system are flipped in Settings.
**Next:** OCR capture (Phase 3) so recipes can be photographed off printed pages.

- Package `io.github.chwi.recipecalculator` · single-activity · MVVM + Flow + Hilt + Room.
- Resolved stack: AGP 9.2.1 · Gradle 9.4.1 · Kotlin 2.2.10 · compileSdk 36.1 / minSdk 24 ·
  Compose BOM 2026.02.01 · Hilt 2.59.2 · Room 2.8.4 · Navigation Compose 2.9.8 (type-safe routes) ·
  Lifecycle 2.10.0 · KSP 2.2.10-2.0.2 · DataStore 1.2.1.
- CLI build notes + AGP 9 constraints live in `memory/build_environment.md`.

---

## Phases

### Phase 0 — Foundations · ✅ done (`v0.1.0`)
Android Studio Kotlin scaffold, Gradle Kotlin DSL + version catalog, Compose + Material 3 editorial
theme (Source Serif 4 + Geist via Downloadable Fonts; light/dark palette; type/spacing tokens via
`RecipeTheme`), Navigation Compose shell + bottom nav, Hilt, Room (recipes/ingredients/tags, exported
schema v1), the `core/rational` exact-fraction library with JUnit tests, and a dev insert/read-back screen.
**Done when:** app builds and runs on emulator; dev screen inserts a recipe and reads it back. ✓

### Phase 1 — Editorial read experience + scaling · ✅ done (`v0.2.0`)
The two hero screens from the design handoff, fully wired to Room, plus the scaling engine and
display settings. (This merged what `spec.html` split across its phases 01-read / 02 / 03.)
- **Home**: editorial masthead, live search, data-driven tag filter, Room-backed library, "Add recipe" FAB.
- **Detail**: hero, title block, forward scaling via yield stepper, tappable ingredients, numbered method, pin/save.
- **Reverse-scale sheet**: "how much do you have?" → recipe-wide rescale with live yield preview (the differentiator).
- **Scaling/display**: forward + reverse scaling, fractional display, smart rounding (counts→int, mass→5 g grid). Unit-tested.
- **`core/units`**: ingredient-aware US↔metric conversion (density via `gramsPerCup`). Unit-tested.
- **Settings (DataStore)**: fraction style + unit system, applied live across Home & Detail.
**Done when:** scale a recipe forward/back and see sensible fractions; toggle units and see cups→grams. ✓

### Phase 2 — Recipe authoring (CRUD) · ✅ done (`v0.3.0`)
The editor that turns the read-only app into a usable single-user cookbook. Shipped:
- `RecipeEditorViewModel` + `RecipeEditorScreen` covering create (`recipeId=null`) and edit.
- Header fields: title, category, time, difficulty, servings + yield unit, pin, free-form comma-separated tags.
- **Structured ingredient editor**: drag-to-reorder cards via `sh.calvin.reorderable`, raw-text qty
  parsed by `Rational.parseOrNull`, unit dropdown (cup/tbsp/tsp/g/kg/ml/ea), name, optional modifier.
  Density auto-fills from the lookup table on cup/tbsp/tsp.
- **Steps editor**: drag-to-reorder multi-line text rows.
- **Persistence**: DAO/repository gained `updateRecipe` (delete-and-reinsert child rows in a
  `@Transaction`) and `deleteRecipe`. FK cascade handles ingredient + tag cleanup.
- **Validation**: non-empty title, servings ≥ 1, ≥ 1 ingredient row that parses to a positive
  quantity with a name; pure-scratch rows are dropped silently on save.
- **Density lookup** (`core/density/Density.kt`): a starter ~40-entry curated `name → gramsPerCup`
  table with normalized lookup (lowercase, trim, basic plural stripping) and graceful `null` fallback.
  Grow as gaps surface; spec target is ~150.
- **Photo picker**: Android system `PickVisualMedia` → copied into `filesDir/recipe_photos/` and stored
  as a `file://` URI on the recipe. Phase 3 will revisit when camera/OCR lands.
- **Detail edit affordance**: previously-orphaned `onEdit` callback now wired via a top-bar overflow
  → "Edit recipe"; the editor's overflow holds "Delete recipe" with a confirmation dialog.

### Phase 3 — OCR capture (camera) · ◀ NEXT
The headline differentiator for the job market. CameraX capture → ML Kit on-device text recognition →
ingredient-line parser (regex + heuristics) → confirmation screen with editable parsed rows → save as a recipe.
**Done when:** photograph a printed ingredient list, fix mis-parses inline, save as a new recipe.

### Phase 4 — Security & hardening · planned
Biometric app lock (AndroidX Biometric), Keystore-backed encryption for lockable recipes / sensitive
prefs (Jetpack Security), Play Integrity attestation on launch (soft-fail), scoped backup rules.
Directly targets the job listing's bonus criteria. Test on a physical device.
**Done when:** app locks behind biometrics; sensitive data encrypted at rest; failed Integrity verdict handled gracefully.

### Phase 5 — Polish & launch · planned
Onboarding, settings defaults, app icon + splash + store assets, signed AAB, privacy policy,
Play Store internal testing track.
**Done when:** the AAB passes internal review and is installable from a tester invite.

---

## Backlog — deferred ideas
- **Manual recipe ordering on Home.** Add a `sortIndex` column on `RecipeEntity` (Room migration),
  switch the DAO query from `ORDER BY createdAt DESC` to `ORDER BY sortIndex`, and add drag-to-reorder
  on the Home list (same `sh.calvin.reorderable` pattern already used in the editor). Currently the
  list is chronological; pinned items aren't grouped. Worth doing once the library is large enough
  that chronological order stops being good enough.

---

## Divergence from `spec.html`
The spec lists 7 phases (00 Foundations, 01 Recipe CRUD, 02 Scaling, 03 Unit system, 04 OCR,
05 Security, 06 Launch). In practice the design handoff pushed the **scaling + unit-conversion
differentiator (spec 02–03) to ship early** inside our Phase 1, alongside the read UI — while
**Recipe CRUD (spec 01) was deferred** and is now our Phase 2. The spec's content is unchanged and
still authoritative for product behavior; this file is the schedule of record. Net remaining work
vs. spec: the full ~150-entry density table, OCR, security, launch.

## Project structure (as built)
```
app/src/main/java/io/github/chwi/recipecalculator/
  MainActivity.kt · RecipeApp.kt
  navigation/   # type-safe routes, NavHost, bottom nav
  ui/
    recipes/    # Home + Detail + ReverseScaleSheet + Editor (+ ViewModels)
    settings/   # Settings + ViewModel
    dev/        # insert/read-back harness
    theme/      # Color, Type, Spacing, Theme (RecipeTheme tokens)
    common/     # shared composables
  core/
    rational/   # exact-fraction Rational + display formatting (pure, tested)
    units/      # ingredient-aware conversion + reverse-scale math (pure, tested)
    density/    # curated name→gramsPerCup lookup for the editor (pure, tested)
  data/
    db/         # RecipeDatabase, RecipeDao, Converters
    model/      # Recipe/Ingredient/Tag entities + relations
    repository/ # RecipeRepository (+impl)
    settings/   # AppSettings + DataStore-backed SettingsRepository
    seed/       # SampleData + first-launch DatabaseSeeder
  di/           # Hilt modules (database, repository, settings)
```
