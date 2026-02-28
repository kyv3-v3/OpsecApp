package com.opsecapp.app.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opsecapp.app.di.AppContainer
import com.opsecapp.app.install.InstallResult
import com.opsecapp.app.ui.category.CategoryScreen
import com.opsecapp.app.ui.category.CategoryViewModel
import com.opsecapp.app.ui.detail.DetailScreen
import com.opsecapp.app.ui.detail.DetailViewModel
import com.opsecapp.app.ui.home.HomeScreen
import com.opsecapp.app.ui.home.HomeViewModel
import com.opsecapp.app.ui.search.SearchScreen
import com.opsecapp.app.ui.search.SearchViewModel
import com.opsecapp.app.ui.settings.SettingsScreen
import com.opsecapp.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

private object Routes {
  const val HOME = "home"
  const val SEARCH = "search"
  const val SETTINGS = "settings"
  const val CATEGORY = "category/{id}"
  const val DETAIL = "detail/{id}"
}

@Composable
fun OpsecAppNavHost(container: AppContainer) {
  val navController = rememberNavController()
  val context = LocalContext.current

  NavHost(navController = navController, startDestination = Routes.HOME) {
    composable(Routes.HOME) {
      val vm: HomeViewModel = viewModel(factory = container.viewModelFactory)
      HomeScreen(
        stateFlow = vm.state,
        onRefresh = vm::refresh,
        onCategoryClick = { id -> navController.navigate("category/$id") },
        onItemClick = { id -> navController.navigate("detail/$id") },
        onSearchClick = { navController.navigate(Routes.SEARCH) },
        onSettingsClick = { navController.navigate(Routes.SETTINGS) }
      )
    }

    composable(
      route = Routes.CATEGORY,
      arguments = listOf(navArgument("id") { type = NavType.StringType })
    ) { backStackEntry ->
      val categoryId = backStackEntry.arguments?.getString("id").orEmpty()
      val vm: CategoryViewModel = viewModel(factory = container.viewModelFactory)
      LaunchedEffect(categoryId) { vm.load(categoryId) }
      CategoryScreen(
        stateFlow = vm.state,
        onBack = navController::popBackStack,
        onItemClick = { id -> navController.navigate("detail/$id") }
      )
    }

    composable(Routes.SEARCH) {
      val vm: SearchViewModel = viewModel(factory = container.viewModelFactory)
      SearchScreen(
        stateFlow = vm.state,
        onBack = navController::popBackStack,
        onQueryChange = vm::onQueryChange,
        onSourceConfidenceChange = vm::onSourceConfidenceChange,
        onInstallTypeChange = vm::onInstallTypeChange,
        onItemClick = { id -> navController.navigate("detail/$id") }
      )
    }

    composable(
      route = Routes.DETAIL,
      arguments = listOf(navArgument("id") { type = NavType.StringType })
    ) { backStackEntry ->
      val vm: DetailViewModel = viewModel(factory = container.viewModelFactory)
      val id = backStackEntry.arguments?.getString("id").orEmpty()
      LaunchedEffect(id) { vm.load(id) }

      LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
          when (event) {
            is InstallResult.NeedsUserAction -> {
              event.actionIntent?.let {
                context.startActivity(it)
              }
            }
            InstallResult.FdroidMissing -> {
              vm.promptInstallFdroid()
            }
            else -> Unit
          }
        }
      }

      DetailScreen(
        stateFlow = vm.state,
        events = vm.events,
        onBack = navController::popBackStack,
        onInstallClick = vm::installPreferred,
        onOpenLink = { url ->
          context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
      )
    }

    composable(Routes.SETTINGS) {
      val vm: SettingsViewModel = viewModel(factory = container.viewModelFactory)
      LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
          when (event) {
            is InstallResult.NeedsUserAction -> {
              event.actionIntent?.let {
                context.startActivity(it)
              }
            }
            else -> Unit
          }
        }
      }

      SettingsScreen(
        stateFlow = vm.state,
        events = vm.events,
        onBack = navController::popBackStack,
        onCatalogUrlSave = vm::saveCatalogBaseUrl,
        onSyncNow = vm::syncNow,
        onCheckAppUpdates = vm::checkAppUpdates,
        onInstallAppUpdate = vm::installAppUpdate,
        onOpenReleasePage = { url ->
          context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
      )
    }
  }
}
