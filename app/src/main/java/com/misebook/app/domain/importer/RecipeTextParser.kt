package com.misebook.app.domain.importer

import com.misebook.app.domain.model.MeasurementUnit

/**
 * Heuristic parser that turns a block of free-form recipe text into a [ParsedRecipe].
 *
 * It never attempts to be "clever" — instead it gives chefs an honest, editable first
 * pass. Tricky lines get tagged with a lower confidence so the review UI can prompt
 * the user to look twice.
 */
object RecipeTextParser {

    private val qtyRegex = Regex(
        "^\\s*(?<qty>\\d+(?:[.,]\\d+)?(?:\\s*[¼½¾⅓⅔⅛⅜⅝⅞]|/\\d+)?|[¼½¾⅓⅔⅛⅜⅝⅞])?\\s*(?<unit>[a-zA-ZÀ-ÿ]+\\.?)?\\s*(?<rest>.*)"
    )
    private val servingsRegex = Regex("(?i)(?:serves|servings?|yields?|makes)\\s*:?\\s*(\\d+)")
    private val ingredientHeaderRegex = Regex("(?i)^\\s*(ingredients?|for the .*)\\s*:?\\s*$")
    private val directionsHeaderRegex = Regex("(?i)^\\s*(directions?|instructions?|method|preparation|steps?)\\s*:?\\s*$")
    private val stepNumberPrefix = Regex("^\\s*(\\d+)[\\.)\\-]\\s+")

    // A line that contains nothing but a step marker such as "1", "1.", "1)",
    // ".1" (a common PDF artefact where glyphs are emitted out of order).
    private val bareStepMarker = Regex("^\\s*\\.?\\s*\\d+\\s*[\\.)\\-]?\\s*$")
    private val bareBulletMarker = Regex("^\\s*[•\\-*·]\\s*$")
    // A line that looks like a word-wrap continuation (starts with lowercase).
    private val continuationStart = Regex("^[a-z]")

    private val unicodeFractions = mapOf(
        '¼' to 0.25, '½' to 0.5, '¾' to 0.75,
        '⅓' to 1.0 / 3.0, '⅔' to 2.0 / 3.0,
        '⅛' to 0.125, '⅜' to 0.375, '⅝' to 0.625, '⅞' to 0.875
    )

    fun parse(rawText: String, sourceUrl: String? = null, titleHint: String? = null): ParsedRecipe {
        val cleaned = rawText.replace("\r\n", "\n").trim()
        val rawLines = cleaned.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val lines = reflowLines(rawLines)
        if (lines.isEmpty()) {
            return ParsedRecipe(title = titleHint ?: "Untitled", confidence = 0.0, warnings = listOf("No text detected"))
        }

        val warnings = mutableListOf<String>()
        val title = titleHint?.takeIf { it.isNotBlank() } ?: lines.first()
        val servings = servingsRegex.find(cleaned)?.groupValues?.get(1)?.toIntOrNull()

        // Partition into ingredients vs directions sections by heuristic.
        var section: Section = Section.UNKNOWN
        val ingredientLines = mutableListOf<String>()
        val directionLines = mutableListOf<String>()

        lines.drop(if (titleHint == null) 1 else 0).forEach { line ->
            when {
                ingredientHeaderRegex.matches(line) -> section = Section.INGREDIENTS
                directionsHeaderRegex.matches(line) -> section = Section.DIRECTIONS
                else -> when (section) {
                    Section.INGREDIENTS -> ingredientLines.add(line)
                    Section.DIRECTIONS -> directionLines.add(line)
                    Section.UNKNOWN -> {
                        if (looksLikeIngredientLine(line)) ingredientLines.add(line)
                        else directionLines.add(line)
                    }
                }
            }
        }

        if (ingredientLines.isEmpty() && directionLines.isNotEmpty()) {
            // Nothing matched clearly as ingredients — fall back: treat short lines as ingredients,
            // longer paragraphs as directions.
            val (short, long) = directionLines.partition { it.length < 80 && looksLikeIngredientLine(it) }
            ingredientLines.addAll(short)
            directionLines.clear()
            directionLines.addAll(long)
            if (short.isEmpty()) warnings.add("Could not detect ingredient section — please review.")
        }

        val parsedIngredients = ingredientLines.map { parseIngredientLine(it) }
        val parsedDirections = directionLines.map { line ->
            ParsedDirection(
                text = line.replace(stepNumberPrefix, "").trim(),
                confidence = if (stepNumberPrefix.containsMatchIn(line) || line.length > 25) 0.9 else 0.7
            )
        }

        val overallConfidence = listOf(
            if (parsedIngredients.isNotEmpty()) parsedIngredients.map { it.confidence }.average() else 0.0,
            if (parsedDirections.isNotEmpty()) parsedDirections.map { it.confidence }.average() else 0.0
        ).filter { it > 0.0 }.ifEmpty { listOf(0.3) }.average()

        return ParsedRecipe(
            title = title,
            servings = servings,
            ingredients = parsedIngredients,
            directions = parsedDirections,
            sourceUrl = sourceUrl,
            confidence = overallConfidence,
            warnings = warnings
        )
    }

    /**
     * PDF and OCR extractors often split a single list item across multiple lines:
     *
     *     "1."
     *     "Preheat the oven to 350°F."
     *
     * Or, when a PDF stores bullet glyphs out-of-order, the marker can appear as
     * ".1" instead of "1." on its own line. This pass merges a bare marker with
     * the line that follows it, and also joins obvious word-wrap continuations
     * (lines starting with a lowercase letter or with a hyphenated word ending
     * the previous line) back onto their parent line.
     */
    internal fun reflowLines(lines: List<String>): List<String> {
        if (lines.isEmpty()) return lines
        val result = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val current = lines[i]
            val isBareStep = bareStepMarker.matches(current)
            val isBareBullet = bareBulletMarker.matches(current)
            if ((isBareStep || isBareBullet) && i + 1 < lines.size) {
                val nextRaw = lines[i + 1]
                val next = nextRaw.trimStart()
                // Keep a normalized "N." prefix for step markers so the parser's
                // step-number regex still fires; drop stray leading dots/hyphens.
                val prefix = when {
                    isBareStep -> current.trim().trimStart('.', ' ').trimEnd('.', ')', '-', ' ').let {
                        if (it.all { ch -> ch.isDigit() }) "$it." else it
                    }
                    else -> ""
                }
                val merged = if (prefix.isBlank()) next else "$prefix $next"
                result.add(merged)
                i += 2
                continue
            }
            // Merge obvious soft-wrapped continuation lines: "Preheat the oven" + "to 350F"
            if (result.isNotEmpty()) {
                val prev = result.last()
                val prevEndsHyphenated = prev.endsWith('-') && !prev.endsWith(" -")
                val looksLikeContinuation =
                    continuationStart.containsMatchIn(current) &&
                    !bareStepMarker.containsMatchIn(current) &&
                    !ingredientHeaderRegex.matches(current) &&
                    !directionsHeaderRegex.matches(current) &&
                    !prev.endsWith(':') &&
                    // Only merge when the previous line ended mid-sentence.
                    prev.lastOrNull()?.let { it.isLetter() || it == ',' || it == '-' } == true
                if (prevEndsHyphenated) {
                    result[result.size - 1] = prev.dropLast(1) + current
                    i++
                    continue
                }
                if (looksLikeContinuation) {
                    result[result.size - 1] = "$prev $current"
                    i++
                    continue
                }
            }
            result.add(current)
            i++
        }
        return result
    }

    private enum class Section { UNKNOWN, INGREDIENTS, DIRECTIONS }

    private fun looksLikeIngredientLine(line: String): Boolean {
        if (line.length > 120) return false
        if (qtyRegex.find(line)?.groups?.get("qty")?.value?.isNotBlank() == true) return true
        // Lines that start with bullet or look like a short noun phrase
        return line.length < 80 && (line.startsWith("-") || line.startsWith("•") || line.startsWith("*") ||
            line.split(" ").size <= 8)
    }

    fun parseIngredientLine(raw: String): ParsedIngredient {
        val trimmed = raw.trimStart('-', '•', '*', ' ')
        val m = qtyRegex.matchEntire(trimmed) ?: return ParsedIngredient(
            quantity = null,
            unit = MeasurementUnit.NONE,
            name = trimmed,
            rawLine = raw,
            confidence = 0.5
        )
        val qtyStr = m.groups["qty"]?.value?.trim().orEmpty()
        val unitStr = m.groups["unit"]?.value?.trim().orEmpty()
        val rest = m.groups["rest"]?.value?.trim().orEmpty()

        val quantity = parseQuantity(qtyStr)
        val parsedUnit = MeasurementUnit.parse(unitStr)
        // If we consumed a word that isn't a known unit, put it back into the name.
        val (unit, name) = if (unitStr.isNotBlank() && parsedUnit == MeasurementUnit.NONE) {
            MeasurementUnit.NONE to ("$unitStr $rest").trim()
        } else {
            parsedUnit to rest
        }

        val (cleanName, note) = extractNote(name)
        val confidence = when {
            quantity != null && unit != MeasurementUnit.NONE -> 0.95
            quantity != null -> 0.8
            cleanName.isBlank() -> 0.3
            else -> 0.6
        }
        return ParsedIngredient(
            quantity = quantity,
            unit = unit,
            name = cleanName.ifBlank { raw },
            note = note,
            rawLine = raw,
            confidence = confidence
        )
    }

    private fun parseQuantity(s: String): Double? {
        if (s.isBlank()) return null
        val normalized = s.replace(",", ".")
        // Handle unicode fractions like "1½" or "½"
        val unicodeTotal = normalized.sumOf { ch -> unicodeFractions[ch] ?: 0.0 }
        val nonFractionPart = normalized.filter { it !in unicodeFractions }.trim()
        if (unicodeTotal > 0.0) {
            val whole = nonFractionPart.toDoubleOrNull() ?: 0.0
            return whole + unicodeTotal
        }
        // Handle "1 1/2" or "1/2"
        val spaceSplit = nonFractionPart.split(" ")
        var total = 0.0
        var any = false
        for (token in spaceSplit) {
            if (token.isBlank()) continue
            if ("/" in token) {
                val parts = token.split("/")
                if (parts.size == 2) {
                    val num = parts[0].toDoubleOrNull() ?: continue
                    val den = parts[1].toDoubleOrNull() ?: continue
                    if (den != 0.0) {
                        total += num / den
                        any = true
                    }
                }
            } else {
                token.toDoubleOrNull()?.let {
                    total += it
                    any = true
                }
            }
        }
        return if (any) total else nonFractionPart.toDoubleOrNull()
    }

    private fun extractNote(name: String): Pair<String, String?> {
        val commaIdx = name.indexOf(',')
        if (commaIdx <= 0) return name to null
        return name.substring(0, commaIdx).trim() to name.substring(commaIdx + 1).trim().ifBlank { null }
    }
}
