package io.github.chwi.recipecalculator.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.units.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed [SettingsRepository]. Enums are stored by [Enum.name]; unknown or missing values
 * fall back to the [AppSettings] defaults, and a read error (corrupt file) degrades to defaults
 * rather than crashing.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                fractionStyle = prefs[FRACTION_STYLE].toEnum(FractionStyle.INLINE),
                unitSystem = prefs[UNIT_SYSTEM].toEnum(UnitSystem.US),
            )
        }

    override suspend fun setFractionStyle(style: FractionStyle) {
        dataStore.edit { it[FRACTION_STYLE] = style.name }
    }

    override suspend fun setUnitSystem(system: UnitSystem) {
        dataStore.edit { it[UNIT_SYSTEM] = system.name }
    }

    private companion object {
        val FRACTION_STYLE = stringPreferencesKey("fraction_style")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
    }
}

/** Parse a stored enum name, falling back to [default] when null or unrecognised. */
private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
