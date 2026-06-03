package io.github.chwi.recipecalculator.core.theme

/**
 * How the app resolves light vs. dark. [SYSTEM] defers to the OS setting (the original behaviour);
 * [LIGHT] / [DARK] pin it regardless. Persisted via DataStore, applied at the top of the theme.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The selectable brand accent. The editorial cream background and serif type are constant across
 * all four — only the accent (and its soft tint) change. [SAGE] is the default. The colour values
 * live in the UI layer (`ui/theme/Color.kt`); this enum is the pure preference token shared by
 * settings and theme.
 */
enum class AccentTheme { TERRACOTTA, SAGE, PLUM, SLATE }
