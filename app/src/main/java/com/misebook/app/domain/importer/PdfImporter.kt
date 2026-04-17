package com.misebook.app.domain.importer

import android.content.Context
import android.net.Uri
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfImporter @Inject constructor(
    private val context: Context
) {
    suspend fun importFromUri(uri: Uri): ParsedRecipe = withContext(Dispatchers.IO) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                PdfReader(input).use { reader ->
                    PdfDocument(reader).use { pdf ->
                        buildString {
                            for (i in 1..pdf.numberOfPages) {
                                append(PdfTextExtractor.getTextFromPage(pdf.getPage(i)))
                                append('\n')
                            }
                        }
                    }
                }
            } ?: ""
        }.getOrElse {
            return@withContext ParsedRecipe(
                title = "Imported PDF",
                confidence = 0.0,
                warnings = listOf("Could not read PDF: ${it.message ?: "unknown error"}")
            )
        }
        if (text.isBlank()) {
            ParsedRecipe(
                title = "Imported PDF",
                confidence = 0.0,
                warnings = listOf("This PDF appears to contain only images. Try importing as an image instead.")
            )
        } else {
            RecipeTextParser.parse(text)
        }
    }
}
