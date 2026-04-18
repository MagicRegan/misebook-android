package com.misebook.app.ui.screens.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.R
import com.misebook.app.domain.model.Recipe
import com.misebook.app.ui.AppViewModel
import com.misebook.app.ui.components.EmptyState
import com.misebook.app.ui.components.MiseCard
import com.misebook.app.ui.components.SectionHeader
import com.misebook.app.ui.components.WorkspaceSwitcher

@Composable
fun HomeScreen(
    appVm: AppViewModel,
    onOpenRecipe: (String) -> Unit,
    onCreateRecipe: () -> Unit,
    onImport: () -> Unit,
    onBrowseAll: () -> Unit,
    onManageWorkspaces: () -> Unit
) {
    val appState by appVm.state.collectAsStateWithLifecycle()
    val current = appState.currentWorkspace
    val vm: HomeViewModel = hiltViewModel()
    val recent by vm.recent.collectAsStateWithLifecycle()

    // Feed the viewmodel the current workspace
    androidx.compose.runtime.LaunchedEffect(current?.id) {
        vm.setWorkspace(current?.id)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WorkspaceSwitcher(
                    current = current,
                    all = appState.workspaces,
                    onSelect = appVm::selectWorkspace,
                    onManage = onManageWorkspaces,
                    onCreateNew = onManageWorkspaces
                )
            }
        }
        item {
            Column {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = current?.name ?: "Kitchen",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.home_quick_actions))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAction(
                    title = stringResource(R.string.home_new_recipe),
                    icon = Icons.Rounded.Add,
                    onClick = onCreateRecipe,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    title = stringResource(R.string.home_import),
                    icon = Icons.Rounded.FileUpload,
                    onClick = onImport,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAction(
                    title = stringResource(R.string.home_browse_all),
                    icon = Icons.Rounded.MenuBook,
                    onClick = onBrowseAll,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    title = stringResource(R.string.home_scale),
                    icon = Icons.Rounded.Tune,
                    onClick = onBrowseAll,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.home_recent))
        }
        if (recent.isEmpty()) {
            item {
                MiseCard {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body),
                        action = {
                            Button(onClick = onCreateRecipe) {
                                Icon(Icons.Rounded.Add, null)
                                Spacer(Modifier.size(4.dp))
                                Text("Create recipe")
                            }
                        }
                    )
                }
            }
        } else {
            items(recent, key = { it.id }) { recipe ->
                RecipeRow(recipe = recipe, onClick = { onOpenRecipe(recipe.id) })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuickAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(92.dp).clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun RecipeRow(recipe: Recipe, onClick: () -> Unit) {
    MiseCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                val sub = buildString {
                    recipe.servings?.let { append("$it servings") }
                    recipe.totalTimeMin?.let {
                        if (isNotEmpty()) append(" · ")
                        append("${it} min")
                    }
                    if (recipe.ingredients.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append("${recipe.ingredients.size} ingredients")
                    }
                }
                if (sub.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}


