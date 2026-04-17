package com.misebook.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misebook.app.ui.AppViewModel
import com.misebook.app.ui.components.MiseCard

@Composable
fun SettingsScreen(
    appVm: AppViewModel,
    onManageWorkspaces: () -> Unit
) {
    val state by appVm.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        MiseCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp), onClick = onManageWorkspaces) {
            ListItem(
                headlineContent = { Text("Workspaces", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("${state.workspaces.size} total") },
                leadingContent = { Icon(Icons.Rounded.Business, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
        MiseCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            ListItem(
                headlineContent = { Text("About MiseBook", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Offline-first recipe workspace for chefs and home cooks.") },
                leadingContent = { Icon(Icons.Rounded.Info, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
