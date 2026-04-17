package com.misebook.app.domain.importer

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
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
        val text = recognize(image)
        if (text.isBlank()) {
            return ParsedRecipe(
                title = "Imported photo",
                confidence = 0.0,
                warnings = listOf("No text could be read from this image. Try a sharper photo with good lighting.")
            )
        }
        return RecipeTextParser.parse(text)
    }

    private suspend fun recognize(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
