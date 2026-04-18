package com.misebook.app.ui.screens.editor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.R
import com.misebook.app.domain.model.MeasurementUnit
import com.misebook.app.ui.AppViewModel
import com.misebook.app.ui.components.MiseCard
import com.misebook.app.ui.components.ReorderableColumn
import com.misebook.app.ui.components.ReorderableItemScope

private enum class EditorTab(val label: Int) {
    OVERVIEW(R.string.tab_overview),
    INGREDIENTS(R.string.tab_ingredients),
    DIRECTIONS(R.string.tab_directions),
    YIELD(R.string.tab_yield),
    NOTES(R.string.tab_notes),
    MEDIA(R.string.tab_media)
}

@Composable
fun RecipeEditorScreen(
    recipeId: String?,
    appVm: AppViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val vm: RecipeEditorViewModel = hiltViewModel()
    val appState by appVm.state.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(recipeId) {
        if (recipeId != null) {
            vm.loadExisting(recipeId)
        } else {
            // Read the workspace id inside the effect so a workspace switch while the
            // editor is open doesn't re-trigger and wipe unsaved edits.
            appState.currentWorkspace?.id?.let { vm.initNew(it) }
        }
    }

    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (state.isNew) "New recipe" else "Edit recipe", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
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
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { vm.save(onSaved) },
                        enabled = !state.saving,
                        modifier = Modifier.weight(2f).height(52.dp)
                    ) {
                        Text(if (state.saving) "Saving…" else "Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                EditorTab.entries.forEachIndexed { idx, tab ->
                    Tab(
                        selected = tabIndex == idx,
                        onClick = { tabIndex = idx },
                        text = {
                            Text(
                                stringResourceWrapper(tab.label),
                                fontWeight = if (tabIndex == idx) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                when (EditorTab.entries[tabIndex]) {
                    EditorTab.OVERVIEW -> OverviewTab(vm = vm)
                    EditorTab.INGREDIENTS -> IngredientsTab(vm = vm)
                    EditorTab.DIRECTIONS -> DirectionsTab(vm = vm)
                    EditorTab.YIELD -> YieldTab(vm = vm)
                    EditorTab.NOTES -> NotesTab(vm = vm)
                    EditorTab.MEDIA -> MediaTab(vm = vm)
                }
            }
        }
    }
}

@Composable
private fun stringResourceWrapper(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun OverviewTab(vm: RecipeEditorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val r = state.recipe
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = r.name,
            onValueChange = vm::updateName,
            label = { Text("Recipe name") },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = r.prepTimeMin?.toString().orEmpty(),
                onValueChange = { vm.updatePrep(it.toIntOrNull()) },
                label = { Text("Prep (min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = r.cookTimeMin?.toString().orEmpty(),
                onValueChange = { vm.updateCook(it.toIntOrNull()) },
                label = { Text("Cook (min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = r.servings?.toString().orEmpty(),
            onValueChange = { vm.updateServings(it.toIntOrNull()) },
            label = { Text("Servings") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Category and subcategory management is available from the Settings screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IngredientsTab(vm: RecipeEditorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Long-press the handle to drag and reorder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReorderableColumn(
            items = state.recipe.ingredients,
            key = { it.id },
            onMove = vm::moveIngredient
        ) { ing, _ ->
            MiseCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = dragHandle().size(24.dp)
                        )
                        OutlinedTextField(
                            value = ing.quantity?.let { q ->
                                if (q == q.toLong().toDouble()) q.toLong().toString()
                                else q.toString()
                            }.orEmpty(),
                            onValueChange = { v ->
                                vm.updateIngredient(ing.id) { it.copy(quantity = v.toDoubleOrNull()) }
                            },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        UnitSelector(
                            selected = ing.unit,
                            onSelect = { u -> vm.updateIngredient(ing.id) { it.copy(unit = u) } },
                            modifier = Modifier.weight(1.4f)
                        )
                        IconButton(onClick = { vm.removeIngredient(ing.id) }) {
                            Icon(Icons.Rounded.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedTextField(
                        value = ing.name,
                        onValueChange = { v -> vm.updateIngredient(ing.id) { it.copy(name = v) } },
                        label = { Text("Ingredient") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ing.note.orEmpty(),
                        onValueChange = { v -> vm.updateIngredient(ing.id) { it.copy(note = v.ifBlank { null }) } },
                        label = { Text("Note (optional) e.g. finely chopped") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        OutlinedButton(onClick = { vm.addIngredient() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.size(8.dp))
            Text("Add ingredient")
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun DirectionsTab(vm: RecipeEditorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Long-press the handle to drag and reorder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReorderableColumn(
            items = state.recipe.directions,
            key = { it.id },
            onMove = vm::moveDirection
        ) { dir, _ ->
            MiseCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${dir.position + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = dragHandle().size(24.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { vm.removeDirection(dir.id) }) {
                            Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedTextField(
                        value = dir.text,
                        onValueChange = { vm.updateDirection(dir.id, it) },
                        label = { Text("Step ${dir.position + 1}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        OutlinedButton(onClick = { vm.addDirection() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.size(8.dp))
            Text("Add step")
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun YieldTab(vm: RecipeEditorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val r = state.recipe
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Servings & yield", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = r.servings?.toString().orEmpty(),
            onValueChange = { vm.updateServings(it.toIntOrNull()) },
            label = { Text("Servings") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = r.yieldAmount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }.orEmpty(),
                onValueChange = { vm.updateYield(it.toDoubleOrNull(), r.yieldUnit) },
                label = { Text("Yield amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = r.yieldUnit.orEmpty(),
                onValueChange = { vm.updateYield(r.yieldAmount, it) },
                label = { Text("Unit (e.g. kg, portions)") },
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
private fun NotesTab(vm: RecipeEditorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.recipe.notes.orEmpty(),
            onValueChange = vm::updateNotes,
            label = { Text("Chef's notes, reminders, history…") },
            modifier = Modifier.fillMaxWidth().height(280.dp)
        )
    }
}

@Composable
private fun MediaTab(vm: RecipeEditorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val r = state.recipe
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Media & source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = r.sourceUrl.orEmpty(),
            onValueChange = { vm.updateSourceUrl(it.ifBlank { null }) },
            label = { Text("Source URL (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = r.imagePath.orEmpty(),
            onValueChange = { vm.updateImage(it.ifBlank { null }) },
            label = { Text("Image path (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Chef tip: attach a finished plate photo so staff can plate it consistently.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnitSelector(
    selected: MeasurementUnit,
    onSelect: (MeasurementUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (selected == MeasurementUnit.NONE) "—" else selected.displayShort,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf(MeasurementUnit.NONE) + MeasurementUnit.entries.filter { it != MeasurementUnit.NONE }).forEach { u ->
                DropdownMenuItem(
                    text = { Text(if (u == MeasurementUnit.NONE) "No unit" else u.displayShort) },
                    onClick = { onSelect(u); expanded = false }
                )
            }
        }
    }
}
