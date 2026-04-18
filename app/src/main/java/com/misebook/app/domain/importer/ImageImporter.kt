package com.misebook.app.domain.importer

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class ImageImporter @Inject constructor(
    private val context: Context
) {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun importFromUri(uri: Uri): ParsedRecipe {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognize(image)
        val text = reflowText(result)
        if (text.isBlank()) {
            return ParsedRecipe(
                title = "Imported photo",
                confidence = 0.0,
                warnings = listOf("No text could be read from this image. Try a sharper photo with good lighting.")
            )
        }
        val parsed = RecipeTextParser.parse(text)
        val warnings = if (parsed.warnings.isEmpty()) {
            listOf("OCR can miss accents and diacritics — please double-check quantities and unit symbols before saving.")
        } else parsed.warnings
        return parsed.copy(
            warnings = warnings,
            confidence = minOf(parsed.confidence, 0.75)
        )
    }

    /**
     * Rebuild a readable string from ML Kit's recognition result.
     *
     * The flat [Text.getText] collapses multi-column layouts in unpredictable ways —
     * a recipe card with ingredients in a side column often comes back with lines
     * from both columns interleaved. We instead walk [Text.getTextBlocks] in
     * document order (ML Kit already orders top-to-bottom, left-to-right) and join
     * lines inside a block with a single newline, then separate blocks with a
     * blank line so the downstream parser can treat each block as a paragraph.
     */
    internal fun reflowText(result: Text): String {
        val blocks = result.textBlocks
        if (blocks.isEmpty()) return result.text
        return blocks.joinToString(separator = "\n\n") { block ->
            block.lines.joinToString(separator = "\n") { line ->
                // Join individual elements with a single space so that very short
                // recognized elements ("1", "/", "2") stay on the same line.
                line.elements.joinToString(separator = " ") { it.text }
                    .ifBlank { line.text }
            }
        }
    }

    private suspend fun recognize(image: InputImage): Text =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
