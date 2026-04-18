package com.misebook.app.domain.model

/**
 * Measurement dimensions tracked by MiseBook. We intentionally treat Mass, Volume
 * and Count as strictly separate dimensions — conversions across dimensions (e.g.
 * cups of flour → grams of flour) require ingredient-specific density and are out
 * of scope for the MVP.
 */
enum class UnitSystem { METRIC, IMPERIAL, UNIVERSAL }

enum class UnitDimension { MASS, VOLUME, COUNT, OTHER }

/**
 * A measurement unit. [canonicalFactor] expresses how many base units (grams for
 * MASS, millilitres for VOLUME, pieces for COUNT) are contained in one of this
 * unit. `displayShort` is what we render in the UI.
 */
enum class MeasurementUnit(
    val displayShort: String,
    val dimension: UnitDimension,
    val system: UnitSystem,
    val canonicalFactor: Double
) {
    GRAM("g", UnitDimension.MASS, UnitSystem.METRIC, 1.0),
    KILOGRAM("kg", UnitDimension.MASS, UnitSystem.METRIC, 1000.0),
    OUNCE("oz", UnitDimension.MASS, UnitSystem.IMPERIAL, 28.3495231),
    POUND("lb", UnitDimension.MASS, UnitSystem.IMPERIAL, 453.59237),

    MILLILITER("ml", UnitDimension.VOLUME, UnitSystem.METRIC, 1.0),
    LITER("l", UnitDimension.VOLUME, UnitSystem.METRIC, 1000.0),
    TEASPOON("tsp", UnitDimension.VOLUME, UnitSystem.IMPERIAL, 4.92892),
    TABLESPOON("tbsp", UnitDimension.VOLUME, UnitSystem.IMPERIAL, 14.7868),
    FLUID_OUNCE("fl oz", UnitDimension.VOLUME, UnitSystem.IMPERIAL, 29.5735),
    CUP("cup", UnitDimension.VOLUME, UnitSystem.IMPERIAL, 236.588),

    PIECE("pcs", UnitDimension.COUNT, UnitSystem.UNIVERSAL, 1.0),
    CLOVE("clove", UnitDimension.COUNT, UnitSystem.UNIVERSAL, 1.0),
    DOZEN("doz", UnitDimension.COUNT, UnitSystem.UNIVERSAL, 12.0),

    NONE("", UnitDimension.OTHER, UnitSystem.UNIVERSAL, 1.0);

    companion object {
        /** Lenient parse of free-text unit strings. Returns [NONE] on unknown. */
        fun parse(raw: String?): MeasurementUnit {
            if (raw.isNullOrBlank()) return NONE
            val cleaned = raw.trim().lowercase().trimEnd('.')
            // Try the exact form first (so aliases ending in 's' like "tbs" or "lbs" match),
            // then fall back to a singular form so "grams", "ounces" etc. also parse.
            return match(cleaned) ?: match(cleaned.removeSuffix("s")) ?: NONE
        }

        private fun match(s: String): MeasurementUnit? = when (s) {
            "g", "gram", "grm" -> GRAM
            "kg", "kilo", "kilogram" -> KILOGRAM
            "oz", "ounce" -> OUNCE
            "lb", "pound", "lbs" -> POUND
            "ml", "milliliter", "millilitre" -> MILLILITER
            "l", "lt", "liter", "litre" -> LITER
            "tsp", "teaspoon" -> TEASPOON
            "tbsp", "tablespoon", "tbs" -> TABLESPOON
            "fl oz", "floz", "fluid ounce" -> FLUID_OUNCE
            "cup", "c" -> CUP
            "pc", "pcs", "piece", "pieces" -> PIECE
            "clove" -> CLOVE
            "doz", "dozen" -> DOZEN
            else -> null
        }
    }
}
