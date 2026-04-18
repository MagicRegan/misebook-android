package com.misebook.app.ui.screens.importer

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.domain.importer.ParsedIngredient
import com.misebook.app.domain.model.Direction
import com.misebook.app.domain.model.Ingredient
import com.misebook.app.domain.model.MeasurementUnit
import com.misebook.app.ui.AppViewModel
import com.misebook.app.ui.components.LowConfidenceBadge
import com.misebook.app.ui.components.MiseCard
import com.misebook.app.ui.components.SectionHeader
import java.util.UUID

@Composable
fun ImportReviewScreen(
    vm: ImportViewModel,
    appVm: AppViewModel,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val appState by appVm.state.collectAsStateWithLifecycle()
    val draft = state.recipeDraft
    val parsed = state.parsed

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Review import", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Discard")
                    }
                    Button(
                        onClick = {
                            val ws = appState.currentWorkspace?.id ?: return@Button
                            vm.save(ws, onSaved)
                        },
                        enabled = !state.saving && appState.currentWorkspace != null,
                        modifier = Modifier.weight(2f).height(52.dp)
                    ) {
                        Text(if (state.saving) "Saving…" else "Save to workspace", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { inner ->
        if (draft == null) {
            Box(Modifier.fillMaxSize().padding(inner)) {
                Text("No recipe to review yet.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (parsed != null) {
                parsed.warnings.forEach { w ->
                    item {
                        MiseCard {
                            Text(w, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { n -> vm.updateDraft { it.copy(name = n) } },
                    label = { Text("Recipe name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draft.servings?.toString().orEmpty(),
                        onValueChange = { v -> vm.updateDraft { it.copy(servings = v.toIntOrNull()) } },
                        label = { Text("Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.prepTimeMin?.toString().orEmpty(),
                        onValueChange = { v -> vm.updateDraft { it.copy(prepTimeMin = v.toIntOrNull()) } },
                        label = { Text("Prep (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { SectionHeader(title = "Ingredients") }
            items(draft.ingredients, key = { it.id }) { ing ->
                val parsedIng = findParsedIngredient(parsed?.ingredients, ing)
                MiseCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LowConfidenceBadge(
                            visible = (parsedIng?.confidence ?: 1.0) < 0.7,
                            text = "Review this line"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ing.quantity?.let { q -> if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString() }.orEmpty(),
                                onValueChange = { v -> vm.updateDraft { r -> r.updateIngredient(ing.id) { it.copy(quantity = v.toDoubleOrNull()) } } },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = if (ing.unit == MeasurementUnit.NONE) "" else ing.unit.displayShort,
                                onValueChange = { v ->
                                    val parsedUnit = MeasurementUnit.parse(v) ?: MeasurementUnit.NONE
                                    vm.updateDraft { r -> r.updateIngredient(ing.id) { it.copy(unit = parsedUnit) } }
                                },
                                label = { Text("Unit") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { vm.updateDraft { r -> r.copy(ingredients = r.ingredients.filter { it.id != ing.id }) } }) {
                                Icon(Icons.Rounded.Close, null)
                            }
                        }
                        OutlinedTextField(
                            value = ing.name,
                            onValueChange = { v -> vm.updateDraft { r -> r.updateIngredient(ing.id) { it.copy(name = v) } } },
                            label = { Text("Ingredient") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!ing.note.isNullOrBlank()) {
                            OutlinedTextField(
                                value = ing.note ?: "",
                                onValueChange = { v -> vm.updateDraft { r -> r.updateIngredient(ing.id) { it.copy(note = v.ifBlank { null }) } } },
                                label = { Text("Note") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        vm.updateDraft { r ->
                            r.copy(ingredients = r.ingredients + Ingredient(
                                id = UUID.randomUUID().toString(),
                                position = r.ingredients.size,
                                quantity = null,
                                unit = MeasurementUnit.NONE,
                                name = ""
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.size(8.dp)); Text("Add ingredient")
                }
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SectionHeader(title = "Directions") }
            items(draft.directions, key = { it.id }) { d ->
                MiseCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Step ${d.position + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                vm.updateDraft { r -> r.copy(directions = r.directions.filter { it.id != d.id }.reindexDir()) }
                            }) { Icon(Icons.Rounded.Close, null) }
                        }
                        OutlinedTextField(
                            value = d.text,
                            onValueChange = { v -> vm.updateDraft { r -> r.copy(directions = r.directions.map { if (it.id == d.id) it.copy(text = v) else it }) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        vm.updateDraft { r ->
                            r.copy(directions = r.directions + Direction(
                                id = UUID.randomUUID().toString(),
                                position = r.directions.size,
                                text = ""
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.size(8.dp)); Text("Add step")
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

private fun findParsedIngredient(parsed: List<ParsedIngredient>?, ing: Ingredient): ParsedIngredient? {
    if (parsed == null) return null
    return parsed.firstOrNull { it.name.equals(ing.name, ignoreCase = true) }
}

private fun com.misebook.app.domain.model.Recipe.updateIngredient(
    id: String,
    transform: (Ingredient) -> Ingredient
) = copy(ingredients = ingredients.map { if (it.id == id) transform(it) else it })

private fun List<Direction>.reindexDir() = mapIndexed { idx, it -> it.copy(position = idx) }
