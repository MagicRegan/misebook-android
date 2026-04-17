package com.misebook.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.misebook.app.ui.navigation.Routes
import com.misebook.app.ui.screens.cookmode.CookModeScreen
import com.misebook.app.ui.screens.editor.RecipeEditorScreen
import com.misebook.app.ui.screens.home.HomeScreen
import com.misebook.app.ui.screens.importer.ImportReviewScreen
import com.misebook.app.ui.screens.importer.ImportScreen
import com.misebook.app.ui.screens.importer.ImportViewModel
import com.misebook.app.ui.screens.onboarding.OnboardingScreen
import com.misebook.app.ui.screens.recipes.RecipeDetailScreen
import com.misebook.app.ui.screens.recipes.RecipesListScreen
import com.misebook.app.ui.screens.scale.ScaleScreen
import com.misebook.app.ui.screens.search.SearchScreen
import com.misebook.app.ui.screens.settings.SettingsScreen
import com.misebook.app.ui.screens.workspace.WorkspacesScreen

@Composable
fun MiseBookRoot() {
    val appVm: AppViewModel = hiltViewModel()
    val state by appVm.state.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            state.loading -> Box(Modifier.fillMaxSize()) {}
            !state.onboarded || state.workspaces.isEmpty() -> {
                OnboardingScreen(onCreate = { name ->
                    appVm.createWorkspace(name)
                })
            }
            else -> MainShell(appVm = appVm)
        }
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Home", Icons.Rounded.Home),
    BottomDestination(Routes.RECIPES, "Recipes", Icons.Rounded.MenuBook),
    BottomDestination(Routes.IMPORT, "Import", Icons.Rounded.FileUpload),
    BottomDestination(Routes.SEARCH, "Search", Icons.Rounded.Search),
    BottomDestination(Routes.SETTINGS, "Settings", Icons.Rounded.Settings)
)

@Composable
private fun MainShell(appVm: AppViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    val importVm: ImportViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.Dp(0f)
                ) {
                    bottomDestinations.forEach { dest ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    appVm = appVm,
                    onOpenRecipe = { id -> navController.navigate(Routes.recipeDetail(id)) },
                    onCreateRecipe = { navController.navigate(Routes.recipeEdit()) },
                    onImport = { navController.navigate(Routes.IMPORT) },
                    onBrowseAll = { navController.navigate(Routes.RECIPES) },
                    onManageWorkspaces = { navController.navigate(Routes.WORKSPACES) }
                )
            }
            composable(Routes.RECIPES) {
                RecipesListScreen(
                    appVm = appVm,
                    onOpenRecipe = { id -> navController.navigate(Routes.recipeDetail(id)) },
                    onCreateRecipe = { navController.navigate(Routes.recipeEdit()) },
                    onManageWorkspaces = { navController.navigate(Routes.WORKSPACES) }
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(
                    vm = importVm,
                    onReviewReady = { navController.navigate(Routes.IMPORT_REVIEW) }
                )
            }
            composable(Routes.IMPORT_REVIEW) {
                ImportReviewScreen(
                    vm = importVm,
                    appVm = appVm,
                    onSaved = { id ->
                        navController.popBackStack(Routes.HOME, inclusive = false)
                        navController.navigate(Routes.recipeDetail(id))
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    appVm = appVm,
                    onOpenRecipe = { id -> navController.navigate(Routes.recipeDetail(id)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    appVm = appVm,
                    onManageWorkspaces = { navController.navigate(Routes.WORKSPACES) }
                )
            }
            composable(Routes.WORKSPACES) {
                WorkspacesScreen(appVm = appVm, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.RECIPE_DETAIL,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                RecipeDetailScreen(
                    recipeId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.recipeEdit(id)) },
                    onScale = { navController.navigate(Routes.scale(id)) },
                    onCook = { navController.navigate(Routes.cookMode(id)) }
                )
            }
            composable(
                route = Routes.RECIPE_EDIT,
                arguments = listOf(navArgument("recipeId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("recipeId")
                RecipeEditorScreen(
                    recipeId = id,
                    appVm = appVm,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedId ->
                        navController.popBackStack()
                        navController.navigate(Routes.recipeDetail(savedId))
                    }
                )
            }
            composable(
                route = Routes.SCALE,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                ScaleScreen(recipeId = id, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.COOK_MODE,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                CookModeScreen(recipeId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun Int.dp(): androidx.compose.ui.unit.Dp = this.toFloat().let { androidx.compose.ui.unit.Dp(it) }
