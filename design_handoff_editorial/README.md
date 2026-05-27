# Handoff: Recipe Calculator — Editorial UI direction

## Overview
Mobile-first recipe app, Android-first per `spec.html`. This handoff covers the **chosen visual direction** ("Editorial — cookbook") for two screens:

1. **Home** — the boot screen / recipe library
2. **Recipe Detail** — the scaling screen (forward + reverse scaling)

The product's differentiator is **reverse scaling**: tap any ingredient to enter "I have X g/cups", and the recipe rescales to match. The Detail screen owns that interaction.

## About the design files
The files in this bundle are **design references created as an HTML prototype** — not production code to copy directly. The intended target stack (per `PLAN.md`) is **native Android — Kotlin + Jetpack Compose + Material 3**. Your job is to recreate these screens as `@Composable` functions using the project's theme system — not to ship the HTML.

## Fidelity
**High-fidelity.** Exact tokens, type scales, spacing, and interactions are defined below. Use them verbatim unless an Android/Material constraint demands a deviation.

## How to view the prototype
Open `prototype.html` in any modern browser. Use the floating **Tweaks** panel (bottom-right) to flip dark mode, fraction style, and unit system.

- Tap any recipe in **Home** → drills into **Detail** (same artboard, internal state change).
- Tap the back chevron in **Detail** → returns to **Home**.
- Tap any ingredient row → opens the **reverse-scale bottom sheet**.
- Tap `+`/`−` on the Yield card → forward-scale by 2 servings per tap.

---

## Screen 1 — Home (boot screen)

**Purpose:** Library + entry point. User lands here on app launch.

**Layout** (412 dp width, vertical scroll inside a `SafeAreaView`):

| Region | Content | Notes |
|---|---|---|
| Top bar | `≡` menu (L) · `COOKBOOK` caps (C) · `⋯` (R) | 48 dp tall, no background fill |
| Masthead | Date kicker · "My cookbook" · meta line | See typography below |
| Search | text input, 1 px rule border, magnifier glyph | placeholder: "Search recipes, ingredients…" |
| Tag filter | horizontal scroll: All · Pinned · Cookies · Pasta · Bread · Mains · Dessert · Breakfast | active tag → 2 px accent underline + text color |
| Section header | filter name (serif 18) + count right | `{N} {recipe / recipes}` |
| Recipe rows | thumbnail 64×64 + title + meta + pin | divider lines between rows |
| FAB | extended pill "+ Add recipe", terracotta | bottom-right, drop shadow tinted accent |

**Masthead details:**
- Date kicker (e.g. `TUESDAY · 27 MAY`): 10 px, `0.18em` letter-spacing, uppercase, accent color, 600 weight
- Title: Source Serif 4, 40 px, 0.98 line-height, `-0.02em`, 400 weight, "My cookbook"
- Meta: 12 px, muted, `47 recipes · 3 pinned · last cooked 2 days ago` (tabular nums)

**Recipe row composition:**
- 64×64 thumbnail (gradient placeholder in mock; real app: `photoUri`)
- Title: Source Serif 4, 17 px, `-0.005em`, 400 weight
- Meta line: 11 px, `CATEGORY · {yield} · {time}` — category is uppercase with `0.08em`, rest is mixed-case tabular
- Pin star: terracotta `★` glyph when `recipe.pin === true`

**FAB:**
- Height 52 dp, padding `0 22px`, fully rounded (`borderRadius: 999`)
- Background: accent (`#a04830` light / `#dc8462` dark)
- Foreground: white, "+ Add recipe" Sans 14/500
- Shadow: `0 10px 28px rgba(160,72,48,0.4)` (accent-tinted)
- Position: absolute `bottom: 18, right: 18`

---

## Screen 2 — Recipe Detail (scaling)

**Purpose:** Read a recipe and scale it (forward or reverse).

**Layout** (412 dp width, vertical scroll):

| Region | Content |
|---|---|
| Top bar | back `‹` (L) · `№ 012 · COOKBOOK` caps (C) · `♡` save (R) |
| Hero photo | 220 dp tall, 18 dp horizontal margin, gradient placeholder (real: photo) |
| Title block | category kicker · serif H1 · meta row |
| Servings card | YIELD label · count (serif 24) · stepper `−` `+` |
| Hint | italic serif "Tap an ingredient to scale by what you have →" |
| Ingredients | serif H2 + ingredient rows (clickable) |
| Method | serif H2 + numbered steps |

**Title block:**
- Category kicker: terracotta caps, 10 px / `0.18em` / 600 weight
- Title: Source Serif 4, 30 px / 1.1 / `-0.012em` / 400 weight
- Meta: `45 min · Easy · 9 ingredients` — 12 px muted, with `·` separators at 0.5 opacity

**Servings card:**
- Card: `surface` bg, 1 px `rule` border, 3 dp radius, padding `14×18`
- Left: `YIELD` kicker (10 caps) + `12 cookies` (serif 24, "cookies" in sans 13 muted)
- Right: two circular stepper buttons, 38 dp, 1 px rule border, surface bg

**Ingredient row:**
- Padding `13px 0`, bottom 1 px `rule`
- Left: name (sans 15) + optional modifier in italic serif 12 (e.g. "cooled", "packed")
- Right: quantity (serif 19, tabular) + unit (sans 13 muted) — same baseline
- Whole row is the tap target → opens reverse-scale sheet

**Method step:**
- Padding `14px 0`, bottom 1 px `rule` (except last)
- Number: italic serif 24, terracotta accent, fixed 22 dp wide
- Body: sans 14 / 1.6 line-height

---

## Screen 3 — Reverse-scale bottom sheet (modal)

**Purpose:** "I have X amount of one ingredient — scale the rest of the recipe to match."

**Layout** (full width, bottom-aligned, presented as a modal over Detail):

| Region | Content |
|---|---|
| Backdrop | `rgba(20,12,4,0.4)` — tap to dismiss |
| Sheet container | bg `bg`, top border 4 px accent, radius 4 dp top, shadow `0 -10px 30px rgba(0,0,0,0.25)` |
| Drag handle | 40×3 dp muted pill, centered |
| Kicker | "REVERSE SCALE" terracotta caps |
| Heading | serif 22, "How much {ingredient name lowercase} do you have?" |
| Helper text | sans 13 muted: "Recipe calls for **{current qty} {unit}**. We'll scale everything else to match." |
| Stepper card | accent-soft bg, padding `14×10` |
| Yield preview | surface card, 1 px rule, `YIELDS` kicker + "{N} cookies" right |
| CTAs | Cancel (outline, 1 width) · "Scale recipe" (terracotta filled, 2 width) |

**Stepper card:**
- Background: `accentSoft` (light `#f1dccc`, dark `#3a2418`)
- Left/right: 38 dp circular buttons (surface bg, rule border)
- Center: serif 38 tabular value (snapped to current fraction style) + unit label below (caps 11, muted)
- Step size: 5 g for grams, 1 for count, 0.25 for cup/tsp/tbsp

**Math:**
- `newFactor = currentFactor × (userValue / currentDisplayValue)`
- All other ingredients rescale; servings = `round(baseServings × newFactor)`, min 1

---

## State

```ts
// App-level (EditorialApp)
screen: 'home' | 'detail'

// Home-local (EditorialHome)
search: string
tag:    'All' | 'Pinned' | <category-name>

// Detail-local (EditorialVariant)
factor:           number         // 1.0 = original recipe
sheetIngredient:  string | null  // ingredient.id whose sheet is open

// Sheet-local (EditorialSheet)
value:            number         // user-edited amount in current display unit
```

State is screen-local for the prototype. In production:
- `screen` → Navigation Compose `NavHost` (per `PLAN.md`)
- recipe data → Room database (per `PLAN.md`)
- `factor` and `sheetIngredient` → ViewModel `StateFlow`, collected in the detail screen
- Tweaks (fraction style, unit system, dark mode) → app settings, persisted via DataStore or Room

---

## Design tokens

### Colors — Light
| Token | Hex | Use |
|---|---|---|
| `bg` | `#faf6ec` | App background (warm cream paper) |
| `surface` | `#fdfbf3` | Cards, search input, sheet inner cards |
| `text` | `#1f1a14` | Primary ink |
| `muted` | `#7a6f5d` | Secondary text, captions |
| `mutedSoft` | `#b4a895` | Hints, drag handle, ›/× glyphs |
| `rule` | `#e5dcc7` | Hairlines, card borders, dividers |
| `accent` | `#a04830` | Terracotta — buttons, active tag, step numbers, kickers |
| `accentSoft` | `#f1dccc` | Subtle accent surfaces (stepper card in sheet) |

### Colors — Dark
| Token | Hex |
|---|---|
| `bg` | `#181410` |
| `surface` | `#221c15` |
| `text` | `#f1ead8` |
| `muted` | `#a39780` |
| `mutedSoft` | `#6e6451` |
| `rule` | `#322a1f` |
| `accent` | `#dc8462` |
| `accentSoft` | `#3a2418` |

### Typography

- **Serif** — `Source Serif 4` (Google Fonts) — titles, quantities, step numbers
- **Sans** — `Geist` (Google Fonts) — body, UI labels, captions

In Compose, load via the Downloadable Fonts API (`GoogleFont` + `FontFamily`) or bundle the TTF files in `res/font/` and expose them through the app theme's `Typography`.

| Token | Family | Size | Line | Letter | Weight |
|---|---|---|---|---|---|
| `mastheadH1` | Serif | 40 | 0.98 | -0.02em | 400 |
| `detailH1` | Serif | 30 | 1.1 | -0.012em | 400 |
| `sectionH2` | Serif | 17–18 | 1.2 | 0 | 400 |
| `sheetTitle` | Serif | 22 | 1.2 | -0.01em | 400 |
| `stepperValue` | Serif | 38 | 1 | tabular | 400 |
| `ingredientQty` | Serif | 19 | 1 | tabular | 400 |
| `stepNumber` | Serif italic | 24 | 1 | 0 | 400 |
| `body` | Sans | 14–15 | 1.3–1.6 | 0 | 400 |
| `caption` | Sans | 11–12 | 1.4 | 0 | 400 |
| `kicker` | Sans caps | 10–11 | 1 | 0.10–0.18em | 600 |

All numeric strings should use `font-variant-numeric: tabular-nums` to keep columns aligned.

### Spacing scale
`4, 6, 8, 10, 12, 14, 18, 22, 28` dp.

### Radius
- Hairline cards: `2–3 dp` (intentionally angular for editorial feel)
- FAB pill: fully rounded
- Circular stepper buttons: 50%

### Shadows
- FAB: `0 10px 28px rgba(160,72,48,0.4)` (accent-tinted, light only — adjust for dark)
- Bottom sheet: `0 -10px 30px rgba(0,0,0,0.25)`

### Borders
- Hairlines: `1 px solid rule`
- Sheet top border: `4 px solid accent` (signature of the sheet)

---

## Recipe data model

Per `spec.html`, quantities are stored as **integer rationals** to keep `3/4 + 1/8` exact:

```ts
type Quantity = { num: number; den: number };
```

See `data.js` for the canonical reference implementation of:

- `simplify(q)` — gcd-based reduction
- `scaleQty(q, factor)` — scale a rational by a float
- `formatFraction(q, style)` — display as `1 1/2` / `1½` / `1.5`; snaps fractions to cooking grid `{1/8, 1/4, 1/3, 3/8, 1/2, 5/8, 2/3, 3/4, 7/8}`
- `convert(qty, unit, gramsPerCup, system)` — US ↔ metric, ingredient-aware (uses density)
- `displayQty(ing, factor, fractionStyle, system)` — top-level display helper used by all rows
- `factorFromDisplayValue(...)` — reverse-scale math

**Load-bearing piece:** the per-ingredient `gramsPerCup` density. v1 should ship a curated table (~150 entries per spec). Unknown ingredients fall back gracefully — see `convert()`.

**Smart rounding:**
- Counts (eggs) → integers ≥ 1
- Gram values ≥ 50 g → snap to 5 g
- Fractional units → snap to the cooking grid above

---

## Interactions / behavior

**Navigation**
- `Home → Detail` on recipe row tap. Real app: `router.push('/recipes/[id]')`.
- `Detail → Home` on back chevron. Real app: `router.back()`.
- Sheet is a modal layer over Detail. Tap backdrop or Cancel to dismiss.

**Scaling**
- Forward: `±` 2 servings per tap on the Yield card stepper. (Step of 2 is recipe-specific — make this `Math.max(1, Math.round(baseServings * 0.1))` or similar in code.)
- Reverse: tap ingredient → set new amount in sheet → Apply.

**Tag filter**
- `All` shows everything.
- `Pinned` shows `r.pin === true` only.
- Other tags filter by `r.cat === tag`.

**Search**
- Substring match against `r.title`, case-insensitive.
- Empty result: "Nothing matches that — yet." (italic serif, centered)

**Empty states (not in mock — please design copy)**
- First launch, zero recipes: pitch the OCR capture flow.

**Animations**
- Sheet present/dismiss: slide-up from bottom with 200 ms ease-out, backdrop fade.
- Home ↔ Detail: standard platform stack push.

---

## Assets

- **Fonts:** Source Serif 4, Geist — both Google Fonts, no licensing concerns.
- **Photos:** gradient placeholders in this mock. Real app: `photoUri` (camera capture or imported). The mock pairs each recipe with a hand-picked `[from, to]` gradient — discard.
- **Icons:** text glyphs in mock (`≡ ⋯ ‹ ♡ ★ ⌕ × − +`). Replace with a Compose icon library (`compose-icons-lucide` or Material Icons Extended) in production. Keep monoline, ~22 dp visual weight.

---

## Files in this bundle

| File | Purpose |
|---|---|
| `prototype.html` | **Open this in a browser** to interact with the design |
| `data.js` | Recipe data + rational quantity math — **copy this logic verbatim** |
| `editorial.jsx` | Editorial UI components (Home, Detail, Sheet, App wrapper) — reference for layout & interactions |
| `phone.jsx` | Mock Android phone shell — display only, drop in real app |
| `android-frame.jsx` | Status bar + nav bar shells — display only |
| `tweaks-panel.jsx` | Dev tool used by the prototype — **not part of the app**, exposed in real app as Settings |

---

## Notes for the dev

- The Tweaks panel in this mock is the prototype's dev console. In the real app, surface **Fraction style** and **Unit system** in the Settings screen (per `PLAN.md`). Dark mode follows system (`isSystemInDarkTheme()`).
- Per `CLAUDE.md`: **no Co-Authored-By lines** in commits.
- The light/dark palettes above map 1:1 to `ui/theme/Color.kt` — define them as a custom `ColorScheme` and pass to `MaterialTheme`.
- The 8-color light/dark palettes and the type scale are deliberately small. Resist adding tokens unless a new screen genuinely needs one — additions creep fast.
- Editorial feel comes from three things: warm cream over white, hairline rules + sharp 2 dp radii (no soft cards), and serif italic step numbers in accent. Keep those when extending.
