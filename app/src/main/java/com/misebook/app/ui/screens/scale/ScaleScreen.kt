package com.misebook.app.ui.screens.scale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.domain.model.MeasurementUnit
import com.misebook.app.ui.components.MiseCard

@Composable
fun ScaleScreen(recipeId: String, onBack: () -> Unit) {
    val vm: ScaleViewModel = hiltViewModel()
    LaunchedEffect(recipeId) { vm.load(recipeId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val r = state.recipe
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scale recipe", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(inner))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(r.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.mode == ScaleMode.SERVINGS,
                        onClick = { vm.setMode(ScaleMode.SERVINGS) },
                        label = { Text("By servings") }
                    )
                    FilterChip(
                        selected = state.mode == ScaleMode.INGREDIENT,
                        onClick = { vm.setMode(ScaleMode.INGREDIENT) },
                        label = { Text("By available amount") }
                    )
                }
            }
            item {
                MiseCard {
                    when (state.mode) {
                        ScaleMode.SERVINGS -> ServingsControl(
                            current = state.targetServings,
                            original = r.servings ?: 1,
                            onChange = vm::setTargetServings
                        )
                        ScaleMode.INGREDIENT -> IngredientControl(vm = vm)
                    }
                }
            }
            if (state.errorMessage != null) {
                item {
                    Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            val scaled = state.scaled
            if (scaled != null) {
                item {
                    Column {
                        Text(vm.reasonLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Factor ×${"%.2f".format(scaled.factor)}" +
                                (scaled.scaledServings?.let { " · $it servings" } ?: "") +
                                (scaled.scaledYieldAmount?.let { " · yield $it ${r.yieldUnit.orEmpty()}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(scaled.ingredients, key = { it.original.id }) { sc ->
                    MiseCard {
                        Column {
                            Text(sc.original.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Original", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${sc.original.displayQuantity} ${sc.original.unit.displayShort}".trim(),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text("→", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Scaled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    val qStr = sc.scaledQuantity?.let {
                                        if (it == it.toLong().toDouble()) it.toLong().toString()
                                        else "%.2f".format(it).trimEnd('0').trimEnd('.')
                                    } ?: "—"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sc.wasChanged) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            "$qStr ${sc.scaledUnit.displayShort}".trim(),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (sc.wasChanged) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ServingsControl(current: Int, original: Int, onChange: (Int) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Target servings", style = MaterialTheme.typography.titleMedium)
            Text("$current", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = current.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 1f..(original * 8).coerceAtLeast(8).toFloat(),
            steps = 0
        )
        Text("Original recipe: $original servings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IngredientControl(vm: ScaleViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val r = state.recipe ?: return
    val anchor = r.ingredients.firstOrNull { it.id == state.anchorIngredientId }
    Column {
        Text("Pick the ingredient that's limiting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        var anchorMenu by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = anchorMenu, onExpandedChange = { anchorMenu = it }) {
            OutlinedTextField(
                value = anchor?.name ?: "Select ingredient",
                onValueChange = {},
                readOnly = true,
                label = { Text("Anchor ingredient") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = anchorMenu) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = anchorMenu, onDismissRequest = { anchorMenu = false }) {
                r.ingredients.filter { it.quantity != null }.forEach { ing ->
                    DropdownMenuItem(
                        text = { Text("${ing.name}  (${ing.displayQuantity} ${ing.unit.displayShort})") },
                        onClick = { vm.setAnchor(ing.id); anchorMenu = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.anchorAvailableQty,
                onValueChange = vm::setAvailableQty,
                label = { Text("Amount you have") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            UnitMenu(
                selected = state.anchorAvailableUnit,
                onSelect = vm::setAvailableUnit,
                dimension = anchor?.unit,
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

@Composable
private fun UnitMenu(
    selected: MeasurementUnit,
    onSelect: (MeasurementUnit) -> Unit,
    dimension: MeasurementUnit?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = if (dimension == null || dimension == MeasurementUnit.NONE) MeasurementUnit.entries.toList()
    else MeasurementUnit.entries.filter { it.dimension == dimension.dimension }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.displayShort.ifBlank { "—" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { u ->
                DropdownMenuItem(
                    text = { Text(if (u == MeasurementUnit.NONE) "No unit" else u.displayShort) },
                    onClick = { onSelect(u); expanded = false }
                )
            }
        }
    }
}
