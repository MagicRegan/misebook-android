package com.misebook.app.ui.screens.recipes

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.domain.model.Recipe
import com.misebook.app.ui.components.MiseCard
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onScale: () -> Unit,
    onCook: () -> Unit
) {
    val vm: RecipeDetailViewModel = hiltViewModel()
    LaunchedEffect(recipeId) { vm.load(recipeId) }
    val recipe by vm.recipe.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }
    // Session-only "done" state so chefs can tick off prep and finished steps.
    // Scoped per recipe; cleared whenever the detail screen is navigated away from.
    val doneIngredients = remember(recipeId) { mutableStateOf(setOf<String>()) }
    val doneDirections = remember(recipeId) { mutableStateOf(setOf<String>()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(recipe?.name.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
                    IconButton(onClick = {
                        scope.launch {
                            val id = vm.duplicate()
                            if (id != null) onBack()
                        }
                    }) { Icon(Icons.Rounded.ContentCopy, "Duplicate") }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Rounded.Delete, "Delete")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onScale,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Scale")
                    }
                    Button(
                        onClick = onCook,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Icon(Icons.Rounded.LocalFireDepartment, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Cook mode")
                    }
                }
            }
        }
    ) { inner ->
        val r = recipe
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(inner)) {}
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MetaHeader(r)
            }
            item { SectionTitle("Ingredients") }
            item {
                Text(
                    "Tap to check off as you go.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(r.ingredients, key = { it.id }) { ing ->
                val isDone = ing.id in doneIngredients.value
                MiseCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                doneIngredients.value = doneIngredients.value.toggle(ing.id)
                            }
                    ) {
                        val chipColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                        val chipContent = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                        Surface(shape = CircleShape, color = chipColor, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    if (isDone) "✓" else ing.displayQuantity.ifBlank { "•" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = chipContent
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                ing.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            val detail = buildString {
                                if (ing.unit != com.misebook.app.domain.model.MeasurementUnit.NONE) append(ing.unit.displayShort)
                                if (!ing.note.isNullOrBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(ing.note)
                                }
                            }
                            if (detail.isNotBlank()) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Directions") }
            item {
                Text(
                    "Tap a step to mark it done.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(r.directions, key = { it.id }) { dir ->
                val isDone = dir.id in doneDirections.value
                MiseCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                doneDirections.value = doneDirections.value.toggle(dir.id)
                            }
                    ) {
                        val stepColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer
                        val stepContent = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer
                        Surface(shape = RoundedCornerShape(10.dp), color = stepColor, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    if (isDone) "✓" else "${dir.position + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = stepContent
                                )
                            }
                        }
                        Text(
                            dir.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            if (!r.notes.isNullOrBlank()) {
                item { SectionTitle("Notes") }
                item {
                    MiseCard { Text(r.notes.orEmpty(), style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete recipe?") },
            text = { Text("\"${recipe?.name}\" will be removed from this workspace. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        vm.delete()
                        onBack()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

@Composable
private fun MetaHeader(r: Recipe) {
    MiseCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                r.prepTimeMin?.let {
                    AssistChip(onClick = {}, label = { Text("Prep ${it} min") }, leadingIcon = { Icon(Icons.Rounded.Schedule, null) })
                }
                r.cookTimeMin?.let {
                    AssistChip(onClick = {}, label = { Text("Cook ${it} min") }, leadingIcon = { Icon(Icons.Rounded.LocalFireDepartment, null) })
                }
                r.servings?.let {
                    AssistChip(onClick = {}, label = { Text("$it servings") })
                }
            }
            if (r.yieldAmount != null) {
                HorizontalDivider()
                Text("Yields ${r.yieldAmount} ${r.yieldUnit.orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
