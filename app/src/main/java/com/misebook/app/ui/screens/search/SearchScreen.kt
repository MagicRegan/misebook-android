package com.misebook.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.ui.AppViewModel
import com.misebook.app.ui.components.EmptyState
import com.misebook.app.ui.components.MiseCard
import com.misebook.app.ui.screens.home.RecipeRow

@Composable
fun SearchScreen(
    appVm: AppViewModel,
    onOpenRecipe: (String) -> Unit
) {
    val appState by appVm.state.collectAsStateWithLifecycle()
    val vm: SearchViewModel = hiltViewModel()
    val q by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    LaunchedEffect(appState.currentWorkspace?.id) {
        vm.setWorkspace(appState.currentWorkspace?.id)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Search", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = q,
            onValueChange = vm::setQuery,
            placeholder = { Text("Search recipes, ingredients, notes…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (q.isBlank()) {
            MiseCard {
                EmptyState(
                    title = "Find anything",
                    body = "Search by recipe name or keyword. Results stay inside this workspace."
                )
            }
        } else if (results.isEmpty()) {
            MiseCard {
                EmptyState(title = "No matches", body = "Try a shorter word or check your spelling.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(results, key = { it.id }) { r ->
                    RecipeRow(recipe = r, onClick = { onOpenRecipe(r.id) })
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }
}
