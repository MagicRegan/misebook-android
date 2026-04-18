package com.misebook.app.ui.screens.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.ui.AppViewModel
import com.misebook.app.ui.components.EmptyState
import com.misebook.app.ui.components.MiseCard
import com.misebook.app.ui.components.WorkspaceSwitcher
import com.misebook.app.ui.screens.home.RecipeRow

@Composable
fun RecipesListScreen(
    appVm: AppViewModel,
    onOpenRecipe: (String) -> Unit,
    onCreateRecipe: () -> Unit,
    onManageWorkspaces: () -> Unit
) {
    val appState by appVm.state.collectAsStateWithLifecycle()
    val vm: RecipesListViewModel = hiltViewModel()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    LaunchedEffect(appState.currentWorkspace?.id) {
        vm.setWorkspace(appState.currentWorkspace?.id)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRecipe,
                text = { Text("New recipe", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Rounded.Add, null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                WorkspaceSwitcher(
                    current = appState.currentWorkspace,
                    all = appState.workspaces,
                    onSelect = appVm::selectWorkspace,
                    onManage = onManageWorkspaces,
                    onCreateNew = onManageWorkspaces
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Recipes", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (recipes.isEmpty()) {
                MiseCard {
                    EmptyState(
                        title = "Your recipe book is empty",
                        body = "Tap the button below to add your first recipe, or import one from an image, PDF, or URL."
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recipes, key = { it.id }) { r ->
                        RecipeRow(recipe = r, onClick = { onOpenRecipe(r.id) })
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }
}
