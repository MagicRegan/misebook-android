package com.misebook.app.ui.screens.cookmode

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CookModeScreen(recipeId: String, onBack: () -> Unit) {
    val vm: CookModeViewModel = hiltViewModel()
    LaunchedEffect(recipeId) { vm.load(recipeId) }
    val recipe by vm.recipe.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { (recipe?.directions?.size ?: 0).coerceAtLeast(1) })
    // Tap-to-tick state for the current cook session. Held in the composition so
    // it resets whenever the chef leaves cook mode.
    var doneSteps by remember(recipeId) { mutableStateOf(setOf<String>()) }
    var doneIngredients by remember(recipeId) { mutableStateOf(setOf<String>()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(recipe?.name.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        val r = recipe ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(r.directions.size.coerceAtLeast(1)) { idx ->
                    val active = idx == pagerState.currentPage
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val step = r.directions.getOrNull(page)
                val stepId = step?.id
                val stepDone = stepId != null && stepId in doneSteps
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .clickable(enabled = stepId != null) {
                            if (stepId != null) {
                                doneSteps = if (stepId in doneSteps) doneSteps - stepId else doneSteps + stepId
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val badgeColor = if (stepDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                    val badgeContent = if (stepDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                    Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (stepDone) {
                                Icon(Icons.Rounded.Check, null, tint = badgeContent, modifier = Modifier.size(32.dp))
                            } else {
                                Text("${page + 1}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = badgeContent)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = step?.text ?: "No steps yet",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (stepDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (stepDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (stepDone) "Tap again to un-mark." else "Tap anywhere to mark this step done.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    if (r.ingredients.isNotEmpty()) {
                        Text("Have ready", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        r.ingredients.take(6).forEach { ing ->
                            val ingDone = ing.id in doneIngredients
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        doneIngredients = if (ing.id in doneIngredients) doneIngredients - ing.id else doneIngredients + ing.id
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                if (ingDone) {
                                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Rounded.Circle, null, modifier = Modifier.size(6.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    ing.displayLine,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (ingDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (ingDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
