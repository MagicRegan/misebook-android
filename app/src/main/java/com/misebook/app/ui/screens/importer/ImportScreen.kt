package com.misebook.app.ui.screens.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.ui.components.MiseCard
import com.misebook.app.ui.components.SectionHeader

@Composable
fun ImportScreen(
    vm: ImportViewModel,
    onReviewReady: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importFromImage(uri, onReviewReady)
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importFromPdf(uri, onReviewReady)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Import a recipe", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text("Pick any source. We'll parse it and let you review before saving.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionHeader(title = "From file")
        ImportTile(
            title = "Import photo (OCR)",
            subtitle = "Handwritten or printed page",
            icon = Icons.Rounded.Image,
            enabled = !state.isImporting,
            onClick = { imagePicker.launch("image/*") }
        )
        ImportTile(
            title = "Import PDF",
            subtitle = "Searchable PDFs work best",
            icon = Icons.Rounded.PictureAsPdf,
            enabled = !state.isImporting,
            onClick = { pdfPicker.launch("application/pdf") }
        )

        SectionHeader(title = "From a link")
        MiseCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.urlInput,
                    onValueChange = vm::onUrlInput,
                    label = { Text("Paste a recipe URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { vm.importFromUrl(onReviewReady) },
                    enabled = !state.isImporting,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.Link, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Fetch recipe")
                }
            }
        }

        SectionHeader(title = "From text")
        MiseCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.freeformText,
                    onValueChange = vm::onFreeformInput,
                    label = { Text("Paste any recipe text") },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                OutlinedButton(
                    onClick = { vm.importFromFreeform(onReviewReady) },
                    enabled = !state.isImporting,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.TextSnippet, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Parse text")
                }
            }
        }

        if (state.isImporting) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Parsing your recipe…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ImportTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else { {} }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.FileUpload, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
