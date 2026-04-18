package com.misebook.app.ui.navigation

/** All navigation routes in one place for safety. */
object Routes {
    const val HOME = "home"
    const val RECIPES = "recipes"
    const val IMPORT = "import"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    const val ONBOARDING = "onboarding"
    const val WORKSPACES = "workspaces"

    const val RECIPE_DETAIL = "recipe/{recipeId}"
    fun recipeDetail(id: String) = "recipe/$id"

    const val RECIPE_EDIT = "recipe/edit?recipeId={recipeId}"
    fun recipeEdit(id: String? = null) = if (id == null) "recipe/edit" else "recipe/edit?recipeId=$id"

    const val COOK_MODE = "cook/{recipeId}"
    fun cookMode(id: String) = "cook/$id"

    const val SCALE = "scale/{recipeId}"
    fun scale(id: String) = "scale/$id"

    const val IMPORT_REVIEW = "import/review"
}
