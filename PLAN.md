# Recipe Calculator — Roadmap

This is the **living roadmap** (the *how/when*). `spec.html` remains the product spec (the *what*).
The phase numbering here reflects the **actual build order** and is aligned with the git tags, so it
intentionally diverges from the original 7-phase list in `spec.html` (see "Divergence" note below).

## Current state — `v0.2.0`
A read-only-but-real recipe app: it opens onto a seeded recipe, you can browse/search/filter the
library, scale a recipe forward (by yield) and backward (by a limiting ingredient), and flip
fraction style + unit system in Settings. **You cannot yet create or edit recipes** — that's next.

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

### Phase 2 — Recipe authoring (CRUD) · ◀ NEXT
Make the app usable with your own recipes. The "Add recipe" FAB and the Detail edit affordance
currently hit a placeholder; this phase replaces it.
- `RecipeEditorViewModel` + screen for create (`id=null`) and edit (existing id).
- Header fields: title, category, time, difficulty, servings + yield unit, pin.
- **Structured ingredient editor**: dynamic rows — quantity (parsed by the existing `Rational.parseOrNull`),
  unit dropdown (cup/tsp/tbsp/g/ml/ea…), name, optional modifier. Add / remove / reorder.
- **Steps editor**: dynamic add / remove / reorder text rows.
- **Persistence gaps to fill**: add `updateRecipe` + `deleteRecipe` to the DAO/repository (today only `addRecipe`).
- **Validation**: non-empty title, ≥1 ingredient, quantities that parse.
- **Density lookup** (folded in from spec Phase 03): a curated name→`gramsPerCup` table so user-entered
  recipes convert to metric, with graceful fallback for unknown ingredients. (Per-recipe unit override: optional, can defer.)
**Done when:** user can enter a real recipe, save it, find it via search/tags, edit it, delete it — and its metric conversion is sane.

### Phase 3 — OCR capture (camera) · planned
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

## Divergence from `spec.html`
The spec lists 7 phases (00 Foundations, 01 Recipe CRUD, 02 Scaling, 03 Unit system, 04 OCR,
05 Security, 06 Launch). In practice the design handoff pushed the **scaling + unit-conversion
differentiator (spec 02–03) to ship early** inside our Phase 1, alongside the read UI — while
**Recipe CRUD (spec 01) was deferred** and is now our Phase 2. The spec's content is unchanged and
still authoritative for product behavior; this file is the schedule of record. Net remaining work
vs. spec: CRUD editor, the full density table, OCR, security, launch.

## Project structure (as built)
```
app/src/main/java/io/github/chwi/recipecalculator/
  MainActivity.kt · RecipeApp.kt
  navigation/   # type-safe routes, NavHost, bottom nav
  ui/
    recipes/    # Home + Detail + ReverseScaleSheet (+ ViewModels)   editor lands here in Phase 2
    settings/   # Settings + ViewModel
    dev/        # insert/read-back harness
    theme/      # Color, Type, Spacing, Theme (RecipeTheme tokens)
    common/     # shared composables
  core/
    rational/   # exact-fraction Rational + display formatting (pure, tested)
    units/       # ingredient-aware conversion + reverse-scale math (pure, tested)
  data/
    db/         # RecipeDatabase, RecipeDao, Converters
    model/      # Recipe/Ingredient/Tag entities + relations
    repository/ # RecipeRepository (+impl)
    settings/   # AppSettings + DataStore-backed SettingsRepository
    seed/       # SampleData + first-launch DatabaseSeeder
  di/           # Hilt modules (database, repository, settings)
```
