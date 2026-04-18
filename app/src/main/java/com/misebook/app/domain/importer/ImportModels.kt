package com.misebook.app.domain.importer

import com.misebook.app.domain.model.Direction
import com.misebook.app.domain.model.Ingredient
import com.misebook.app.domain.model.MeasurementUnit
import java.util.UUID

/** Parsed recipe that the user must review before saving. */
data class ParsedRecipe(
    val title: String,
    val servings: Int? = null,
    val yieldAmount: Double? = null,
    val yieldUnit: String? = null,
    val ingredients: List<ParsedIngredient> = emptyList(),
    val directions: List<ParsedDirection> = emptyList(),
    val sourceUrl: String? = null,
    val confidence: Double = 0.8,
    val warnings: List<String> = emptyList()
)

data class ParsedIngredient(
    val quantity: Double?,
    val unit: MeasurementUnit,
    val name: String,
    val note: String? = null,
    val rawLine: String,
    val confidence: Double = 0.8
) {
    fun toIngredient(position: Int): Ingredient = Ingredient(
        id = UUID.randomUUID().toString(),
        position = position,
        quantity = quantity,
        unit = unit,
        name = name,
        note = note
    )
}

data class ParsedDirection(
    val text: String,
    val confidence: Double = 0.9
) {
    fun toDirection(position: Int): Direction = Direction(
        id = UUID.randomUUID().toString(),
        position = position,
        text = text
    )
}

enum class ImportSource { IMAGE, PDF, URL, TEXT }
