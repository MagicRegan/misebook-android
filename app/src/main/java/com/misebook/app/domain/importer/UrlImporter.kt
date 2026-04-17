package com.misebook.app.domain.importer

import com.misebook.app.domain.model.MeasurementUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports recipes from URLs. First tries to read schema.org/Recipe JSON-LD blocks
 * (the de-facto standard — used by most recipe sites); falls back to freeform text
 * extraction when JSON-LD isn't present.
 */
@Singleton
class UrlImporter @Inject constructor() {

    suspend fun importFromUrl(url: String): ParsedRecipe = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            return@withContext ParsedRecipe(
                title = "Imported URL",
                confidence = 0.0,
                warnings = listOf("URL was empty.")
            )
        }
        val doc = runCatching {
            Jsoup.connect(cleanUrl)
                .userAgent("Mozilla/5.0 (Android) MiseBook/1.0")
                .timeout(15_000)
                .get()
        }.getOrElse {
            return@withContext ParsedRecipe(
                title = "Imported URL",
                sourceUrl = cleanUrl,
                confidence = 0.0,
                warnings = listOf("Could not reach the page: ${it.message ?: "unknown error"}")
            )
        }
        extractJsonLd(doc, cleanUrl) ?: extractFreeform(doc, cleanUrl)
    }

    private fun extractJsonLd(doc: Document, url: String): ParsedRecipe? {
        val scripts = doc.select("script[type=application/ld+json]")
        for (script in scripts) {
            val raw = script.data().trim()
            if (raw.isBlank()) continue
            val recipe = findRecipeNode(raw) ?: continue
            return jsonToRecipe(recipe, url)
        }
        return null
    }

    private fun findRecipeNode(raw: String): JSONObject? {
        return try {
            when (val first = parseJsonFlexible(raw)) {
                is JSONObject -> findInObject(first)
                is JSONArray -> {
                    for (i in 0 until first.length()) {
                        val item = first.opt(i)
                        if (item is JSONObject) findInObject(item)?.let { return it }
                    }
                    null
                }
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun parseJsonFlexible(raw: String): Any {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
    }

    private fun findInObject(obj: JSONObject): JSONObject? {
        val type = obj.opt("@type")
        if (matchesRecipe(type)) return obj
        val graph = obj.optJSONArray("@graph")
        if (graph != null) {
            for (i in 0 until graph.length()) {
                val node = graph.optJSONObject(i) ?: continue
                if (matchesRecipe(node.opt("@type"))) return node
            }
        }
        return null
    }

    private fun matchesRecipe(type: Any?): Boolean = when (type) {
        is String -> type.equals("Recipe", ignoreCase = true)
        is JSONArray -> (0 until type.length()).any { i ->
            (type.optString(i) ?: "").equals("Recipe", ignoreCase = true)
        }
        else -> false
    }

    private fun jsonToRecipe(node: JSONObject, url: String): ParsedRecipe {
        val title = node.optString("name").takeIf { it.isNotBlank() } ?: "Imported recipe"
        val servings = node.optString("recipeYield").let {
            Regex("\\d+").find(it)?.value?.toIntOrNull()
        } ?: node.optInt("recipeYield", 0).takeIf { it > 0 }

        val ingredientsJson = node.optJSONArray("recipeIngredient") ?: JSONArray()
        val ingredients = buildList {
            for (i in 0 until ingredientsJson.length()) {
                val line = ingredientsJson.optString(i) ?: continue
                if (line.isBlank()) continue
                add(RecipeTextParser.parseIngredientLine(line))
            }
        }

        val instructionsArr = node.optJSONArray("recipeInstructions")
        val instructionsFromArr = if (instructionsArr != null) {
            buildList {
                for (i in 0 until instructionsArr.length()) {
                    when (val item = instructionsArr.opt(i)) {
                        is String -> if (item.isNotBlank()) add(ParsedDirection(item, confidence = 0.95))
                        is JSONObject -> {
                            val text = item.optString("text").ifBlank { item.optString("name") }
                            if (text.isNotBlank()) add(ParsedDirection(text, confidence = 0.95))
                        }
                    }
                }
            }
        } else emptyList()

        val instructionsFromString = if (instructionsFromArr.isEmpty()) {
            node.optString("recipeInstructions")
                .split(Regex("\\n+|\\. +"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { ParsedDirection(it, confidence = 0.85) }
        } else emptyList()

        val directions = (instructionsFromArr + instructionsFromString)

        return ParsedRecipe(
            title = title,
            servings = servings,
            ingredients = ingredients,
            directions = directions,
            sourceUrl = url,
            confidence = if (ingredients.isNotEmpty() && directions.isNotEmpty()) 0.95 else 0.7,
            warnings = buildList {
                if (ingredients.isEmpty()) add("No ingredients found in page metadata.")
                if (directions.isEmpty()) add("No directions found in page metadata.")
            }
        )
    }

    private fun extractFreeform(doc: Document, url: String): ParsedRecipe {
        val title = doc.title().ifBlank { doc.select("h1").first()?.text() ?: "Imported recipe" }
        val bodyText = doc.select("article, main, [itemprop=recipeInstructions], .recipe, #recipe, body")
            .firstOrNull()
            ?.text()
            ?: doc.body().text()
        val parsed = RecipeTextParser.parse(bodyText, sourceUrl = url, titleHint = title)
        val warnings = (parsed.warnings + "Site did not provide structured recipe data — please review carefully.").distinct()
        return parsed.copy(warnings = warnings, confidence = minOf(parsed.confidence, 0.6))
    }
}
